package vn.edu.policethief.client.ui;

import static vn.edu.policethief.client.ui.UiTheme.*;

import vn.edu.policethief.client.controller.ClientActions;
import vn.edu.policethief.client.model.ClientState;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/** Swing presentation for ClientDialogs; client actions are delegated to the controller. */
final class ClientDialogs {
    private final MainFrame window;
    private final ClientState state;
    private final ClientActions actions;
    JDialog invitation;

    ClientDialogs(MainFrame window, ClientState state, ClientActions actions) {
        this.window = window;
        this.state = state;
        this.actions = actions;
    }

    void invite(String from) {
        if (invitation != null) invitation.dispose();
        JDialog d = new JDialog(window, "Lời thách đấu", false);
        invitation = d;
        d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        JPanel p = box();
        p.setLayout(new BorderLayout(10, 18));
        p.add(label(from + " mời bạn truy bắt trong bảo tàng", 17, INK), BorderLayout.NORTH);
        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.setOpaque(false);
        buttons.add(
                button(
                        "Chấp nhận",
                        MINT,
                        () -> {
                            actions.answerInvite(from, true);
                            d.dispose();
                        }));
        buttons.add(
                button(
                        "Từ chối",
                        SOFT,
                        () -> {
                            actions.answerInvite(from, false);
                            d.dispose();
                        }));
        p.add(buttons);
        d.setContentPane(p);
        d.pack();
        d.setLocationRelativeTo(window);
        d.setVisible(true);
    }

    void rules() {
        String rules;
        try (java.io.InputStream in = ClientDialogs.class.getResourceAsStream("/rules.txt")) {
            rules =
                    in == null
                            ? "Không tìm thấy luật chơi. Xem docs/rules.md."
                            : new String(
                                    in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            rules = "Không đọc được luật chơi. Xem docs/rules.md.";
        }
        JTextArea t = new JTextArea(rules);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        t.setFont(BODY);
        t.setBackground(PANEL);
        t.setForeground(INK);
        t.setEditable(false);
        t.setBorder(new EmptyBorder(18, 18, 18, 18));
        JScrollPane scroll = new JScrollPane(t);
        scroll.setPreferredSize(new Dimension(680, 520));
        t.setCaretPosition(0);
        JOptionPane.showMessageDialog(window, scroll, "Luật chơi", JOptionPane.PLAIN_MESSAGE);
    }
}
