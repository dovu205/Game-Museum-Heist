package vn.edu.policethief.shared;

import vn.edu.policethief.shared.model.*;

import java.util.*;

/** Shared geometry and validation for previews; the server repeats every check. */
public class GameRules {
    public static final int SIZE = 7,
            MAX_TURNS = 32,
            TURN_MS = 8000,
            TRAP_LIMIT = 2,
            SLIP_LIMIT = 3;
    public static final String[] MAP = {
        ".......", ".##.#..", "...#...", ".#...#.", "...#...", "..#.##.", "......."
    };
    public static final int COP_START = 0, THIEF_START = 48, FIXED_EXIT = 42;
    public static final int[] EXITS = {42, 6}, RELICS = {29, 19};
    public static final String[] DIRECTIONS = {"UP", "RIGHT", "DOWN", "LEFT", "STAY"};

    public static int x(int cell) {
        return cell % SIZE;
    }

    public static int y(int cell) {
        return cell / SIZE;
    }

    public static boolean walkable(int cell) {
        return cell >= 0 && cell < SIZE * SIZE && MAP[y(cell)].charAt(x(cell)) != '#';
    }

    public static int direction(String value) {
        for (int i = 0; i < DIRECTIONS.length; i++) if (DIRECTIONS[i].equals(value)) return i;
        throw new IllegalArgumentException("Hướng không hợp lệ.");
    }

    public static boolean isExit(int cell) {
        return Arrays.stream(EXITS).anyMatch(e -> e == cell);
    }

    public static boolean trapAllowed(int cell) {
        return walkable(cell)
                && Arrays.stream(EXITS)
                        .noneMatch(
                                e ->
                                        Math.max(Math.abs(x(e) - x(cell)), Math.abs(y(e) - y(cell)))
                                                <= 1);
    }

    public static int adjacent(int cell, String value) {
        int d = direction(value);
        int[] dx = {0, 1, 0, -1, 0}, dy = {-1, 0, 1, 0, 0};
        int nx = x(cell) + dx[d], ny = y(cell) + dy[d];
        if (nx < 0 || nx >= SIZE || ny < 0 || ny >= SIZE) return cell;
        int n = ny * SIZE + nx;
        return walkable(n) ? n : cell;
    }

    public static List<Integer> path(int cell, String dir, int steps) {
        List<Integer> p = new ArrayList<>();
        for (int i = 0; i < steps; i++) {
            int n = adjacent(cell, dir);
            if (n == cell) break;
            p.add(n);
            cell = n;
        }
        return p;
    }

    public static boolean perpendicular(String a, String b) {
        int x = direction(a), y = direction(b);
        return x < 4 && y < 4 && x % 2 != y % 2;
    }

    public static List<Integer> path(int cell, PlayerAction action) {
        if (action.kind().equals("MOVE")) return path(cell, action.direction(), 1);
        if (action.kind().equals("DASH")) return path(cell, action.direction(), 2);
        if (!action.kind().equals("SLIP")) return List.of();
        List<Integer> p = new ArrayList<>(path(cell, action.direction(), 1));
        if (!p.isEmpty()) p.addAll(path(p.get(0), action.secondDirection(), 1));
        return p;
    }

    public static String validate(GameState s, boolean police, PlayerAction a) {
        int pos = police ? s.cop : s.thief;
        try {
            direction(a.direction());
            direction(a.secondDirection());
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
        if (!police
                && s.heldTurns > 0
                && !(a.kind().equals("MOVE") && a.direction().equals("STAY")))
            return "Bạn đang mắc bẫy: phải đứng yên nhịp này.";
        return switch (a.kind()) {
            case "MOVE" ->
                    !a.direction().equals("STAY") && adjacent(pos, a.direction()) == pos
                            ? "Đường bị tường chắn. Chọn hướng khác."
                            : null;
            case "DASH" ->
                    a.direction().equals("STAY") || adjacent(pos, a.direction()) == pos
                            ? "Chọn hướng chạy không bị chắn."
                            : null;
            case "TRAP" ->
                    !police
                            ? "Chỉ cảnh sát được đặt bẫy."
                            : s.trapsLeft <= 0
                                    ? "Bạn đã hết lượt đặt bẫy."
                                    : a.direction().equals("STAY")
                                                    || adjacent(pos, a.direction()) == pos
                                            ? "Chọn ô đường đi kề để đặt bẫy."
                                            : !trapAllowed(adjacent(pos, a.direction()))
                                                    ? "Không đặt bẫy tại hoặc sát cửa thoát, kể cả"
                                                          + " ô chéo."
                                                    : s.traps.containsKey(
                                                                    adjacent(pos, a.direction()))
                                                            ? "Ô này đã có bẫy."
                                                            : null;
            case "SLIP" ->
                    police
                            ? "Chỉ trộm được luồn lách."
                            : s.slipsLeft <= 0
                                    ? "Bạn đã hết lượt luồn lách."
                                    : !perpendicular(a.direction(), a.secondDirection())
                                            ? "Chọn hai hướng vuông góc."
                                            : adjacent(pos, a.direction()) == pos
                                                    ? "Bước đầu bị chặn. Chọn hướng khác."
                                                    : null;
            default -> "Hành động không hợp lệ.";
        };
    }

    public static String grid() {
        return String.join("", MAP);
    }

    public static String cellName(int cell) {
        return cell < 0 ? "?" : String.valueOf((char) ('A' + x(cell))) + (y(cell) + 1);
    }

    public static String cells(Collection<Integer> cells) {
        return String.join(",", cells.stream().map(String::valueOf).toList());
    }
}
