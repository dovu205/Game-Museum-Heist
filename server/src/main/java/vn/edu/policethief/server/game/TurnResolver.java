package vn.edu.policethief.server.game;

import vn.edu.policethief.shared.GameRules;
import vn.edu.policethief.shared.model.*;

import java.util.*;

/** Each substep checks capture before treasure/escape. Paths stop on trap or victory. */
public final class TurnResolver {
    private final TrapService traps = new TrapService();
    private final WinConditionService wins = new WinConditionService();

    public GameState.Result resolve(GameState s, PlayerAction police, PlayerAction thief) {
        ActionValidator validator = new ActionValidator();
        String error = validator.validate(s, true, police);
        if (error == null) error = validator.validate(s, false, thief);
        if (error != null) throw new IllegalArgumentException(error);
        s.clues.removeIf(c -> c.until() < s.turn);
        s.intelligence = "";
        int oldCop = s.cop, oldThief = s.thief;
        boolean noise = thief.kind().equals("DASH"),
                trace =
                        thief.kind().equals("MOVE")
                                && !thief.direction().equals("STAY")
                                && s.turn % 3 == 0;
        if (noise) s.clues.add(new GameState.Clue(s.thief, "NOISE", s.turn + 1));
        if (trace) s.clues.add(new GameState.Clue(s.thief, "TRACE", s.turn + 2));
        traps.place(s, police);
        if (thief.kind().equals("SLIP")) s.slipsLeft--;
        if (s.heldTurns > 0) s.heldTurns--;
        List<Integer> cp = GameRules.path(s.cop, police),
                tp = GameRules.path(s.thief, thief),
                actualCop = new ArrayList<>(),
                actualThief = new ArrayList<>();
        boolean caught = false,
                escaped = false,
                trapped = false,
                collected = false,
                stopped = false;
        for (int step = 0; step < Math.max(1, Math.max(cp.size(), tp.size())); step++) {
            int prevCop = s.cop, prevThief = s.thief;
            if (step < cp.size()) {
                s.cop = cp.get(step);
                actualCop.add(s.cop);
            }
            if (!stopped && step < tp.size()) {
                s.thief = tp.get(step);
                actualThief.add(s.thief);
            }
            caught = wins.caught(s, prevCop, prevThief);
            if (caught) break;
            if (traps.enter(s, prevThief)) {
                trapped = true;
                stopped = true;
            }
            if (prevThief != s.thief)
                for (int i = 0; i < s.relics.length; i++)
                    if (s.thief == s.relics[i] && (s.relicMask & (1 << i)) != 0) {
                        s.carrying = true;
                        s.relicMask &= ~(1 << i);
                        collected = true;
                    }
            escaped = wins.escaped(s);
            if (escaped) break;
        }
        s.hasResolved = true;
        String ending = wins.ending(s, caught, escaped);
        if (ending.equals("NONE")) s.intelligence = new VisibilityService().intelligence(s);
        return new GameState.Result(
                ending,
                collected,
                noise,
                trace,
                trapped,
                oldCop,
                oldThief,
                List.copyOf(actualCop),
                List.copyOf(actualThief));
    }
}
