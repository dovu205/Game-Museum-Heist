package vn.edu.policethief.server.game;

import vn.edu.policethief.shared.GameRules;
import vn.edu.policethief.shared.model.GameState;

public final class WinConditionService {
    public boolean caught(GameState s, int oldCop, int oldThief) {
        return s.cop == s.thief || (oldCop == s.thief && oldThief == s.cop);
    }

    public boolean escaped(GameState s) {
        return s.carrying && GameRules.isExit(s.thief);
    }

    public String ending(GameState s, boolean caught, boolean escaped) {
        return caught
                ? "CAPTURE"
                : escaped ? "ESCAPE" : s.turn >= GameRules.MAX_TURNS ? "TIMEOUT" : "NONE";
    }
}
