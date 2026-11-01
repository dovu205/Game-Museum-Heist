package vn.edu.policethief.server.game;

import vn.edu.policethief.shared.model.*;

/** Mutable server board; all calls run under the owning TCP server monitor. */
public final class GameSession extends GameState {
    public GameSession() {
        super();
    }

    public GameSession(int exit) {
        super(exit);
    }

    public Result resolve(PlayerAction police, PlayerAction thief) {
        return new TurnResolver().resolve(this, police, thief);
    }
}
