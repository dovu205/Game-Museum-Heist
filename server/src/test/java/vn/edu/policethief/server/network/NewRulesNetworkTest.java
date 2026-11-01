package vn.edu.policethief.server.network;

import vn.edu.policethief.server.game.GameSession;
import vn.edu.policethief.server.lobby.Room;

import java.nio.file.*;

/** Verify role-filtered version-2 packets over real TCP, including trap holding. */
public final class NewRulesNetworkTest {
    static int checks;

    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }

    static String[][] fixture(
            TcpServer server, NetworkTest.Bot cop, NetworkTest.Bot thief, GameSession board)
            throws Exception {
        synchronized (server) {
            Room room = server.rooms.get(0);
            room.board = board;
            room.beginTurn("Version 2 test fixture");
        }
        return new String[][] {cop.await("STATE"), thief.await("STATE")};
    }

    static GameSession board(int cop, int thief) {
        GameSession board = new GameSession();
        board.cop = cop;
        board.thief = thief;
        return board;
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("work"));
        try (TcpServer server =
                new TcpServer(0, Files.createTempDirectory(Path.of("work"), "rules-v2-"))) {
            Thread runner =
                    new Thread(
                            () -> {
                                try {
                                    server.run();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
            runner.setDaemon(true);
            runner.start();
            try (NetworkTest.Bot a = new NetworkTest.Bot(server.port());
                    NetworkTest.Bot b = new NetworkTest.Bot(server.port())) {
                a.send("REGISTER", "officer", "secret123");
                a.await("REGISTERED");
                b.send("REGISTER", "runner", "secret123");
                b.await("REGISTERED");
                a.send("LOGIN", "officer", "secret123");
                a.await("LOGIN_OK");
                b.send("LOGIN", "runner", "secret123");
                b.await("LOGIN_OK");
                a.send("INVITE", "runner");
                b.await("INVITED");
                b.send("ACCEPT", "officer");
                String[] first = a.await("STATE");
                b.await("STATE");
                NetworkTest.Bot cop = first[4].equals("POLICE") ? a : b, thief = cop == a ? b : a;
                GameSession board = board(48, 3);
                board.traps.put(2, 1);
                String[][] state = fixture(server, cop, thief, board);
                check(state[0].length == 24 && state[0][23].equals("42,6"), "two exits in STATE");
                check(
                        state[0][20].equals("2") && state[1][20].isEmpty(),
                        "untriggered trap filtered from thief STATE");
                NetworkTest.act(cop, state[0], "MOVE", "STAY");
                cop.await("LOCKED");
                long sent = System.currentTimeMillis();
                NetworkTest.act(thief, state[1], "DASH", "LEFT");
                thief.await("LOCKED");
                String[] cu = cop.await("UPDATE"), tu = thief.await("UPDATE");
                check(
                        System.currentTimeMillis() - sent < 2000,
                        "two submissions resolve without waiting eight seconds");
                check(
                        tu[4].equals("2") && tu[5].equals("2") && tu[19].equals("1"),
                        "dash halted and next turn held");
                check(
                        cu[14].equals("2") && tu[14].equals("-1"),
                        "trap reveals distant thief only to police");
                check(
                        tu[18].equals("2") && cu[15].isEmpty() && tu[15].isEmpty(),
                        "activated trap known, opponent paths stay private");
                NetworkTest.advance(server);
                String[] cs = cop.await("STATE"), ts = thief.await("STATE");
                check(
                        ts[21].equals("1") && cs[19].equals("2"),
                        "held state and reveal survive turn transition");
                NetworkTest.act(thief, ts, "MOVE", "LEFT");
                thief.await("ACTION_REJECTED");
                checks++;
                NetworkTest.act(thief, ts, "DASH", "LEFT");
                thief.await("ACTION_REJECTED");
                checks++;
                thief.send("ACTION", ts[1], ts[2], ts[3], "SLIP", "LEFT", "UP");
                thief.await("ACTION_REJECTED");
                checks++;
                NetworkTest.act(thief, ts, "MOVE", "STAY");
                thief.await("LOCKED");
                NetworkTest.act(cop, cs, "MOVE", "STAY");
                cop.await("LOCKED");
                cu = cop.await("UPDATE");
                tu = thief.await("UPDATE");
                check(
                        tu[19].equals("0") && tu[18].equals("2"),
                        "one held turn expires without consuming trap");
                board = board(0, 48);
                board.turn = 8;
                state = fixture(server, cop, thief, board);
                NetworkTest.act(cop, state[0], "MOVE", "STAY");
                cop.await("LOCKED");
                NetworkTest.act(thief, state[1], "MOVE", "STAY");
                thief.await("LOCKED");
                cu = cop.await("UPDATE");
                tu = thief.await("UPDATE");
                check(
                        cu[12].contains("Nam") && tu[12].isEmpty() && cu[14].equals("-1"),
                        "regional intelligence private, no exact coordinate");
                board = board(0, 2);
                board.hasResolved = true;
                state = fixture(server, cop, thief, board);
                check(
                        state[0][19].equals("2") && state[1][19].equals("0"),
                        "mutual line of sight in STATE");
                NetworkTest.act(cop, state[0], "MOVE", "STAY");
                cop.await("LOCKED");
                NetworkTest.act(thief, state[1], "MOVE", "RIGHT");
                thief.await("LOCKED");
                cu = cop.await("UPDATE");
                tu = thief.await("UPDATE");
                check(
                        cu[14].equals("-1") && tu[14].equals("-1"),
                        "opponent hidden after leaving sight");
                board = board(0, 48);
                board.relics[0] = 47;
                state = fixture(server, cop, thief, board);
                NetworkTest.act(cop, state[0], "DASH", "RIGHT");
                cop.await("LOCKED");
                NetworkTest.act(thief, state[1], "DASH", "LEFT");
                thief.await("LOCKED");
                cu = cop.await("UPDATE");
                tu = thief.await("UPDATE");
                check(cu[5].equals("2") && tu[5].equals("46"), "both roles dash over TCP");
                check(
                        cu[7].equals("-1")
                                && cu[8].equals("-1")
                                && cu[10].equals("-1")
                                && cu[11].contains("Chuông"),
                        "loot and thief resource state private to thief");
                check(
                        cu[6].contains("48,NOISE") && !cu[6].contains("0,NOISE") && tu[6].isEmpty(),
                        "only thief dash makes police-visible noise");
                check(tu[8].equals("1") && tu[7].equals("2"), "treasure collected during dash");
            }
        }
        System.out.println("PASS: " + checks + " version-2 TCP privacy and skill assertions.");
    }
}
