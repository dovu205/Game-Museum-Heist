package vn.edu.policethief.client.ui;

import vn.edu.policethief.client.controller.GameController;
import vn.edu.policethief.shared.model.GameState;

import java.awt.*;
import java.nio.file.*;

import javax.swing.*;

public final class UiTest {
    static int checks;

    static void check(boolean b, String m) {
        if (!b) throw new AssertionError(m);
        checks++;
    }

    static void inspect(Component c) {
        if (c instanceof JButton b)
            check(
                    !b.getText().toLowerCase().contains("server"),
                    "no server controls inside player UI");
        if (c instanceof Container parent)
            for (Component child : parent.getComponents()) inspect(child);
    }

    static String[] state(boolean cop, String id, int round, int turn, int pos) {
        long now = System.currentTimeMillis();
        return new String[] {
            "STATE",
            id,
            String.valueOf(round),
            String.valueOf(turn),
            cop ? "POLICE" : "THIEF",
            "Opponent",
            "0",
            "0",
            String.valueOf(pos),
            cop ? "-1" : "42",
            cop ? "-1" : "3",
            cop ? "-1" : "0",
            cop ? "2" : "-1",
            cop ? "-1" : "1",
            cop ? "29,NOISE,2;" : "",
            String.valueOf(now + 5000),
            String.valueOf(now),
            "Ready",
            "2,28"
        };
    }

    static void checkMovementAfterSkill(GameController c, boolean police) {
        MainFrame w = c.window();
        c.receive(state(police, "keys", 1, 1, police ? 0 : 48));
        c.chooseMode(police ? "TRAP" : "DASH");
        c.chooseDirection(police ? "RIGHT" : "LEFT");
        w.game.send.doClick();
        check(c.state().locked, "skill submission locks current turn");
        c.receive(state(police, "keys", 1, 2, police ? 0 : 46));
        check(
                !c.state().locked && c.state().kind.equals("MOVE"),
                "next turn restores movement after skill");
        int[] codes = {
            java.awt.event.KeyEvent.VK_UP,
            java.awt.event.KeyEvent.VK_DOWN,
            java.awt.event.KeyEvent.VK_LEFT,
            java.awt.event.KeyEvent.VK_RIGHT
        };
        String[] directions = {"UP", "DOWN", "LEFT", "RIGHT"};
        for (int i = 0; i < codes.length; i++) {
            c.chooseDirection("STAY");
            boolean handled =
                    SwingUtilities.processKeyBindings(
                            new java.awt.event.KeyEvent(
                                    w.game.log,
                                    java.awt.event.KeyEvent.KEY_PRESSED,
                                    System.currentTimeMillis(),
                                    0,
                                    codes[i],
                                    java.awt.event.KeyEvent.CHAR_UNDEFINED));
            check(
                    handled && c.state().direction.equals(directions[i]),
                    "arrows move after " + (police ? "scan" : "dash") + " when log has focus");
        }
    }

    static void checkVersionTwoControls(GameController c) {
        MainFrame w = c.window();
        String[] message = java.util.Arrays.copyOf(state(false, "v2", 1, 4, 42), 24);
        message[13] = "3";
        message[19] = "-1";
        message[20] = "";
        message[21] = "0";
        message[22] = "";
        message[23] = "42,6";
        c.receive(message);
        c.chooseMode("SLIP");
        c.chooseDirection("RIGHT");
        c.chooseDirection("UP");
        check(
                c.state().kind.equals("SLIP")
                        && c.state().direction.equals("RIGHT")
                        && c.state().secondDirection.equals("UP"),
                "slip selects two successive perpendicular directions");
        check(
                GameState.path(
                                c.state().pos,
                                new vn.edu.policethief.shared.model.PlayerAction(
                                        c.state().kind,
                                        c.state().direction,
                                        c.state().secondDirection))
                        .equals(java.util.List.of(43, 36)),
                "slip preview turns at first step");
        check(
                java.util.Arrays.equals(c.state().exits, new int[] {42, 6}),
                "client parses two exits");
        boolean handled =
                SwingUtilities.processKeyBindings(
                        new java.awt.event.KeyEvent(
                                w.game.log,
                                java.awt.event.KeyEvent.KEY_PRESSED,
                                System.currentTimeMillis(),
                                0,
                                java.awt.event.KeyEvent.VK_ENTER,
                                java.awt.event.KeyEvent.CHAR_UNDEFINED));
        check(handled && c.state().locked, "Enter submits skill when log has focus");
        c.receive(
                new String[] {
                    "UPDATE",
                    "v2",
                    "1",
                    "4",
                    "43",
                    "43",
                    "",
                    "3",
                    "0",
                    "-1",
                    "2",
                    "Bạn mắc bẫy",
                    "",
                    "NONE",
                    "-1",
                    "",
                    "42",
                    "0",
                    "43",
                    "1",
                    "42,6"
                });
        check(
                c.state().heldTurns == 1
                        && c.state().visibleTraps[0] == 43
                        && c.state().slipsLeft == 2,
                "client parses trap and resource fields in UPDATE");
        message[3] = "5";
        message[8] = "43";
        message[13] = "2";
        message[20] = "43";
        message[21] = "1";
        c.receive(message);
        check(
                !w.game.modeButtons.get("DASH").isEnabled()
                        && !w.game.modeButtons.get("SLIP").isEnabled(),
                "held thief has no movement skills");
        c.chooseDirection("RIGHT");
        check(c.state().direction.equals("STAY"), "held thief cannot select arrow movement");
        w.game.send.doClick();
        check(c.state().locked, "held thief can submit stay");
        message[3] = "6";
        message[21] = "0";
        c.receive(message);
        c.chooseDirection("LEFT");
        check(
                c.state().direction.equals("LEFT") && w.game.modeButtons.get("DASH").isEnabled(),
                "movement restored after one held turn");
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(
                () -> {
                    for (boolean hosting : new boolean[] {true, false}) {
                        GameController c = new GameController(hosting);
                        MainFrame w = c.window();
                        try {
                            check(
                                    c.state().view.equals("LOGIN")
                                            && !w.shell.sidePanel.isVisible()
                                            && !w.shell.footerPanel.isVisible(),
                                    "login is separate from game shell");
                            check(
                                    !SwingUtilities.isDescendingFrom(w.auth.confirm, w.root),
                                    "login does not include password confirmation");
                            check(
                                    SwingUtilities.isDescendingFrom(w.auth.login, w.root)
                                            && !SwingUtilities.isDescendingFrom(
                                                    w.auth.register, w.root),
                                    "login has only its primary action");
                            check(
                                    !SwingUtilities.isDescendingFrom(w.auth.host, w.root)
                                            && !SwingUtilities.isDescendingFrom(
                                                    w.auth.port, w.root),
                                    "network configuration outside authentication form");
                            c.showAuth(true);
                            check(
                                    c.state().view.equals("REGISTER")
                                            && SwingUtilities.isDescendingFrom(
                                                    w.auth.confirm, w.root),
                                    "signup has confirmation");
                            check(
                                    SwingUtilities.isDescendingFrom(w.auth.register, w.root)
                                            && !SwingUtilities.isDescendingFrom(
                                                    w.auth.login, w.root),
                                    "signup has only its primary action");
                            w.auth.name.setText("alice");
                            w.auth.password.setText("secret123");
                            w.auth.confirm.setText("different");
                            w.auth.register.doClick();
                            check(
                                    w.auth.authStatus.getText().contains("không trùng"),
                                    "signup validation shown inline");
                            c.receive(new String[] {"REGISTERED", "OK"});
                            check(
                                    c.state().view.equals("LOGIN")
                                            && w.auth.name.getText().equals("alice"),
                                    "registration returns to login with name preserved");
                            check(
                                    w.auth.password.getPassword().length == 0,
                                    "password cleared on form switch");
                            c.showLobby();
                            check(
                                    w.shell.sidePanel.isVisible() && w.lobby.online != null,
                                    "lobby appears only after authentication");
                            inspect(w.root);
                            checkMovementAfterSkill(c, true);
                            checkMovementAfterSkill(c, false);
                            checkVersionTwoControls(c);
                            c.receive(state(false, "ui", 1, 1, 48));
                            check(
                                    c.state().view.equals("GAME") && c.state().enemyPos == -1,
                                    "opponent hidden during play");
                            check(
                                    java.util.Arrays.equals(c.state().relics, new int[] {2, 28}),
                                    "randomized pedestal coordinates reach the canvas model");
                            check(
                                    w.game.modeButtons.get("DASH").isVisible()
                                            && !w.game.modeButtons.get("TRAP").isVisible(),
                                    "thief gets thief skills");
                            c.chooseMode("DASH");
                            c.chooseDirection("LEFT");
                            check(
                                    c.state().kind.equals("DASH")
                                            && c.state().direction.equals("LEFT"),
                                    "dash selection keeps two-step mode");
                            c.chooseDirection("STAY");
                            check(
                                    c.state().kind.equals("MOVE")
                                            && c.state().direction.equals("STAY"),
                                    "stay cancels directional skill");
                            c.receive(state(true, "ui", 2, 1, 0));
                            check(
                                    w.game.modeButtons.get("TRAP").isVisible()
                                            && w.game.modeButtons.get("DASH").isVisible(),
                                    "police gets scan instead of dash");
                            c.chooseMode("TRAP");
                            check(c.state().kind.equals("TRAP"), "trap replaces movement action");
                            c.receive(new String[] {"LOCKED"});
                            check(
                                    c.state().locked && !w.game.send.isEnabled(),
                                    "locked input cannot be resubmitted");
                            c.receive(new String[] {"ACTION_REJECTED", "Wall"});
                            check(
                                    !c.state().locked && w.game.send.isEnabled(),
                                    "rejected action allows retry before deadline");
                            c.receive(
                                    new String[] {
                                        "UPDATE",
                                        "ui",
                                        "2",
                                        "1",
                                        "1",
                                        "1",
                                        "29,NOISE,2;",
                                        "-1",
                                        "-1",
                                        "1",
                                        "-1",
                                        "Scan says no",
                                        "NO",
                                        "NONE",
                                        "-1",
                                        "",
                                        "-1",
                                        "0"
                                    });
                            check(
                                    c.state().enemyPos == -1
                                            && c.state().exitCell == 42
                                            && c.state().carrying == -1,
                                    "fixed exit is visible while enemy and carrying remain"
                                        + " private");
                            check(c.state().clues.size() == 1, "private clues displayed");
                            c.receive(
                                    new String[] {
                                        "ROUND_END",
                                        "WIN",
                                        "2",
                                        "1",
                                        "0",
                                        "Captured",
                                        "1",
                                        "1",
                                        "42"
                                    });
                            check(
                                    c.state().enemyPos == 1 && c.state().exitCell == 42,
                                    "round result can reveal final positions");
                            c.receive(new String[] {"FINISHED", "WIN", "1", "0", "Complete"});
                            check(
                                    c.state().finished
                                            && w.game.again.isVisible()
                                            && w.game.again.isEnabled(),
                                    "replay available after full match only");
                            c.receive(state(false, "new", 1, 1, 48));
                            check(
                                    c.state().enemyPos == -1
                                            && c.state().ending.isEmpty()
                                            && !w.game.again.isVisible(),
                                    "rematch removes revealed position and results");
                            c.state().username = "alice";
                            c.receive(
                                    new String[] {
                                        "HISTORY",
                                        "id|alice|bob|1:0|1|alice|2026-10-26|Win\n",
                                        "0",
                                        "13",
                                        "125"
                                    });
                            DataView history = w.dataViews.get("HISTORY");
                            check(
                                    history != null && history.rows.getRowCount() == 1,
                                    "history page displayed");
                            check(
                                    !history.previous.isEnabled() && history.next.isEnabled(),
                                    "first page navigation");
                            c.receive(
                                    new String[] {
                                        "HISTORY",
                                        "last|alice|bob|1:1|2|Hòa|2026-10-26|Draw\n",
                                        "12",
                                        "13",
                                        "125"
                                    });
                            check(
                                    w.dataViews.get("HISTORY") == history && history.page == 12,
                                    "page response reuses history window");
                            check(
                                    history.previous.isEnabled()
                                            && !history.next.isEnabled()
                                            && history.pageLabel.getText().contains("13 / 13"),
                                    "last page navigation and page count");
                            c.receive(
                                    new String[] {
                                        "RANKING", "1|alice|10|1|0|1|100%\n", "0", "1", "1"
                                    });
                            check(
                                    w.dataViews.size() == 2
                                            && !w.dataViews.get("RANKING").next.isEnabled(),
                                    "ranking and history keep separate windows");
                            c.showAuth(false);
                            check(
                                    !w.shell.sidePanel.isVisible()
                                            && !SwingUtilities.isDescendingFrom(
                                                    w.auth.confirm, w.root),
                                    "logout returns to clean login");
                            check(
                                    w.dataViews.isEmpty() && !history.isDisplayable(),
                                    "logout closes account data windows");
                        } finally {
                            c.close();
                        }
                    }
                });
        System.out.println("PASS: " + checks + " UI assertions.");
    }
}
