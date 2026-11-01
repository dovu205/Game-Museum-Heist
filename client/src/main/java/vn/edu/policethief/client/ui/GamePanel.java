package vn.edu.policethief.client.ui;

import static vn.edu.policethief.client.ui.UiTheme.*;

import vn.edu.policethief.client.controller.ClientActions;
import vn.edu.policethief.client.model.ClientState;
import vn.edu.policethief.shared.model.GameState;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/** Swing presentation for GamePanel; client actions are delegated to the controller. */
final class GamePanel {
    private final MainFrame window;
    private final ClientState state;
    private final ClientActions actions;
    JButton send, again, exit;
    JLabel roleLabel, roundLabel, turnLabel, countdown, scoreLabel, selectionLabel, mission;
    final JTextArea log = new JTextArea();
    JProgressBar timebar;
    BoardPanel canvas;
    final java.util.List<JButton> actionButtons = new java.util.ArrayList<>();
    final java.util.Map<String, JButton> modeButtons = new java.util.LinkedHashMap<>();

    GamePanel(MainFrame window, ClientState state, ClientActions actions) {
        this.window = window;
        this.state = state;
        this.actions = actions;
    }

    void showGame() {
        window.setShell(true);
        window.clear(
                "TRUY BẮT TRONG BẢO TÀNG",
                "Hành động bí mật • Cập nhật đồng thời • Mỗi nhịp 8 giây");
        JPanel body = new JPanel(new BorderLayout(16, 12));
        body.setOpaque(false);
        JPanel stats = box();
        stats.setBorder(new EmptyBorder(13, 18, 13, 18));
        stats.setLayout(new GridLayout(1, 5, 12, 0));
        roleLabel = label("", 16, BLUE);
        roundLabel = label("", 16, INK);
        turnLabel = label("", 16, INK);
        scoreLabel = label("", 16, MINT);
        countdown = label("", 21, GOLD);
        for (JLabel l : new JLabel[] {roleLabel, roundLabel, turnLabel, scoreLabel, countdown})
            stats.add(l);
        body.add(stats, BorderLayout.NORTH);
        JPanel mapArea = new JPanel(new BorderLayout(0, 10));
        mapArea.setOpaque(false);
        canvas = new BoardPanel(state, actions);
        mapArea.add(canvas);
        JLabel legend =
                label(
                        "Gạch nâu: tường   ·   Vàng: báu vật / tiếng động   ·   Cửa xanh: A7 / G1",
                        12,
                        MUTED);
        mapArea.add(legend, BorderLayout.SOUTH);
        body.add(mapArea);
        JPanel tools = new ActionPanel();
        tools.setBorder(new EmptyBorder(17, 17, 16, 17));
        tools.setLayout(new BoxLayout(tools, BoxLayout.Y_AXIS));
        mission = label("", 13, INK);
        mission.setPreferredSize(new Dimension(240, 50));
        mission.setMinimumSize(new Dimension(240, 50));
        mission.setMaximumSize(new Dimension(240, 50));
        mission.setVerticalAlignment(SwingConstants.TOP);
        tools.add(mission);
        tools.add(Box.createVerticalStrut(12));
        tools.add(label("HÀNH ĐỘNG", 11, GOLD));
        tools.add(Box.createVerticalStrut(8));
        modeButtons.clear();
        actionButtons.clear();
        for (String mode : new String[] {"MOVE", "DASH", "SLIP", "TRAP"}) {
            String text =
                    switch (mode) {
                        case "MOVE" -> "Di chuyển";
                        case "DASH" -> "Chạy nhanh · 2 ô";
                        case "SLIP" -> "Luồn lách · 2 hướng";
                        default -> "Đặt bẫy";
                    };
            JButton b = button(text, SOFT, () -> actions.chooseMode(mode));
            b.setMaximumSize(new Dimension(1000, 31));
            b.setBorder(new EmptyBorder(6, 11, 6, 11));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            modeButtons.put(mode, b);
            tools.add(b);
            tools.add(Box.createVerticalStrut(5));
            actionButtons.add(b);
        }
        tools.add(Box.createVerticalStrut(8));
        JPanel pad = new JPanel(new GridLayout(3, 3, 5, 5));
        pad.setOpaque(false);
        pad.setMaximumSize(new Dimension(240, 96));
        pad.setPreferredSize(new Dimension(240, 96));
        pad.setAlignmentX(Component.LEFT_ALIGNMENT);
        String[] cells = {"", "↑", "", "←", "•", "→", "", "↓", ""};
        String[] dirs = {"", "UP", "", "LEFT", "STAY", "RIGHT", "", "DOWN", ""};
        for (int i = 0; i < 9; i++) {
            if (cells[i].isEmpty()) {
                JPanel gap = new JPanel();
                gap.setOpaque(false);
                pad.add(gap);
            } else {
                String d = dirs[i];
                JButton b = button(cells[i], SOFT, () -> actions.chooseDirection(d));
                b.putClientProperty("direction", d);
                b.setBorder(new EmptyBorder(3, 6, 3, 6));
                pad.add(b);
                actionButtons.add(b);
            }
        }
        tools.add(pad);
        tools.add(Box.createVerticalStrut(10));
        selectionLabel = label("", 12, MINT);
        selectionLabel.setMaximumSize(new Dimension(240, 25));
        tools.add(selectionLabel);
        tools.add(Box.createVerticalStrut(9));
        send = button("Khóa hành động →", BLUE, actions::submit);
        send.setMaximumSize(new Dimension(1000, 42));
        send.setAlignmentX(Component.LEFT_ALIGNMENT);
        tools.add(send);
        tools.add(Box.createVerticalStrut(9));
        timebar = new JProgressBar(0, GameState.TURN_MS);
        timebar.setForeground(BLUE);
        timebar.setBackground(BG);
        timebar.setBorderPainted(false);
        timebar.setMaximumSize(new Dimension(1000, 5));
        timebar.setAlignmentX(Component.LEFT_ALIGNMENT);
        tools.add(timebar);
        tools.add(Box.createVerticalStrut(15));
        tools.add(label("NHẬT KÝ CỦA BẠN", 11, MUTED));
        tools.add(Box.createVerticalStrut(6));
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setFont(BODY.deriveFont(12f));
        log.setBackground(PANEL);
        log.setForeground(MUTED);
        log.setBorder(new EmptyBorder(5, 0, 5, 0));
        JScrollPane ls = new JScrollPane(log);
        ls.setBorder(null);
        ls.getViewport().setBackground(PANEL);
        ls.setAlignmentX(Component.LEFT_ALIGNMENT);
        ls.setPreferredSize(new Dimension(240, 105));
        tools.add(ls);
        JScrollPane toolScroll =
                new JScrollPane(
                        tools,
                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        toolScroll.setPreferredSize(new Dimension(278, 0));
        toolScroll.setBorder(null);
        toolScroll.getViewport().setBackground(PANEL);
        styleScroll(toolScroll);
        toolScroll.getVerticalScrollBar().setUnitIncrement(14);
        body.add(toolScroll, BorderLayout.EAST);
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionsPanel.setOpaque(false);
        exit =
                button(
                        "Rời trận",
                        SOFT,
                        () -> {
                            if (state.finished
                                    || JOptionPane.showConfirmDialog(
                                                    window,
                                                    "Rời trận sẽ tính là đầu hàng. Tiếp tục?",
                                                    "Rời trận",
                                                    JOptionPane.YES_NO_OPTION)
                                            == JOptionPane.YES_OPTION) actions.leaveMatch();
                        });
        again =
                button(
                        "Chơi lại",
                        MINT,
                        () -> {
                            actions.replay();
                            again.setEnabled(false);
                        });
        again.setVisible(false);
        actionsPanel.add(exit);
        actionsPanel.add(again);
        body.add(actionsPanel, BorderLayout.SOUTH);
        window.content.add(body);
        bindKeys();
        window.refresh();
    }

    void updateControls() {
        boolean cop = state.role.equals("POLICE");
        modeButtons.get("DASH").setVisible(true);
        modeButtons.get("SLIP").setVisible(!cop);
        modeButtons.get("TRAP").setVisible(cop);
        for (var e : modeButtons.entrySet()) {
            String k = e.getKey();
            e.getValue().setBackground(k.equals(state.kind) ? BLUE : SOFT);
            e.getValue().setForeground(k.equals(state.kind) ? BG : INK);
            e.getValue()
                    .setEnabled(
                            !state.locked
                                    && !state.finished
                                    && (state.heldTurns == 0 || k.equals("MOVE"))
                                    && (!k.equals("TRAP") || state.trapsLeft > 0)
                                    && (!k.equals("SLIP") || state.slipsLeft > 0));
        }
        modeButtons.get("TRAP").setText("Đặt bẫy · còn " + Math.max(0, state.trapsLeft) + "/2");
        modeButtons.get("SLIP").setText("Luồn lách · còn " + Math.max(0, state.slipsLeft) + "/3");
        for (JButton b : actionButtons)
            if (!modeButtons.containsValue(b)) {
                b.setEnabled(
                        !state.locked
                                && !state.finished
                                && (state.heldTurns == 0
                                        || "STAY".equals(b.getClientProperty("direction"))));
            }
        send.setEnabled(!state.locked && !state.finished);
        send.setText(state.locked ? "Đã khóa · chờ phân xử" : "Khóa hành động →");
        String action =
                switch (state.kind) {
                    case "DASH" -> "Chạy";
                    case "SLIP" -> "Luồn lách";
                    case "TRAP" -> "Bẫy";
                    default -> "Đi";
                };
        String dir =
                switch (state.direction) {
                    case "UP" -> "lên";
                    case "RIGHT" -> "phải";
                    case "DOWN" -> "xuống";
                    case "LEFT" -> "trái";
                    default -> "đứng yên";
                };
        selectionLabel.setText(
                action
                        + " · "
                        + dir
                        + (state.kind.equals("SLIP")
                                ? " → "
                                        + (state.secondDirection.equals("STAY")
                                                ? "chọn hướng thứ 2"
                                                : switch (state.secondDirection) {
                                                    case "UP" -> "lên";
                                                    case "DOWN" -> "xuống";
                                                    case "LEFT" -> "trái";
                                                    default -> "phải";
                                                })
                                : ""));
        String text =
                cop
                        ? "Bắt trộm trong 32 nhịp.<br>Hai cửa thoát: A7 và G1."
                        : state.carrying == 1
                                ? "Đã có báu vật!<br>Đến cửa A7 hoặc G1 để thắng."
                                : "Lấy một báu vật rồi đến cửa thoát.<br>Chạy nhanh sẽ gây tiếng"
                                      + " động.";
        if (state.heldTurns > 0) text = "Bạn đang mắc bẫy.<br>Nhịp này chỉ được đứng yên.";
        mission.setText("<html><div style='width:168px'>" + text + "</div></html>");
        canvas.repaint();
        window.refresh();
    }

    void bindKeys() {
        String[] keys = {"UP", "RIGHT", "DOWN", "LEFT", "SPACE"};
        for (int i = 0; i < 5; i++) {
            String d = GameState.DIRECTIONS[i], key = keys[i];
            window.root
                    .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(key), key);
            Action action =
                    new AbstractAction() {
                        public void actionPerformed(ActionEvent e) {
                            if (state.view.equals("GAME")) actions.chooseDirection(d);
                        }
                    };
            window.root.getActionMap().put(key, action);
            bindGameKey(window.content, KeyStroke.getKeyStroke(key), key, action);
        }
        window.root
                .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ENTER"), "SUBMIT");
        Action submit =
                new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        if (state.view.equals("GAME")) actions.submit();
                    }
                };
        window.root.getActionMap().put("SUBMIT", submit);
        bindGameKey(window.content, KeyStroke.getKeyStroke("ENTER"), "SUBMIT", submit);
    }

    private void bindGameKey(Component component, KeyStroke key, String name, Action action) {
        // Focused text areas and scroll panes otherwise consume arrows before
        // the window binding, including after a locked button loses focus.
        if (component instanceof JComponent target) {
            target.getInputMap(JComponent.WHEN_FOCUSED).put(key, name);
            target.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(key, name);
            target.getActionMap().put(name, action);
        }
        if (component instanceof Container parent) {
            for (Component child : parent.getComponents()) bindGameKey(child, key, name, action);
        }
    }
}
