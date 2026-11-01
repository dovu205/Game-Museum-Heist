package vn.edu.policethief.server.game;

import vn.edu.policethief.shared.GameRules;
import vn.edu.policethief.shared.model.*;
import vn.edu.policethief.shared.protocol.Packet;

import java.util.*;

public final class EngineTest {
    static int checks;

    static void check(boolean b, String message) {
        if (!b) throw new AssertionError(message);
        checks++;
    }

    static PlayerAction move(String d) {
        return new PlayerAction("MOVE", d);
    }

    static PlayerAction dash(String d) {
        return new PlayerAction("DASH", d);
    }

    static PlayerAction slip(String a, String b) {
        return new PlayerAction("SLIP", a, b);
    }

    static GameSession board(int c, int t) {
        GameSession s = new GameSession();
        s.cop = c;
        s.thief = t;
        return s;
    }

    public static void main(String[] args) {
        check(GameRules.MAX_TURNS == 32 && GameRules.TURN_MS == 8000, "32 turns, 8 seconds");
        check(Arrays.equals(GameRules.EXITS, new int[] {42, 6}), "A7 and G1 exits");
        Random random = new Random(17);
        MapGenerator maps = new MapGenerator();
        for (int i = 0; i < 1000; i++) {
            GameSession s = maps.generate(random);
            Set<Integer> cells = new HashSet<>(List.of(s.cop, s.thief, s.relics[0], s.relics[1]));
            check(cells.size() == 4, "distinct spawns and treasure");
            for (int cell : cells)
                check(
                        GameRules.walkable(cell) && !GameRules.isExit(cell),
                        "valid connected non-exit cells");
        }
        GameSession s = board(0, 48);
        check(s.validate(true, dash("RIGHT")) == null, "police can dash");
        GameState.Result r = s.resolve(dash("RIGHT"), PlayerAction.stay());
        check(s.cop == 2 && r.copPath().equals(List.of(1, 2)) && !r.noise(), "police dash silent");
        s = board(48, 3);
        s.resolve(PlayerAction.stay(), dash("DOWN"));
        check(s.thief == 10, "dash stops before wall");
        s = board(0, 42);
        s.resolve(PlayerAction.stay(), slip("RIGHT", "UP"));
        check(s.thief == 36 && s.slipsLeft == 2 && s.clues.isEmpty(), "perpendicular slip silent");
        s = board(48, 0);
        s.resolve(PlayerAction.stay(), slip("RIGHT", "DOWN"));
        check(s.thief == 1, "blocked second slip step stops after first");
        check(s.validate(false, slip("DOWN", "RIGHT")) != null, "blocked first step rejected");
        check(s.validate(false, slip("RIGHT", "LEFT")) != null, "reverse slip rejected");
        check(s.validate(false, slip("RIGHT", "RIGHT")) != null, "straight slip rejected");
        check(s.validate(true, slip("RIGHT", "UP")) != null, "police cannot slip");
        s.slipsLeft = 0;
        check(s.validate(false, slip("RIGHT", "UP")) != null, "slip exhausted");
        s = board(0, 1);
        check(
                s.resolve(move("RIGHT"), PlayerAction.stay()).ending().equals("CAPTURE"),
                "same-cell capture");
        s = board(0, 1);
        check(s.resolve(move("RIGHT"), move("LEFT")).ending().equals("CAPTURE"), "swap capture");
        s = board(1, 3);
        check(
                s.resolve(PlayerAction.stay(), dash("LEFT")).ending().equals("CAPTURE"),
                "dash cannot pass officer");
        s = board(36, 42);
        r = s.resolve(PlayerAction.stay(), slip("RIGHT", "UP"));
        check(r.ending().equals("CAPTURE") && s.thief == 36, "capture during second slip step");
        s = board(0, 48);
        s.relics[0] = 47;
        r = s.resolve(PlayerAction.stay(), dash("LEFT"));
        check(r.collected() && s.carrying && s.thief == 46, "collect passed treasure");
        s = board(48, 5);
        s.carrying = true;
        r = s.resolve(PlayerAction.stay(), dash("RIGHT"));
        check(
                r.ending().equals("ESCAPE") && r.thiefPath().equals(List.of(6)),
                "escape G1 halts dash");
        s = board(0, 43);
        s.carrying = true;
        check(s.resolve(PlayerAction.stay(), move("LEFT")).ending().equals("ESCAPE"), "escape A7");
        s = board(0, 43);
        check(
                s.resolve(PlayerAction.stay(), move("LEFT")).ending().equals("NONE"),
                "exit without treasure stays in play");
        s = board(6, 5);
        s.carrying = true;
        check(
                s.resolve(PlayerAction.stay(), move("RIGHT")).ending().equals("CAPTURE"),
                "capture overrides escape");
        s = board(0, 48);
        s.turn = 31;
        check(
                s.resolve(PlayerAction.stay(), PlayerAction.stay()).ending().equals("NONE"),
                "turn31 continues");
        s.turn = 32;
        check(
                s.resolve(PlayerAction.stay(), PlayerAction.stay()).ending().equals("TIMEOUT"),
                "turn32 timeout");
        s = board(48, 5);
        s.turn = 32;
        s.carrying = true;
        check(
                s.resolve(PlayerAction.stay(), move("RIGHT")).ending().equals("ESCAPE"),
                "escape on last turn wins");
        for (int exit : GameRules.EXITS)
            for (int cell = 0; cell < 49; cell++)
                if (Math.max(
                                Math.abs(GameRules.x(exit) - GameRules.x(cell)),
                                Math.abs(GameRules.y(exit) - GameRules.y(cell)))
                        <= 1)
                    check(
                            !GameRules.trapAllowed(cell),
                            "no traps in exit neighborhood including diagonals");
        s = board(0, 48);
        check(s.validate(true, new PlayerAction("TRAP", "RIGHT")) == null, "legal adjacent trap");
        check(s.validate(false, new PlayerAction("TRAP", "LEFT")) != null, "thief cannot trap");
        s.trapsLeft = 0;
        check(s.validate(true, new PlayerAction("TRAP", "RIGHT")) != null, "trap exhausted");
        s = board(0, 2);
        r = s.resolve(new PlayerAction("TRAP", "RIGHT"), move("LEFT"));
        check(!r.trapped() && s.thief == 1 && s.heldTurns == 0, "new trap inactive this turn");
        check(s.trapsLeft == 1 && s.traps.get(1) == 2, "trap becomes active next turn");
        s.turn++;
        s.resolve(PlayerAction.stay(), move("RIGHT"));
        s.turn++;
        r = s.resolve(PlayerAction.stay(), dash("LEFT"));
        check(
                r.trapped() && s.thief == 1 && r.thiefPath().equals(List.of(1)) && s.heldTurns == 1,
                "active trap halts dash");
        check(
                s.validate(false, move("RIGHT")) != null
                        && s.validate(false, dash("RIGHT")) != null
                        && s.validate(false, slip("RIGHT", "DOWN")) != null,
                "held thief cannot move");
        check(new TrapService().visible(s, false).equals("1"), "activated trap known to thief");
        s.turn++;
        r = s.resolve(PlayerAction.stay(), PlayerAction.stay());
        check(
                !r.trapped() && s.heldTurns == 0 && s.traps.containsKey(1),
                "stay on trap does not retrigger; persistent trap");
        s.turn++;
        s.resolve(PlayerAction.stay(), move("RIGHT"));
        s.turn++;
        r = s.resolve(PlayerAction.stay(), move("LEFT"));
        check(r.trapped(), "reentry retriggers trap");
        s = board(0, 3);
        s.traps.put(2, 1);
        r = s.resolve(dash("RIGHT"), move("LEFT"));
        check(r.ending().equals("CAPTURE"), "police catches thief on trap");
        s = board(0, 42);
        s.traps.put(36, 1);
        r = s.resolve(PlayerAction.stay(), slip("RIGHT", "UP"));
        check(r.trapped() && s.thief == 36, "slip obeys traps");
        VisibilityService visibility = new VisibilityService();
        check(
                visibility.lineOfSight(0, 2) && visibility.lineOfSight(0, 14),
                "unblocked line within 2");
        check(
                !visibility.lineOfSight(0, 3)
                        && !visibility.lineOfSight(0, 8)
                        && !visibility.lineOfSight(7, 9),
                "range, diagonal, walls hide opponent");
        s = board(0, 48);
        check(
                visibility.opponent(s, true, false) == -1
                        && visibility.opponent(s, false, false) == -1,
                "distant positions private");
        s.revealUntil = 2;
        check(
                visibility.opponent(s, true, false) == 48
                        && visibility.opponent(s, false, false) == -1,
                "trap reveals thief only");
        s.turn = 3;
        check(visibility.opponent(s, true, false) == -1, "trap reveal expires");
        check(visibility.opponent(s, true, true) == 48, "round end reveals all");
        s = board(0, 48);
        s.turn = 8;
        s.resolve(PlayerAction.stay(), PlayerAction.stay());
        check(
                s.intelligence.contains("Nam") && !s.intelligence.contains("G7"),
                "turn8 regional intelligence");
        s.turn = 16;
        s.thief = 14;
        s.resolve(PlayerAction.stay(), PlayerAction.stay());
        check(s.intelligence.contains("Trung tâm"), "turn16 intelligence");
        s.turn = 24;
        s.thief = 6;
        s.resolve(PlayerAction.stay(), PlayerAction.stay());
        check(s.intelligence.contains("Bắc"), "turn24 intelligence");
        s.turn = 25;
        s.resolve(PlayerAction.stay(), PlayerAction.stay());
        check(s.intelligence.isEmpty(), "no continuous intelligence");
        s = board(0, 48);
        s.resolve(PlayerAction.stay(), dash("LEFT"));
        check(s.clueText().contains("48,NOISE,2"), "noise duration starts at2");
        s.turn++;
        check(s.clueText().contains("NOISE,1"), "noise second turn");
        s.turn++;
        check(s.clueText().isEmpty(), "noise expires");
        s = board(0, 48);
        s.turn = 3;
        s.resolve(PlayerAction.stay(), move("LEFT"));
        check(s.clueText().contains("48,TRACE,3"), "walking every third turn leaves trace");
        s.turn += 3;
        check(s.clueText().isEmpty(), "trace expires");
        String wire = Packet.encode("STATE", new Object[] {"Tiếng Việt", "", "x\ny"});
        check(
                Arrays.equals(
                        Packet.decode(wire), new String[] {"STATE", "Tiếng Việt", "", "x\ny"}),
                "UTF8 protocol roundtrip");
        for (int i = 0; i < 1000; i++) {
            s = maps.generate(random);
            for (int turn = 1; turn <= 32; turn++) {
                s.turn = turn;
                List<PlayerAction> cop = new ArrayList<>(), thief = new ArrayList<>();
                for (String d : GameRules.DIRECTIONS)
                    for (String k : List.of("MOVE", "DASH", "TRAP")) {
                        PlayerAction a = new PlayerAction(k, d);
                        if (s.validate(true, a) == null) cop.add(a);
                    }
                for (String d : GameRules.DIRECTIONS) {
                    for (String k : List.of("MOVE", "DASH")) {
                        PlayerAction a = new PlayerAction(k, d);
                        if (s.validate(false, a) == null) thief.add(a);
                    }
                    for (String d2 : GameRules.DIRECTIONS) {
                        PlayerAction a = slip(d, d2);
                        if (s.validate(false, a) == null) thief.add(a);
                    }
                }
                r =
                        s.resolve(
                                cop.get(random.nextInt(cop.size())),
                                thief.get(random.nextInt(thief.size())));
                check(
                        GameRules.walkable(s.cop)
                                && GameRules.walkable(s.thief)
                                && s.trapsLeft >= 0
                                && s.slipsLeft >= 0,
                        "random match invariants");
                if (!r.ending().equals("NONE")) break;
            }
        }
        System.out.println("PASS: " + checks + " new-rule engine assertions.");
    }
}
