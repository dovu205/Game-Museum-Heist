package vn.edu.policethief.client.ui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/** Shared Swing colors, fonts and component factories. */
final class UiTheme {
    private UiTheme() {}

    static void fill(DefaultTableModel model, java.util.List<java.util.List<String>> data) {
        model.setRowCount(0);
        for (var row : data) model.addRow(row.toArray());
    }

    static final Color BG = new Color(9, 15, 25),
            PANEL = new Color(18, 28, 44),
            SOFT = new Color(33, 48, 66),
            INK = new Color(234, 242, 251),
            MUTED = new Color(145, 166, 186),
            BLUE = new Color(86, 176, 255),
            MINT = new Color(89, 225, 184),
            GOLD = new Color(255, 200, 109),
            RED = new Color(255, 113, 133);
    static final Font BODY = new Font("Segoe UI", Font.PLAIN, 14);

    static JLabel label(String text, int size, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(BODY.deriveFont(size >= 19 ? Font.BOLD : Font.PLAIN, (float) size));
        l.setForeground(c);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    static JButton button(String text, Color c, Runnable action) {
        JButton b = new JButton(text);
        b.setFont(BODY.deriveFont(Font.BOLD, 14f));
        b.setBackground(c);
        b.setForeground(c.equals(BLUE) || c.equals(MINT) || c.equals(GOLD) ? BG : INK);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(12, 16, 12, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

    static JPanel box() {
        JPanel p = new JPanel();
        p.setBackground(PANEL);
        p.setBorder(
                new CompoundBorder(new LineBorder(SOFT, 1, true), new EmptyBorder(22, 24, 22, 24)));
        return p;
    }

    static DefaultTableModel model(String[] cols) {
        return new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setFont(BODY);
        t.setRowHeight(42);
        t.setBackground(PANEL);
        t.setForeground(INK);
        t.setSelectionBackground(new Color(39, 78, 107));
        t.setSelectionForeground(INK);
        t.setGridColor(SOFT);
        t.setShowVerticalLines(false);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setBackground(SOFT);
        t.getTableHeader().setForeground(INK);
        t.getTableHeader().setFont(BODY.deriveFont(Font.BOLD, 12f));
        t.getTableHeader().setPreferredSize(new Dimension(100, 38));
        return t;
    }

    static JScrollPane scroll(JTable t) {
        JScrollPane s = new JScrollPane(t);
        s.setColumnHeaderView(t.getTableHeader());
        s.setBorder(new LineBorder(SOFT));
        s.getViewport().setBackground(PANEL);
        return s;
    }

    static void styleScroll(JScrollPane pane) {
        JScrollBar bar = pane.getVerticalScrollBar();
        bar.setPreferredSize(new Dimension(9, 0));
        bar.setUI(
                new javax.swing.plaf.basic.BasicScrollBarUI() {
                    protected void configureScrollBarColors() {
                        thumbColor = SOFT;
                        trackColor = PANEL;
                    }

                    protected JButton createDecreaseButton(int o) {
                        return tiny();
                    }

                    protected JButton createIncreaseButton(int o) {
                        return tiny();
                    }

                    JButton tiny() {
                        JButton b = new JButton();
                        b.setPreferredSize(new Dimension(0, 0));
                        b.setMinimumSize(new Dimension(0, 0));
                        b.setMaximumSize(new Dimension(0, 0));
                        return b;
                    }

                    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                        if (r.isEmpty()) return;
                        Graphics2D a = (Graphics2D) g.create();
                        a.setRenderingHint(
                                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        a.setColor(new Color(68, 91, 111));
                        a.fillRoundRect(
                                r.x + 1, r.y + 2, Math.max(3, r.width - 2), r.height - 4, 7, 7);
                        a.dispose();
                    }
                });
    }

    static void field(JPanel p, String title, JTextField f) {
        p.add(label(title, 13, MUTED));
        f.setFont(BODY);
        f.setBackground(BG);
        f.setForeground(INK);
        f.setCaretColor(BLUE);
        f.setBorder(new CompoundBorder(new LineBorder(SOFT), new EmptyBorder(7, 10, 7, 10)));
        p.add(f);
    }
}
