package vn.edu.policethief.server.game;

import vn.edu.policethief.shared.model.*;

public final class ActionValidator {
    public String validate(GameState state, boolean police, PlayerAction action) {
        return state.validate(police, action);
    }
}
