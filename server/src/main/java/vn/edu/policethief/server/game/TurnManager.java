package vn.edu.policethief.server.game;

import vn.edu.policethief.shared.GameRules;
import vn.edu.policethief.shared.model.PlayerAction;

public final class TurnManager {
    public long deadline(long now) {
        return now + GameRules.TURN_MS;
    }

    public PlayerAction effective(PlayerAction action) {
        return action == null ? PlayerAction.stay() : action;
    }

    public boolean ready(PlayerAction[] actions, long now, long deadline) {
        return now >= deadline || (actions[0] != null && actions[1] != null);
    }
}
