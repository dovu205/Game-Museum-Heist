package vn.edu.policethief.server.game;

import vn.edu.policethief.shared.GameRules;
import vn.edu.policethief.shared.model.*;

public final class TrapService {
    public void place(GameState s, PlayerAction action) {
        if (action.kind().equals("TRAP")) {
            s.traps.put(GameRules.adjacent(s.cop, action.direction()), s.turn + 1);
            s.trapsLeft--;
        }
    }

    public boolean enter(GameState s, int previous) {
        if (previous == s.thief || s.traps.getOrDefault(s.thief, Integer.MAX_VALUE) > s.turn)
            return false;
        s.knownTraps.add(s.thief);
        s.heldTurns = 1;
        s.revealUntil = s.turn + 1;
        return true;
    }

    public String visible(GameState s, boolean police) {
        return GameRules.cells(police ? s.traps.keySet() : s.knownTraps);
    }
}
