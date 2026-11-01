package vn.edu.policethief.server.network;

import vn.edu.policethief.server.Store;
import vn.edu.policethief.server.lobby.Room;
import vn.edu.policethief.shared.model.PlayerAction;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** All gameplay mutations are serialized under this server's monitor. */
public final class TcpServer implements AutoCloseable {
    public final Store store;
    public final ServerSocket listener;
    public final Map<String, ClientHandler> online = new HashMap<>();
    public final Set<ClientHandler> peers = new HashSet<>();
    public final vn.edu.policethief.server.lobby.RoomManager rooms =
            new vn.edu.policethief.server.lobby.RoomManager();
    public final Map<String, Invite> invites = new HashMap<>();
    public final Map<String, Store.Match> pendingResults = new LinkedHashMap<>();
    public long nextSaveRetry;
    public final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
    public final Random random = new java.security.SecureRandom();
    public volatile boolean running = true;

    public record Invite(String from, long until) {}

    public TcpServer(int port, Path dir) throws IOException {
        store = new Store(dir);
        listener = new ServerSocket();
        listener.setReuseAddress(true);
        listener.bind(new InetSocketAddress("0.0.0.0", port));
        timer.scheduleAtFixedRate(
                () -> {
                    synchronized (this) {
                        try {
                            tick();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                },
                50,
                50,
                TimeUnit.MILLISECONDS);
    }

    public int port() {
        return listener.getLocalPort();
    }

    public void run() throws IOException {
        System.out.println("MUSEUM HEIST server • TCP " + port() + " • data saved locally");
        while (running)
            try {
                Socket s = listener.accept();
                s.setTcpNoDelay(true);
                s.setKeepAlive(true);
                s.setSoTimeout(45000);
                synchronized (this) {
                    if (peers.size() >= 128) {
                        s.close();
                        continue;
                    }
                    ClientHandler p = new ClientHandler(this, s);
                    peers.add(p);
                    p.start();
                }
            } catch (SocketException e) {
                if (running) throw e;
            }
    }

    public void need(String[] m, int count) {
        if (m.length != count) throw new IllegalArgumentException();
    }

    public void handle(ClientHandler p, String[] m) {
        try {
            dispatch(p, m);
        } catch (IOException e) {
            p.send("ERROR", "Không truy cập được dữ liệu server. Hãy thử lại.");
            e.printStackTrace();
        }
    }

    public int requestedPage(String[] m) {
        if (m.length == 1) return 0;
        need(m, 2);
        int page = Integer.parseInt(m[1]);
        if (page < 0) throw new IllegalArgumentException();
        return page;
    }

    // Legacy values may contain long text or table delimiters. Bound the display only.

    public String display(String value, int maxBytes) {
        String clean = value.replace('|', '/').replace('\n', ' ').replace('\r', ' ');
        StringBuilder b = new StringBuilder();
        int bytes = 0;
        for (int i = 0; i < clean.length(); ) {
            int cp = clean.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            int size = ch.getBytes(StandardCharsets.UTF_8).length;
            if (bytes + size > maxBytes) {
                b.append('…');
                break;
            }
            b.append(ch);
            bytes += size;
            i += Character.charCount(cp);
        }
        return b.toString();
    }

    public void dispatch(ClientHandler p, String[] m) throws IOException {
        String op = m[0];
        if (op.equals("PING")) {
            p.send("PONG", System.currentTimeMillis());
            return;
        }
        if (op.equals("LOGIN") || op.equals("REGISTER")) {
            need(m, 3);
            if (p.user != null) {
                p.send("ERROR", "Bạn đã đăng nhập.");
                return;
            }
            if (System.currentTimeMillis() < p.retryAt) {
                p.send("ERROR", "Thử lại sau vài giây.");
                return;
            }
            if (!m[1].matches("[A-Za-z0-9_]{3,20}") || m[2].length() < 6 || m[2].length() > 128) {
                p.send("ERROR", "Tên 3–20 ký tự chữ/số/_, mật khẩu 6–128 ký tự.");
                return;
            }
            try {
                if (op.equals("REGISTER")) {
                    boolean ok = store.register(m[1], m[2]);
                    p.send(
                            ok ? "REGISTERED" : "ERROR",
                            ok
                                    ? "Đăng ký thành công, hãy đăng nhập."
                                    : "Tên tài khoản đã tồn tại.");
                    return;
                }
                Store.User u = store.login(m[1], m[2]);
                if (u == null) {
                    if (++p.failures >= 5) {
                        p.retryAt = System.currentTimeMillis() + 10000;
                        p.failures = 0;
                    }
                    p.send("ERROR", "Sai tài khoản hoặc mật khẩu.");
                    return;
                }
                if (online.containsKey(u.name)) {
                    p.send("ERROR", "Tài khoản đang online trên máy khác.");
                    return;
                }
                p.user = u;
                p.failures = 0;
                online.put(u.name, p);
                p.send("LOGIN_OK", u.name);
                lobbies();
            } catch (IOException e) {
                p.send("ERROR", "Không lưu được dữ liệu server.");
                e.printStackTrace();
            }
            return;
        }
        if (p.user == null) {
            p.send("ERROR", "Hãy đăng nhập trước.");
            return;
        }
        switch (op) {
            case "LIST" -> lobby(p);
            case "RANKING" -> {
                Store.Page<Store.User> page = store.rankingPage(requestedPage(m));
                StringBuilder b = new StringBuilder();
                int rank = page.page() * Store.PAGE_SIZE;
                for (Store.User u : page.items())
                    b.append(++rank)
                            .append('|')
                            .append(display(u.name, 80))
                            .append('|')
                            .append(u.points)
                            .append('|')
                            .append(u.wins)
                            .append('|')
                            .append(u.losses)
                            .append('|')
                            .append(u.played)
                            .append('|')
                            .append(
                                    u.played == 0
                                            ? "0%"
                                            : String.format(
                                                    Locale.ROOT,
                                                    "%.1f%%",
                                                    100.0 * u.wins / u.played))
                            .append('\n');
                p.send("RANKING", b, page.page(), page.pages(), page.total());
            }
            case "HISTORY" -> {
                Store.Page<Store.Match> page = store.historyPage(p.user.name, requestedPage(m));
                StringBuilder b = new StringBuilder();
                for (Store.Match x : page.items())
                    b.append(display(x.id(), 80))
                            .append('|')
                            .append(display(x.first(), 80))
                            .append('|')
                            .append(display(x.second(), 80))
                            .append('|')
                            .append(x.scoreFirst() + ":" + x.scoreSecond())
                            .append('|')
                            .append(x.rounds())
                            .append('|')
                            .append(x.winner().isEmpty() ? "Hòa" : display(x.winner(), 80))
                            .append('|')
                            .append(java.time.Instant.ofEpochMilli(x.end()))
                            .append('|')
                            .append(display(x.reason(), 320))
                            .append('\n');
                p.send("HISTORY", b, page.page(), page.pages(), page.total());
            }
            case "INVITE" -> {
                need(m, 2);
                ClientHandler target = online.get(m[1]);
                if (target == null || target == p || target.room != null || p.room != null) {
                    p.send("ERROR", "Đối thủ không sẵn sàng.");
                    return;
                }
                if (invites.containsKey(m[1])
                        || invites.containsKey(p.user.name)
                        || invites.values().stream()
                                .anyMatch(i -> i.from.equals(p.user.name) || i.from.equals(m[1]))) {
                    p.send("ERROR", "Một trong hai người đang có lời mời chờ xử lý.");
                    return;
                }
                invites.put(m[1], new Invite(p.user.name, System.currentTimeMillis() + 20000));
                target.send("INVITED", p.user.name);
                p.send("NOTICE", "Đã gửi lời mời (hết hạn sau 20 giây).");
            }
            case "ACCEPT", "REJECT" -> {
                need(m, 2);
                Invite i = invites.get(p.user.name);
                if (i == null || !i.from.equals(m[1]) || i.until < System.currentTimeMillis()) {
                    p.send("ERROR", "Lời mời đã hết hạn.");
                    return;
                }
                invites.remove(p.user.name);
                ClientHandler from = online.get(i.from);
                if (from == null) return;
                if (op.equals("REJECT")) {
                    from.send("NOTICE", p.user.name + " đã từ chối.");
                    return;
                }
                if (from.room != null || p.room != null) {
                    p.send("ERROR", "Người chơi đang bận.");
                    return;
                }
                start(from, p);
            }
            case "ACTION" -> {
                need(m, 7);
                Room r = p.room;
                if (r == null) return;
                if (!r.id.equals(m[1])
                        || r.round != Integer.parseInt(m[2])
                        || r.board.turn != Integer.parseInt(m[3])
                        || !r.phase.equals("TURN")) {
                    p.send("ERROR", "Nhịp đã thay đổi. Hãy chọn ở nhịp hiện tại.");
                    return;
                }
                int k = r.index(p);
                long now = System.currentTimeMillis();
                if (now >= r.deadline || r.actions[k] != null) {
                    p.send("ERROR", "Nhịp đã hết hạn hoặc bạn đã khóa hành động.");
                    return;
                }
                PlayerAction action = new PlayerAction(m[4], m[5], m[6]);
                String error =
                        new vn.edu.policethief.server.game.ActionValidator()
                                .validate(r.board, k == r.police, action);
                if (error != null) {
                    p.send("ACTION_REJECTED", error);
                    return;
                }
                r.actions[k] = action;
                p.send("LOCKED");
                if (r.actions[0] != null && r.actions[1] != null) r.resolve();
            }
            case "AGAIN" -> {
                Room r = p.room;
                if (r != null && r.phase.equals("FINISHED")) {
                    r.again[r.index(p)] = true;
                    p.send("NOTICE", "Đang chờ đối thủ đồng ý chơi lại.");
                    if (r.again[0] && r.again[1]) {
                        ClientHandler a = r.players[0], b = r.players[1];
                        rooms.remove(r);
                        a.room = null;
                        b.room = null;
                        start(a, b);
                    }
                }
            }
            case "EXIT" -> {
                if (p.room != null) leave(p);
            }
            case "LOGOUT" -> {
                if (p.room != null) leave(p);
                removeInvites(p.user.name);
                online.remove(p.user.name);
                p.user = null;
                p.send("LOGGED_OUT");
                lobbies();
            }
            default -> p.send("ERROR", "Lệnh không được hỗ trợ.");
        }
    }

    public void start(ClientHandler a, ClientHandler b) {
        Room r = rooms.create(this, a, b);
        lobbies();
        r.prepare();
    }

    public void lobby(ClientHandler p) {
        StringBuilder b = new StringBuilder();
        online.values().stream()
                .sorted(Comparator.comparing(x -> x.user.name))
                .forEach(
                        x ->
                                b.append(x.user.name)
                                        .append('|')
                                        .append(x.user.points)
                                        .append('|')
                                        .append(x.room == null ? "Rỗi" : "Đang chơi")
                                        .append('\n'));
        p.send("LOBBY", b);
    }

    public void lobbies() {
        for (ClientHandler p : online.values()) lobby(p);
    }

    public void removeInvites(String name) {
        Iterator<Map.Entry<String, Invite>> it = invites.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            if (e.getKey().equals(name) || e.getValue().from.equals(name)) {
                ClientHandler a = online.get(e.getKey()), b = online.get(e.getValue().from);
                if (a != null) a.send("INVITE_CLOSED");
                if (b != null) b.send("NOTICE", "Lời mời đã hủy.");
                it.remove();
            }
        }
    }

    public void tick() {
        long now = System.currentTimeMillis();
        if (!pendingResults.isEmpty() && now >= nextSaveRetry) {
            nextSaveRetry = now + 5000;
            retryResults();
        }
        Iterator<Map.Entry<String, Invite>> it = invites.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            if (e.getValue().until <= now) {
                ClientHandler a = online.get(e.getKey()), b = online.get(e.getValue().from);
                if (a != null) a.send("INVITE_CLOSED");
                if (b != null) b.send("NOTICE", "Lời mời hết hạn.");
                it.remove();
            }
        }
        for (Room r : new ArrayList<>(rooms)) r.tick(now);
    }

    public void retryResults() {
        for (Store.Match result : new ArrayList<>(pendingResults.values())) {
            try {
                store.finish(result);
                pendingResults.remove(result.id());
                for (String name : new String[] {result.first(), result.second()}) {
                    ClientHandler p = online.get(name);
                    if (p != null) p.send("NOTICE", "Đã lưu lại thành công kết quả trận đang chờ.");
                }
                lobbies();
            } catch (IOException e) {
                System.err.println(
                        "SQLite result still pending: " + result.id() + ": " + e.getMessage());
            }
        }
    }

    public synchronized void disconnect(ClientHandler p) {
        if (p.closed) return;
        p.closed = true;
        try {
            p.socket.close();
        } catch (IOException ignored) {
        }
        peers.remove(p);
        if (p.user != null) {
            if (p.room != null) leave(p);
            removeInvites(p.user.name);
            online.remove(p.user.name);
            lobbies();
        }
    }

    public void leave(ClientHandler p) {
        Room r = p.room;
        if (r == null) return;
        if (!r.phase.equals("FINISHED")) r.finish(1 - r.index(p), "Đầu hàng / mất kết nối");
        for (ClientHandler q : r.players) {
            q.room = null;
            q.send("BACK", "Đã trở về sảnh. " + p.user.name + " rời phòng.");
        }
        rooms.remove(r);
        lobbies();
    }

    public synchronized void close() {
        running = false;
        timer.shutdownNow();
        for (ClientHandler p : new ArrayList<>(peers)) disconnect(p);
        retryResults();
        try {
            listener.close();
        } catch (IOException ignored) {
        }
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5555;
        TcpServer s = new TcpServer(port, Path.of("data"));
        Runtime.getRuntime().addShutdownHook(new Thread(s::close));
        s.run();
    }
}
