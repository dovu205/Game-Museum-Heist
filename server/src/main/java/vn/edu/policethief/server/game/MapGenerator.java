package vn.edu.policethief.server.game;

import vn.edu.policethief.shared.GameRules;

import java.util.*;

/** Reject disconnected maps before selecting distinct, non-exit spawn/pedestal cells. */
public final class MapGenerator {
    public GameSession generate(Random random) {
        try (java.io.InputStream in = MapGenerator.class.getResourceAsStream("/maps/map01.txt")) {
            if (in == null) throw new IllegalStateException("Missing map01.txt");
            String[] rows =
                    new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                            .strip()
                            .split("\\R");
            if (!java.util.Arrays.equals(rows, GameRules.MAP))
                throw new IllegalStateException("map01.txt differs from shared GameRules.MAP");
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot load map", e);
        }
        List<Integer> cells = new ArrayList<>();
        for (int i = 0; i < 49; i++) if (GameRules.walkable(i)) cells.add(i);
        Set<Integer> seen = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(cells.get(0));
        while (!queue.isEmpty()) {
            int c = queue.remove();
            if (!seen.add(c)) continue;
            for (String d : GameRules.DIRECTIONS) {
                int n = GameRules.adjacent(c, d);
                if (!seen.contains(n)) queue.add(n);
            }
        }
        if (seen.size() != cells.size()) throw new IllegalStateException("Map is disconnected");
        cells.removeIf(GameRules::isExit);
        Collections.shuffle(cells, random);
        GameSession s = new GameSession();
        s.cop = cells.get(0);
        s.thief = cells.get(1);
        s.relics[0] = cells.get(2);
        s.relics[1] = cells.get(3);
        return s;
    }
}
