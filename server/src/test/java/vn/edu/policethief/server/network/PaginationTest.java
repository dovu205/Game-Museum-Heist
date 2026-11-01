package vn.edu.policethief.server.network;

import vn.edu.policethief.server.Store;
import vn.edu.policethief.shared.protocol.Packet;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public final class PaginationTest {
    static int checks;

    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }

    static String[] response(NetworkTest.Bot bot, String type) throws Exception {
        String[] message = bot.await(type);
        String encoded =
                Packet.encode(type, (Object[]) Arrays.copyOfRange(message, 1, message.length));
        check(encoded.length() <= Packet.MAX_LINE, "response stays within protocol line limit");
        check(
                Packet.decode(Packet.read(new BufferedReader(new StringReader(encoded + "\n"))))[0]
                        .equals(type),
                "response decodes through real bounded reader");
        return message;
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("work"));
        Path directory = Files.createTempDirectory(Path.of("work"), "pagination-");
        try (TcpServer server = new TcpServer(0, directory)) {
            server.store.register("alice", "secret123");
            server.store.register("bob", "secret456");
            try (Connection c = StoreTest.connect(directory);
                    PreparedStatement s =
                            c.prepareStatement(
                                    "INSERT INTO"
                                        + " users(name,salt,password_hash,points,wins,losses,played)"
                                        + " SELECT ?,salt,password_hash,?,0,0,0 FROM users WHERE"
                                        + " name='alice'")) {
                for (int i = 0; i < 35; i++) {
                    s.setString(1, String.format(Locale.ROOT, "user%02d", i));
                    s.setInt(2, i);
                    s.executeUpdate();
                }
            }
            String longReason = "😀|Lý do rất dài\n".repeat(2000);
            Set<String> expected = new HashSet<>();
            for (int i = 0; i < 125; i++) {
                String id = UUID.randomUUID().toString();
                expected.add(id);
                server.store.finish(
                        new Store.Match(
                                id,
                                "alice",
                                "bob",
                                1,
                                1,
                                2,
                                "",
                                100,
                                200,
                                i == 124 ? longReason : "Bảo tàng · Hoàn tất hai ván đổi vai"));
            }
            server.store.finish(
                    new Store.Match(
                            "private-other-match",
                            "user00",
                            "user01",
                            0,
                            1,
                            1,
                            "user01",
                            1,
                            2,
                            "Other users"));
            Thread thread =
                    new Thread(
                            () -> {
                                try {
                                    server.run();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
            thread.setDaemon(true);
            thread.start();
            try (NetworkTest.Bot bot = new NetworkTest.Bot(server.port());
                    NetworkTest.Bot empty = new NetworkTest.Bot(server.port())) {
                bot.send("LOGIN", "alice", "secret123");
                bot.await("LOGIN_OK");
                Set<String> seen = new HashSet<>();
                for (int page = 0; page < 13; page++) {
                    if (page == 0) bot.send("HISTORY");
                    else bot.send("HISTORY", page);
                    String[] message = response(bot, "HISTORY");
                    check(
                            message[2].equals(String.valueOf(page))
                                    && message[3].equals("13")
                                    && message[4].equals("125"),
                            "history page metadata");
                    String[] rows = message[1].split("\n");
                    check(rows.length == (page == 12 ? 5 : 10), "bounded page size");
                    for (String row : rows) {
                        String[] cells = row.split("\\|", -1);
                        check(cells.length == 8, "sanitized history keeps all eight columns");
                        check(
                                seen.add(cells[0]),
                                "no duplicate matches across tied-timestamp pages");
                    }
                }
                check(
                        seen.equals(expected),
                        "all 125 matches available beyond old 100-match cap; private matches"
                            + " excluded");
                boolean storedFullReason = false;
                for (int page = 0; page < 13; page++)
                    storedFullReason |=
                            server.store.historyPage("alice", page).items().stream()
                                    .anyMatch(m -> m.reason().equals(longReason));
                check(storedFullReason, "display bound does not truncate stored reason");
                bot.send("HISTORY", Integer.MAX_VALUE);
                check(
                        response(bot, "HISTORY")[2].equals("12"),
                        "out-of-range history clamps to final page");
                bot.send("HISTORY", -1);
                bot.await("ERROR");
                checks++;
                bot.send("HISTORY", "abc");
                bot.await("ERROR");
                checks++;
                Set<String> names = new HashSet<>();
                int rank = 0;
                for (int page = 0; page < 4; page++) {
                    bot.send("RANKING", page);
                    String[] message = response(bot, "RANKING");
                    check(
                            message[2].equals(String.valueOf(page))
                                    && message[3].equals("4")
                                    && message[4].equals("37"),
                            "ranking page metadata");
                    for (String row : message[1].split("\n")) {
                        String[] cells = row.split("\\|", -1);
                        check(
                                Integer.parseInt(cells[0]) == ++rank,
                                "ranking numbers continue across pages");
                        check(names.add(cells[1]), "ranking users not duplicated");
                    }
                }
                check(names.size() == 37, "all ranking users accessible");
                empty.send("LOGIN", "user34", "secret123");
                empty.await("LOGIN_OK");
                empty.send("HISTORY");
                String[] message = response(empty, "HISTORY");
                check(
                        message[1].isEmpty()
                                && message[2].equals("0")
                                && message[3].equals("1")
                                && message[4].equals("0"),
                        "empty history has valid navigation");
                bot.send("PING");
                bot.await("PONG");
                checks++;
            }
        }
        System.out.println("PASS: " + checks + " TCP pagination regression assertions.");
    }
}
