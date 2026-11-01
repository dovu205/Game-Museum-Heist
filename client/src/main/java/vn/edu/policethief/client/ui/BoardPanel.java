package vn.edu.policethief.client.ui;

import static vn.edu.policethief.client.ui.UiTheme.*;

import vn.edu.policethief.client.controller.ClientActions;
import vn.edu.policethief.client.model.ClientState;
import vn.edu.policethief.shared.model.GameState;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import javax.swing.*;

final class BoardPanel extends JPanel {
    int tile, ox, oy, hover = -1;
    private final ClientState state;
    private final ClientActions actions;

    BoardPanel(ClientState state, ClientActions actions) {
        this.state = state;
        this.actions = actions;
        setOpaque(false);
        setPreferredSize(new Dimension(650, 630));
        setToolTipText("");
        MouseAdapter mouse =
                new MouseAdapter() {
                    public void mouseMoved(MouseEvent e) {
                        hover = cellAt(e.getX(), e.getY());
                        repaint();
                    }

                    public void mouseExited(MouseEvent e) {
                        hover = -1;
                        repaint();
                    }

                    public void mousePressed(MouseEvent e) {
                        int cell = cellAt(e.getX(), e.getY());
                        if (cell < 0 || state.locked) return;
                        if (cell == state.pos) {
                            actions.chooseDirection("STAY");
                            return;
                        }
                        int origin =
                                state.kind.equals("SLIP")
                                                && state.secondDirection.equals("STAY")
                                                && !state.direction.equals("STAY")
                                        ? GameState.adjacent(state.pos, state.direction)
                                        : state.pos;
                        for (String d : GameState.DIRECTIONS)
                            if (!d.equals("STAY") && GameState.adjacent(origin, d) == cell) {
                                actions.chooseDirection(d);
                                break;
                            }
                    }
                };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    int cellAt(int px, int py) {
        if (tile <= 0 || px < ox || py < oy || px >= ox + tile * 7 || py >= oy + tile * 7)
            return -1;
        return (py - oy) / tile * 7 + (px - ox) / tile;
    }

    public String getToolTipText(MouseEvent e) {
        int cell = cellAt(e.getX(), e.getY());
        return cell < 0
                ? null
                : GameState.cellName(cell) + (GameState.walkable(cell) ? "" : " · Tường");
    }

    void geometry() {
        tile = Math.max(15, Math.min((getWidth() - 42) / 7, (getHeight() - 64) / 7));
        ox = (getWidth() - tile * 7) / 2;
        oy = (getHeight() - tile * 7) / 2 + 8;
    }

    double cx(int c) {
        return ox + (GameState.x(c) + .5) * tile;
    }

    double cy(int c) {
        return oy + (GameState.y(c) + .5) * tile;
    }

    protected void paintComponent(Graphics raw) {
        super.paintComponent(raw);
        Graphics2D g = (Graphics2D) raw.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        geometry();
        int w = getWidth(), h = getHeight();
        g.setPaint(new GradientPaint(0, 0, new Color(15, 27, 41), w, h, new Color(11, 20, 31)));
        g.fillRoundRect(0, 0, w, h, 18, 18);
        g.setFont(BODY.deriveFont(Font.BOLD, 10f));
        g.setColor(MUTED);
        g.drawString("MUSÉE / PLAN 07", 16, 20);
        String privacy =
                state.enemyPos < 0
                        ? "VỊ TRÍ ĐỐI THỦ: BÍ MẬT"
                        : state.ending.isEmpty() || state.ending.equals("NONE")
                                ? "ĐỐI THỦ ĐANG LỘ DIỆN"
                                : "VÁN ĐÃ KẾT THÚC";
        g.setColor(state.enemyPos < 0 ? MINT : GOLD);
        g.drawString(privacy, Math.max(160, w - g.getFontMetrics().stringWidth(privacy) - 17), 20);
        for (int y = 0; y < 7; y++)
            for (int x = 0; x < 7; x++) {
                int c = y * 7 + x, px = ox + x * tile, py = oy + y * tile;
                if (!GameState.walkable(c)) {
                    drawWall(g, px, py);
                } else {
                    g.setColor((x + y) % 2 == 0 ? new Color(25, 40, 52) : new Color(29, 44, 56));
                    g.fillRect(px + 1, py + 1, tile - 2, tile - 2);
                    g.setColor(new Color(49, 65, 77, 100));
                    g.drawRect(px + 6, py + 6, tile - 13, tile - 13);
                    if (c % 9 == 0) {
                        g.setColor(new Color(91, 110, 112, 30));
                        g.fillOval(px + tile / 4, py + tile / 4, tile / 2, tile / 2);
                    }
                }
            }
        g.setFont(BODY.deriveFont(11f));
        g.setColor(MUTED);
        for (int i = 0; i < 7; i++) {
            center(g, String.valueOf((char) ('A' + i)), ox + i * tile + tile / 2, oy - 7);
            center(g, String.valueOf(i + 1), ox - 12, oy + i * tile + tile / 2 + 4);
        }
        for (int i = 0; i < state.relics.length; i++) {
            int c = state.relics[i];
            boolean visible = state.role.equals("THIEF") && (state.relicMask & (1 << i)) != 0;
            int x = (int) cx(c), y = (int) cy(c);
            g.setColor(new Color(9, 19, 28));
            g.fillRoundRect(x - 17, y - 13, 34, 32, 6, 6);
            g.setColor(new Color(78, 112, 130));
            g.drawRoundRect(x - 17, y - 15, 34, 30, 6, 6);
            if (visible) {
                g.setColor(new Color(255, 202, 103, 23));
                g.fillOval(x - 24, y - 24, 48, 48);
                g.setColor(GOLD);
                g.fillPolygon(
                        new int[] {x, x + 11, x, x - 11}, new int[] {y - 13, y, y + 12, y}, 4);
                g.setColor(new Color(255, 236, 185));
                g.drawLine(x, y - 10, x - 6, y);
            } else {
                g.setColor(new Color(75, 107, 125));
                g.drawOval(x - 6, y - 6, 12, 12);
            }
        }
        for (int exit : state.exits) {
            int x = (int) cx(exit), y = (int) cy(exit);
            g.setColor(new Color(89, 225, 184, 35));
            g.fillRoundRect(x - tile / 2 + 3, y - tile / 2 + 3, tile - 6, tile - 6, 7, 7);
            g.setColor(MINT);
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(x - 12, y - 17, 24, 32, 3, 3);
            g.drawLine(x + 6, y - 3, x + 10, y - 3);
            g.setFont(BODY.deriveFont(Font.BOLD, 8f));
            center(g, "THOÁT", x, y + tile / 2 - 6);
        }
        if (state.role.equals("POLICE")) {
            for (GameState.Clue c : state.clues) {
                int x = (int) cx(c.cell()), y = (int) cy(c.cell());
                g.setColor(c.kind().equals("NOISE") ? GOLD : MUTED);
                if (c.kind().equals("NOISE")) {
                    double pulse = 8 + 4 * Math.sin(System.currentTimeMillis() / 180.0);
                    g.setStroke(new BasicStroke(2));
                    g.draw(new Ellipse2D.Double(x - pulse, y - pulse, pulse * 2, pulse * 2));
                    g.drawOval(x - 18, y - 18, 36, 36);
                    g.fillOval(x - 3, y - 3, 6, 6);
                } else {
                    g.fillOval(x - 9, y - 3, 5, 9);
                    g.fillOval(x + 3, y + 3, 5, 9);
                }
                g.setFont(BODY.deriveFont(9f));
                center(g, String.valueOf(c.until()), x, y + tile / 2 - 6);
            }
        }
        if (!state.locked
                && !state.finished
                && (state.kind.equals("MOVE")
                        || state.kind.equals("DASH")
                        || state.kind.equals("SLIP"))) {
            java.util.List<Integer> path =
                    GameState.path(
                            state.pos,
                            new vn.edu.policethief.shared.model.PlayerAction(
                                    state.kind, state.direction, state.secondDirection));
            if (!path.isEmpty()) {
                Path2D line = new Path2D.Double();
                line.moveTo(cx(state.pos), cy(state.pos));
                for (int c : path) line.lineTo(cx(c), cy(c));
                g.setColor(new Color(86, 176, 255, 185));
                g.setStroke(
                        new BasicStroke(
                                3,
                                BasicStroke.CAP_ROUND,
                                BasicStroke.JOIN_ROUND,
                                1,
                                new float[] {4, 7},
                                0));
                g.draw(line);
                int last = path.get(path.size() - 1);
                g.setStroke(new BasicStroke(2));
                g.drawOval((int) cx(last) - 13, (int) cy(last) - 13, 26, 26);
            }
        }
        if (!state.locked && state.kind.equals("TRAP")) {
            int target = GameState.adjacent(state.pos, state.direction);
            if (target != state.pos) {
                g.setColor(GOLD);
                g.setStroke(new BasicStroke(2));
                g.drawOval((int) cx(target) - 16, (int) cy(target) - 16, 32, 32);
            }
        }
        for (int trap : state.visibleTraps) {
            int tx = (int) cx(trap), ty = (int) cy(trap);
            g.setColor(RED);
            g.setStroke(new BasicStroke(2));
            g.drawOval(tx - 13, ty - 11, 26, 22);
            g.drawLine(tx - 9, ty - 8, tx + 9, ty + 8);
            g.drawLine(tx - 9, ty + 8, tx + 9, ty - 8);
        }
        if (hover >= 0 && !state.locked && GameState.walkable(hover)) {
            g.setColor(new Color(86, 176, 255, 25));
            g.fillRect(
                    ox + GameState.x(hover) * tile + 1,
                    oy + GameState.y(hover) * tile + 1,
                    tile - 2,
                    tile - 2);
        }
        Point2D animated = animatedPosition();
        if (state.enemyPos >= 0)
            actor(
                    g,
                    cx(state.enemyPos) + (state.enemyPos == state.pos ? tile * .18 : 0),
                    cy(state.enemyPos),
                    !state.role.equals("POLICE"),
                    false);
        actor(
                g,
                animated.getX() - (state.enemyPos == state.pos ? tile * .18 : 0),
                animated.getY(),
                state.role.equals("POLICE"),
                state.carrying == 1);
        g.setFont(BODY.deriveFont(Font.BOLD, 9f));
        g.setColor(INK);
        center(g, "BẠN", (int) animated.getX(), (int) animated.getY() - 24);
        if (!state.ending.equals("NONE") && !state.ending.isEmpty()) {
            String banner =
                    switch (state.ending) {
                        case "CAPTURE" -> "ĐÃ BẮT ĐƯỢC TRỘM";
                        case "ESCAPE" -> "BÁU VẬT ĐÃ RỜI BẢO TÀNG";
                        default -> "BẢO VỆ THÀNH CÔNG";
                    };
            int width = Math.min(w - 28, 390);
            g.setColor(new Color(6, 13, 22, 230));
            g.fillRoundRect((w - width) / 2, h / 2 - 26, width, 52, 14, 14);
            g.setColor(state.ending.equals("ESCAPE") ? GOLD : BLUE);
            g.setFont(BODY.deriveFont(Font.BOLD, 18f));
            center(g, banner, w / 2, h / 2 + 6);
        }
        long remaining = state.deadline - System.currentTimeMillis() - state.serverOffset;
        if (!state.locked && remaining > 0 && remaining <= 2000) {
            int alpha = (int) (80 + 50 * Math.sin(System.currentTimeMillis() / 100.0));
            g.setColor(new Color(255, 113, 133, alpha));
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(2, 2, w - 5, h - 5, 16, 16);
        }
        g.dispose();
    }

    Point2D animatedPosition() {
        if (state.animationPath.length == 0
                || System.currentTimeMillis() - state.animationStart > 650)
            return new Point2D.Double(cx(state.pos), cy(state.pos));
        double progress =
                Math.min(.999, (System.currentTimeMillis() - state.animationStart) / 650.0)
                        * state.animationPath.length;
        int step = (int) progress;
        double f = progress - step;
        f = f * f * (3 - 2 * f);
        int from = step == 0 ? state.animationFrom : state.animationPath[step - 1],
                to = state.animationPath[step];
        return new Point2D.Double(
                cx(from) + (cx(to) - cx(from)) * f, cy(from) + (cy(to) - cy(from)) * f);
    }

    private void drawWall(Graphics2D g, int px, int py) {
        int left = px + 3, top = py + 3, size = tile - 6;
        g.setColor(new Color(5, 10, 16, 180));
        g.fillRoundRect(left + 2, top + 3, size, size, 5, 5);
        g.setPaint(
                new GradientPaint(
                        left,
                        top,
                        new Color(157, 116, 88),
                        left,
                        top + size,
                        new Color(101, 70, 55)));
        g.fillRoundRect(left, top, size, size, 5, 5);
        Shape clip = g.getClip();
        g.clipRect(left + 1, top + 1, size - 2, size - 2);
        g.setStroke(new BasicStroke(Math.max(1f, tile / 45f)));
        g.setColor(new Color(54, 40, 35));
        int rowHeight = Math.max(6, size / 3);
        for (int row = 0; row < 3; row++) {
            int y = top + row * rowHeight;
            if (row > 0) g.drawLine(left, y, left + size, y);
            int joint = left + (row % 2 == 0 ? size / 3 : size * 2 / 3);
            g.drawLine(joint, y, joint, Math.min(top + size, y + rowHeight));
        }
        g.setClip(clip);
        g.setColor(new Color(218, 180, 137));
        g.drawLine(left + 3, top + 1, left + size - 4, top + 1);
        g.setColor(new Color(78, 51, 42));
        g.drawRoundRect(left, top, size, size, 5, 5);
        g.setStroke(new BasicStroke(1));
    }

    void actor(Graphics2D g, double px, double py, boolean cop, boolean hasRelic) {
        double scale = Math.min(1.2, tile / 57.0);
        Graphics2D a = (Graphics2D) g.create();
        a.translate(px, py);
        a.scale(scale, scale);
        a.setColor(new Color(0, 6, 12, 140));
        a.fillOval(-15, 6, 30, 14);
        a.setColor(new Color(15, 22, 34));
        a.fillRoundRect(-9, 3, 7, 13, 4, 4);
        a.fillRoundRect(2, 3, 7, 13, 4, 4);
        a.setColor(cop ? BLUE : new Color(218, 225, 231));
        a.fillRoundRect(-12, -9, 24, 19, 8, 8);
        if (!cop) {
            a.setColor(new Color(33, 42, 54));
            for (int i = 0; i < 3; i++) a.fillRect(-11, -6 + i * 5, 22, 2);
        }
        a.setColor(new Color(232, 186, 150));
        a.fillOval(-9, -23, 18, 18);
        if (cop) {
            a.setColor(new Color(28, 77, 137));
            a.fillRoundRect(-11, -26, 22, 9, 4, 4);
            a.fillRect(-12, -19, 24, 3);
            a.setColor(GOLD);
            a.fillOval(-3, -24, 6, 5);
            a.fillOval(3, -5, 5, 5);
        } else {
            a.setColor(new Color(22, 28, 38));
            a.fillRoundRect(-9, -25, 18, 7, 4, 4);
            a.fillRect(-9, -17, 18, 5);
            a.setColor(INK);
            a.fillRect(-5, -16, 3, 1);
            a.fillRect(3, -16, 3, 1);
            if (hasRelic) {
                a.setColor(GOLD);
                a.fillOval(9, -1, 14, 17);
                a.setColor(BG);
                a.setFont(BODY.deriveFont(Font.BOLD, 10f));
                a.drawString("◆", 11, 11);
            }
        }
        a.dispose();
    }

    void center(Graphics2D g, String text, int x, int y) {
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, y);
    }
}
