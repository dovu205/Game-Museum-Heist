package vn.edu.policethief.client.controller;

import vn.edu.policethief.client.audio.Audio;
import vn.edu.policethief.client.model.ClientState;
import vn.edu.policethief.client.model.OnlinePlayer;
import vn.edu.policethief.client.network.GameClient;
import vn.edu.policethief.client.ui.MainFrame;
import vn.edu.policethief.client.ui.TablePage;
import vn.edu.policethief.shared.model.GameState;
import vn.edu.policethief.shared.model.PlayerAction;

import java.util.Arrays;
import java.util.List;

/** Coordinates player actions and authoritative server events on the Swing event thread. */
public final class GameController implements ClientActions {
    private final ClientState state = new ClientState();
    private final Audio audio = new Audio();
    private final MainFrame window;
    private final AutoCloseable localServer;
    private GameClient service;
    private int connectionVersion;
    private boolean closed;

    public GameController(boolean hosting) {
        this(hosting, null);
    }

    public GameController(boolean hosting, AutoCloseable localServer) {
        this.localServer = localServer;
        window = new MainFrame(hosting, state, this);
        showAuth(false);
        audio.music("menu");
    }

    public ClientState state() {
        return state;
    }

    public MainFrame window() {
        return window;
    }

    @Override
    public void showAuth(boolean signup) {
        state.view = signup ? "REGISTER" : "LOGIN";
        window.showAuth(signup);
    }

    @Override
    public void showLobby() {
        state.view = "LOBBY";
        state.match = "";
        state.finished = false;
        window.showLobby();
        audio.music("menu");
    }

    private void showGame() {
        state.view = "GAME";
        window.showGame();
        audio.music("chase");
    }

    @Override
    public void authenticate(
            boolean signup,
            String name,
            String password,
            String confirmation,
            String host,
            String port) {
        String user = name.trim();
        if (!user.matches("[A-Za-z0-9_]{3,20}") || password.length() < 6) {
            notice("Tên 3–20 ký tự chữ/số/_, mật khẩu tối thiểu 6 ký tự.");
            return;
        }
        if (signup && !password.equals(confirmation)) {
            notice("Hai mật khẩu không trùng nhau.");
            return;
        }
        int number;
        try {
            number = Integer.parseInt(port.trim());
            if (number < 1 || number > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            notice("Cổng không hợp lệ.");
            return;
        }
        window.setAuthEnabled(false);
        notice("Đang kết nối…");
        if (service != null) service.close();
        int version = ++connectionVersion;
        service =
                new GameClient(
                        message -> {
                            if (version == connectionVersion && !closed) receive(message);
                        },
                        javax.swing.SwingUtilities::invokeLater);
        GameClient current = service;
        current.connect(
                host.trim(),
                number,
                () -> current.send(signup ? "REGISTER" : "LOGIN", user, password));
    }

    @Override
    public void chooseMode(String mode) {
        if (state.locked || state.finished) return;
        boolean police = state.role.equals("POLICE");
        if (state.heldTurns > 0) {
            notice("Đang mắc bẫy: chỉ được đứng yên.");
            return;
        }
        if (mode.equals("TRAP") && (!police || state.trapsLeft <= 0)) return;
        if (mode.equals("SLIP") && (police || state.slipsLeft <= 0)) return;
        state.kind = mode;
        state.secondDirection = "STAY";
        if (mode.equals("SLIP")) state.direction = "STAY";
        else if (!mode.equals("MOVE") && state.direction.equals("STAY")) state.direction = "UP";
        window.updateControls();
        audio.effect("click");
    }

    @Override
    public void chooseDirection(String direction) {
        if (state.locked || state.finished) return;
        if (state.heldTurns > 0 && !direction.equals("STAY")) {
            notice("Đang mắc bẫy: chỉ được đứng yên.");
            return;
        }
        if (direction.equals("STAY")) {
            state.kind = "MOVE";
            state.direction = "STAY";
            state.secondDirection = "STAY";
        } else if (state.kind.equals("SLIP")) {
            if (state.secondDirection.equals("STAY")
                    && !state.direction.equals("STAY")
                    && GameState.perpendicular(state.direction, direction))
                state.secondDirection = direction;
            else {
                state.direction = direction;
                state.secondDirection = "STAY";
            }
        } else state.direction = direction;
        window.updateControls();
        audio.effect("click");
    }

    @Override
    public void submit() {
        if (state.locked || state.finished) return;
        GameState preview = new GameState(GameState.FIXED_EXIT);
        preview.cop = state.pos;
        preview.thief = state.pos;
        preview.trapsLeft = state.trapsLeft;
        preview.slipsLeft = state.slipsLeft;
        preview.heldTurns = state.heldTurns;
        for (int cell : state.visibleTraps) preview.traps.put(cell, 0);
        String error =
                preview.validate(
                        state.role.equals("POLICE"),
                        new PlayerAction(state.kind, state.direction, state.secondDirection));
        if (error != null) {
            notice(error);
            window.appendLog(error);
            return;
        }
        state.locked = true;
        window.updateControls();
        send(
                "ACTION",
                state.match,
                state.round,
                state.turn,
                state.kind,
                state.direction,
                state.secondDirection);
        audio.effect("lock");
        notice("Đã gửi hành động. Chờ server phân xử.");
    }

    private void send(String type, Object... args) {
        if (service != null) service.send(type, args);
    }

    @Override
    public void refreshPlayers() {
        send("LIST");
    }

    @Override
    public void logout() {
        send("LOGOUT");
    }

    @Override
    public void leaveMatch() {
        send("EXIT");
    }

    @Override
    public void replay() {
        send("AGAIN");
    }

    @Override
    public void answerInvite(String name, boolean accept) {
        send(accept ? "ACCEPT" : "REJECT", name);
    }

    @Override
    public void mute(boolean muted) {
        audio.mute(muted);
    }

    @Override
    public void volume(int value) {
        audio.volume(value);
    }

    @Override
    public void rules() {
        window.rules();
    }

    @Override
    public void connectionDialog() {
        window.connectionDialog();
    }

    @Override
    public void notice(String message) {
        window.notice(message);
    }

    @Override
    public void invitePlayer(String name) {
        if (name.equals(state.username)
                || state.onlinePlayers.stream()
                        .noneMatch(
                                player ->
                                        player.name().equals(name)
                                                && player.status().equals("Rỗi"))) {
            notice("Chọn người khác đang rỗi.");
            return;
        }
        send("INVITE", name);
    }

    @Override
    public void request(String type) {
        if (!state.username.isEmpty()) send(type);
    }

    @Override
    public void requestPage(String type, int page) {
        if (!state.username.isEmpty()) send(type, page);
    }

    private List<List<String>> parseRows(String text) {
        return text.lines()
                .filter(line -> !line.isBlank())
                .map(line -> List.of(line.split("\\|", -1)))
                .toList();
    }

    private void readClues(String text) {
        state.clues.clear();
        for (String part : text.split(";")) {
            if (part.isBlank()) continue;
            String[] clue = part.split(",");
            state.clues.add(
                    new GameState.Clue(
                            Integer.parseInt(clue[0]), clue[1], Integer.parseInt(clue[2])));
        }
    }

    private int[] parsePath(String text) {
        return text.isBlank()
                ? new int[0]
                : Arrays.stream(text.split(",")).mapToInt(Integer::parseInt).toArray();
    }

    public void receive(String[] message) {
        try {
            switch (message[0]) {
                case "LOGIN_OK" -> {
                    state.username = message[1];
                    window.setIdentity(state.username);
                    window.clearPasswords();
                    showLobby();
                    notice("Chào " + state.username + "! Chọn đối thủ để thách đấu.");
                }
                case "REGISTERED" -> {
                    showAuth(false);
                    notice("Tạo tài khoản thành công. Bạn có thể đăng nhập.");
                }
                case "ERROR", "NOTICE" -> {
                    notice(message[1]);
                    if (state.isAuth()) window.setAuthEnabled(true);
                }
                case "ACTION_REJECTED" -> {
                    state.locked = false;
                    window.updateControls();
                    notice(message[1]);
                    window.appendLog(message[1]);
                }
                case "LOBBY" -> {
                    state.onlinePlayers =
                            parseRows(message[1]).stream()
                                    .map(
                                            row ->
                                                    new OnlinePlayer(
                                                            row.get(0), row.get(1), row.get(2)))
                                    .toList();
                    window.updatePlayers();
                }
                case "RANKING", "HISTORY" -> {
                    if (!state.username.isEmpty())
                        window.showData(
                                new TablePage(
                                        message[0],
                                        parseRows(message[1]),
                                        Integer.parseInt(message[2]),
                                        Integer.parseInt(message[3]),
                                        Integer.parseInt(message[4])));
                }
                case "INVITED" -> {
                    audio.effect("invite");
                    window.invite(message[1]);
                }
                case "INVITE_CLOSED" -> {
                    window.closeInvitation();
                    notice("Lời mời đã hủy hoặc hết hạn.");
                }
                case "STATE" -> startTurn(message);
                case "LOCKED" -> {
                    state.locked = true;
                    window.updateControls();
                    notice("Đã khóa hành động. Lựa chọn đối thủ vẫn bí mật.");
                }
                case "UPDATE" -> resolveTurn(message);
                case "ROUND_END" -> endRound(message);
                case "FINISHED" -> finishMatch(message);
                case "BACK" -> {
                    showLobby();
                    notice(message[1]);
                    refreshPlayers();
                }
                case "LOGGED_OUT", "DISCONNECTED" -> {
                    state.match = "";
                    state.username = "";
                    window.setIdentity("");
                    showAuth(false);
                    notice(message[0].equals("LOGGED_OUT") ? "Đã đăng xuất." : message[1]);
                    audio.music("menu");
                }
                default -> {
                    /* Forward-compatible unknown event. */
                }
            }
        } catch (RuntimeException e) {
            notice("Không đọc được thông điệp: " + e.getClass().getSimpleName());
        }
    }

    private void startTurn(String[] m) {
        window.closeInvitation();
        boolean fresh = !state.match.equals(m[1]) || state.round != Integer.parseInt(m[2]);
        state.match = m[1];
        state.round = Integer.parseInt(m[2]);
        state.turn = Integer.parseInt(m[3]);
        state.role = m[4];
        state.opponent = m[5];
        state.score = Integer.parseInt(m[6]);
        state.otherScore = Integer.parseInt(m[7]);
        state.pos = Integer.parseInt(m[8]);
        state.exitCell = GameState.FIXED_EXIT;
        state.relics = m.length > 18 ? parsePath(m[18]) : GameState.RELICS.clone();
        state.relicMask = Integer.parseInt(m[10]);
        state.carrying = Integer.parseInt(m[11]);
        state.trapsLeft = Integer.parseInt(m[12]);
        state.slipsLeft = Integer.parseInt(m[13]);
        readClues(m[14]);
        state.deadline = Long.parseLong(m[15]);
        state.serverOffset = Long.parseLong(m[16]) - System.currentTimeMillis();
        state.locked = false;
        state.finished = false;
        state.ending = "";
        state.enemyPos = m.length > 19 ? Integer.parseInt(m[19]) : -1;
        state.visibleTraps = m.length > 20 ? parsePath(m[20]) : new int[0];
        state.heldTurns = m.length > 21 ? Integer.parseInt(m[21]) : 0;
        state.intelligence = m.length > 22 ? m[22] : "";
        state.exits = m.length > 23 ? parsePath(m[23]) : GameState.EXITS.clone();
        state.kind = "MOVE";
        state.direction = "STAY";
        state.secondDirection = "STAY";
        state.lastTick = -1;
        state.animationPath = new int[0];
        if (!state.view.equals("GAME")) showGame();
        if (fresh) {
            window.clearLog();
            window.appendLog(m[17]);
            audio.effect("round");
        }
        window.renderTurn();
        tick();
        notice("Chọn hành động rồi nhấn Khóa hoặc Enter.");
        audio.music(state.turn >= 29 ? "finale" : "chase");
    }

    private void resolveTurn(String[] m) {
        if (!state.match.equals(m[1])) return;
        state.locked = true;
        state.deadline = 0;
        state.animationFrom = state.pos;
        state.animationPath = parsePath(m[4]);
        state.animationStart = System.currentTimeMillis();
        state.pos = Integer.parseInt(m[5]);
        readClues(m[6]);
        state.relicMask = Integer.parseInt(m[7]);
        state.carrying = Integer.parseInt(m[8]);
        state.trapsLeft = Integer.parseInt(m[9]);
        state.slipsLeft = Integer.parseInt(m[10]);
        window.appendLog(m[11]);
        state.intelligence = m[12];
        state.visibleTraps = m.length > 18 ? parsePath(m[18]) : new int[0];
        state.heldTurns = m.length > 19 ? Integer.parseInt(m[19]) : 0;
        state.exits = m.length > 20 ? parsePath(m[20]) : GameState.EXITS.clone();
        state.ending = m[13];
        state.enemyPos = Integer.parseInt(m[14]);
        state.exitCell = GameState.FIXED_EXIT;
        window.setCountdown("PHÂN XỬ");
        window.updateControls();
        if (m[11].contains("Đã lấy báu vật")) audio.effect("drop");
        else if (m[11].contains("tiếng động")) audio.effect("click");
    }

    private void endRound(String[] m) {
        state.locked = true;
        state.deadline = 0;
        state.score = Integer.parseInt(m[3]);
        state.otherScore = Integer.parseInt(m[4]);
        state.enemyPos = Integer.parseInt(m[state.role.equals("POLICE") ? 7 : 6]);
        state.exitCell = Integer.parseInt(m[8]);
        window.renderScore();
        window.appendLog(m[5]);
        notice(m[5]);
        window.setCountdown(m[1].equals("WIN") ? "THẮNG VÁN" : "THUA VÁN");
        audio.effect(
                state.ending.equals("CAPTURE")
                        ? "caught"
                        : state.ending.equals("ESCAPE") ? "escape" : "lose");
        window.updateControls();
    }

    private void finishMatch(String[] m) {
        state.locked = true;
        state.finished = true;
        state.deadline = 0;
        state.score = Integer.parseInt(m[2]);
        state.otherScore = Integer.parseInt(m[3]);
        window.renderScore();
        window.setCountdown(m[1].equals("DRAW") ? "HÒA" : m[1].equals("WIN") ? "THẮNG" : "THUA");
        window.appendLog(
                "Kết thúc trận "
                        + state.score
                        + ":"
                        + state.otherScore
                        + ". Cả hai chọn Chơi lại để đấu trận mới.");
        window.showReplay();
        window.updateControls();
        audio.music("menu");
        audio.effect(m[1].equals("WIN") ? "victory" : "round");
    }

    @Override
    public void tick() {
        window.updateClock();
        if (!state.view.equals("GAME") || state.deadline == 0 || state.finished) return;
        long left = Math.max(0, state.deadline - System.currentTimeMillis() - state.serverOffset);
        int seconds = (int) Math.ceil(left / 1000.0);
        if (!state.locked && seconds <= 2 && seconds > 0 && seconds != state.lastTick) {
            audio.effect("tick");
            state.lastTick = seconds;
        }
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        connectionVersion++;
        if (service != null) service.close();
        if (localServer != null)
            try {
                localServer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        audio.close();
        window.close();
    }
}
