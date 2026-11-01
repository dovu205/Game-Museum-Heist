package vn.edu.policethief.client.ui;

import static vn.edu.policethief.client.ui.UiTheme.*;

import vn.edu.policethief.client.controller.ClientActions;
import vn.edu.policethief.client.model.ClientState;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/** Swing presentation for ShellView; client actions are delegated to the controller. */
final class ShellView {
    private final MainFrame window;
    private final ClientState state;
    private final ClientActions actions;
    JPanel sidePanel, footerPanel;
    final JLabel status = new JLabel("Sẵn sàng."), identity = new JLabel("MUSEUM HEIST");

    ShellView(MainFrame window, ClientState state, ClientActions actions) {
        this.window = window;
        this.state = state;
        this.actions = actions;
    }

    JPanel sidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(190, 0));
        side.setBackground(PANEL);
        side.setBorder(new EmptyBorder(30, 18, 22, 18));
        side.add(label("MH /", 34, GOLD));
        side.add(Box.createVerticalStrut(9));
        side.add(label("MUSEUM HEIST", 17, INK));
        side.add(label("BẢO TÀNG LÚC NỬA ĐÊM", 10, MUTED));
        side.add(Box.createVerticalStrut(32));
        identity.setForeground(MINT);
        identity.setFont(BODY.deriveFont(12f));
        side.add(identity);
        side.add(Box.createVerticalStrut(20));
        JButton home =
                button(
                        "Sảnh chờ",
                        SOFT,
                        () -> {
                            if (!state.username.isEmpty() && state.match.isEmpty()) {
                                actions.showLobby();
                                actions.refreshPlayers();
                            }
                        });
        JButton rank = button("Bảng xếp hạng", SOFT, () -> actions.request("RANKING"));
        JButton history = button("Lịch sử đấu", SOFT, () -> actions.request("HISTORY"));
        JButton help = button("Luật bảo tàng", SOFT, actions::rules);
        for (JButton b : new JButton[] {home, rank, history, help}) {
            b.setMaximumSize(new Dimension(160, 43));
            side.add(b);
            side.add(Box.createVerticalStrut(9));
        }
        side.add(Box.createVerticalGlue());
        side.add(label("ÂM THANH", 11, MUTED));
        side.add(Box.createVerticalStrut(8));
        JCheckBox mute = new JCheckBox("Tắt âm thanh");
        mute.setOpaque(false);
        mute.setForeground(INK);
        mute.setFont(BODY.deriveFont(12f));
        mute.addActionListener(e -> actions.mute(mute.isSelected()));
        side.add(mute);
        JSlider volume = new JSlider(0, 100, 55);
        volume.setOpaque(false);
        volume.setMaximumSize(new Dimension(150, 30));
        volume.addChangeListener(e -> actions.volume(volume.getValue()));
        side.add(volume);
        side.add(Box.createVerticalStrut(20));
        side.add(label("7 × 7  /  2 VÁN ĐỔI VAI", 10, GOLD));
        return side;
    }

    void clear(String title, String subtitle) {
        window.content.removeAll();
        JPanel h = new JPanel(new GridLayout(2, 1, 0, 4));
        h.setOpaque(false);
        h.setBorder(new EmptyBorder(0, 0, 22, 0));
        h.add(label(title, 29, INK));
        h.add(label(subtitle, 13, MUTED));
        window.content.add(h, BorderLayout.NORTH);
    }

    void setShell(boolean playing) {
        sidePanel.setVisible(playing);
        footerPanel.setVisible(playing);
        window.content.setLayout(new BorderLayout());
        window.content.setBorder(
                new EmptyBorder(
                        playing ? 28 : 20,
                        playing ? 32 : 20,
                        playing ? 12 : 20,
                        playing ? 32 : 20));
        if (playing) {
            window.getRootPane().setDefaultButton(null);
            if (window.getWidth() < 1080) {
                Rectangle screen =
                        GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                window.setMinimumSize(
                        new Dimension(Math.min(1080, screen.width), Math.min(740, screen.height)));
                window.setSize(Math.min(1280, screen.width), Math.min(880, screen.height));
                window.setLocationRelativeTo(null);
            }
        } else {
            window.getRootPane()
                    .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .remove(KeyStroke.getKeyStroke("ENTER"));
            window.root
                    .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .remove(KeyStroke.getKeyStroke("ENTER"));
            Rectangle screen =
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            window.setMinimumSize(
                    new Dimension(Math.min(500, screen.width), Math.min(740, screen.height)));
            window.setSize(Math.min(640, screen.width), Math.min(760, screen.height));
            window.setLocationRelativeTo(null);
        }
    }
}
