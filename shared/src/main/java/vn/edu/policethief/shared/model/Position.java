package vn.edu.policethief.shared.model;

/** Immutable board coordinate. Packet messages continue to use a flat cell index. */
public record Position(int x, int y) {
    public Position {
        if (x < 0 || x >= GameState.SIZE || y < 0 || y >= GameState.SIZE) {
            throw new IllegalArgumentException("Position is outside the museum");
        }
    }

    public static Position fromCell(int cell) {
        if (cell < 0 || cell >= GameState.SIZE * GameState.SIZE) {
            throw new IllegalArgumentException("Invalid cell index");
        }
        return new Position(cell % GameState.SIZE, cell / GameState.SIZE);
    }

    public int toCell() {
        return y * GameState.SIZE + x;
    }
}
