package vn.edu.policethief.server.game;

import vn.edu.policethief.shared.GameRules;
import vn.edu.policethief.shared.model.GameState;

public final class VisibilityService {
    public boolean lineOfSight(int a, int b) {
        int dx = GameRules.x(b) - GameRules.x(a), dy = GameRules.y(b) - GameRules.y(a);
        if (dx != 0 && dy != 0 || Math.abs(dx) + Math.abs(dy) > 2) return false;
        String direction = dx > 0 ? "RIGHT" : dx < 0 ? "LEFT" : dy > 0 ? "DOWN" : "UP";
        int c = a;
        for (int i = 0; i < Math.abs(dx) + Math.abs(dy); i++) {
            int n = GameRules.adjacent(c, direction);
            if (n == c) return false;
            c = n;
        }
        return true;
    }

    public int opponent(GameState s, boolean police, boolean ended) {
        return ended
                        || (s.hasResolved && lineOfSight(s.cop, s.thief))
                        || (police && s.turn <= s.revealUntil)
                ? (police ? s.thief : s.cop)
                : -1;
    }

    public String intelligence(GameState s) {
        return s.turn % 8 == 0 && s.turn <= 24
                ? "Tình báo nhịp "
                        + s.turn
                        + ": trộm ở khu "
                        + (GameRules.y(s.thief) <= 1
                                ? "Bắc (hàng 1–2)"
                                : GameRules.y(s.thief) <= 4
                                        ? "Trung tâm (hàng 3–5)"
                                        : "Nam (hàng 6–7)")
                        + "."
                : "";
    }
}
