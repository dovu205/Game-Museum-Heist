package vn.edu.policethief.shared.model;

import vn.edu.policethief.shared.GameRules;

import java.util.*;

/** Complete authoritative board. Only role-filtered fields may be sent over TCP. */
public class GameState extends GameRules {
    public record Clue(int cell, String kind, int until) {}

    public record Result(
            String ending,
            boolean collected,
            boolean noise,
            boolean trace,
            boolean trapped,
            int oldCop,
            int oldThief,
            List<Integer> copPath,
            List<Integer> thiefPath) {}

    public int cop = COP_START,
            thief = THIEF_START,
            exit = FIXED_EXIT,
            turn = 1,
            relicMask = 3,
            trapsLeft = TRAP_LIMIT,
            slipsLeft = SLIP_LIMIT,
            heldTurns,
            revealUntil = -1;
    public boolean carrying, hasResolved;
    public final int[] relics = RELICS.clone();

    /** Trap cell -> first active turn. */
    public final Map<Integer, Integer> traps = new LinkedHashMap<>();

    public final Set<Integer> knownTraps = new LinkedHashSet<>();
    public final List<Clue> clues = new ArrayList<>();
    public String intelligence = "";

    public GameState() {}

    public GameState(int exit) {
        if (!isExit(exit)) throw new IllegalArgumentException("Invalid exit");
    }

    public String validate(boolean police, PlayerAction action) {
        return GameRules.validate(this, police, action);
    }

    public String relicText() {
        return relics[0] + "," + relics[1];
    }

    public String clueText() {
        StringBuilder b = new StringBuilder();
        for (Clue c : clues)
            if (c.until() >= turn)
                b.append(c.cell())
                        .append(',')
                        .append(c.kind())
                        .append(',')
                        .append(c.until() - turn + 1)
                        .append(';');
        return b.toString();
    }
}
