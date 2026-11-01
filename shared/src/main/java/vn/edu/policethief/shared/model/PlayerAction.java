package vn.edu.policethief.shared.model;

import java.util.Objects;

public record PlayerAction(String kind, String direction, String secondDirection) {
    public PlayerAction {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(direction);
        Objects.requireNonNull(secondDirection);
    }

    public PlayerAction(String kind, String direction) {
        this(kind, direction, "STAY");
    }

    public PlayerAction(ActionType type, Direction first, Direction second) {
        this(type.name(), first.name(), second.name());
    }

    public static PlayerAction stay() {
        return new PlayerAction("MOVE", "STAY");
    }
}
