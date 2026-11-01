package vn.edu.policethief.client.ui;

import static vn.edu.policethief.client.ui.UiTheme.*;

import vn.edu.policethief.client.controller.ClientActions;
import vn.edu.policethief.client.model.ClientState;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/** Swing presentation for AuthView; client actions are delegated to the controller. */
final class AuthView {
    private final MainFrame window;
    private final ClientState state;
    private final ClientActions actions;
    JLabel authStatus;
    final JTextField host = new JTextField("127.0.0.1"),
            port = new JTextField("5555"),
            name = new JTextField();
    final JPasswordField password = new JPasswordField(), confirm = new JPasswordField();
    JButton login, register;

    AuthView(MainFrame window, ClientState state, ClientActions actions) {
        this.window = window;
        this.state = state;
        this.actions = actions;
    }

    void showAuth(boolean signup) {
        window.closeDataViews();
        window.setShell(false);
        password.setText("");
        confirm.setText("");
        password.setEchoChar('●');
        confirm.setEchoChar('●');
        window.content.removeAll();
        window.content.setLayout(new GridBagLayout());
        JPanel card = box();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(
                new CompoundBorder(new LineBorder(SOFT, 1, true), new EmptyBorder(30, 34, 26, 34)));
        card.setPreferredSize(new Dimension(410, signup ? 660 : 600));
        JLabel brand = label("MUSEUM HEIST", 25, INK);
        card.add(brand);
        card.add(Box.createVerticalStrut(7));
        card.add(label("TRUY BẮT TRONG BẢO TÀNG", 11, BLUE));
        card.add(Box.createVerticalStrut(27));
        card.add(label(signup ? "Tạo tài khoản" : "Đăng nhập", 22, INK));
        card.add(Box.createVerticalStrut(7));
        card.add(
                label(
                        signup ? "Tham gia cuộc truy đuổi cùng bạn bè." : "Chào mừng bạn quay lại.",
                        13,
                        MUTED));
        card.add(Box.createVerticalStrut(23));
        authField(card, "Tên tài khoản", name);
        authField(card, "Mật khẩu", password);
        if (signup) authField(card, "Xác nhận mật khẩu", confirm);
        JCheckBox showPassword = new JCheckBox("Hiện mật khẩu");
        showPassword.setOpaque(false);
        showPassword.setForeground(MUTED);
        showPassword.setFont(BODY.deriveFont(12f));
        showPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        showPassword.addActionListener(
                e -> {
                    password.setEchoChar(showPassword.isSelected() ? (char) 0 : '●');
                    confirm.setEchoChar(showPassword.isSelected() ? (char) 0 : '●');
                });
        card.add(showPassword);
        card.add(Box.createVerticalStrut(15));
        login = button("Đăng nhập", BLUE, () -> authenticate(false));
        register = button("Tạo tài khoản", BLUE, () -> authenticate(true));
        JButton primary = signup ? register : login;
        primary.setAlignmentX(Component.LEFT_ALIGNMENT);
        primary.setMaximumSize(new Dimension(1000, 44));
        card.add(primary);
        window.getRootPane().setDefaultButton(primary);
        card.add(Box.createVerticalStrut(10));
        JButton toggle =
                button(
                        signup ? "Đã có tài khoản? Đăng nhập" : "Chưa có tài khoản? Đăng ký",
                        PANEL,
                        () -> actions.showAuth(!signup));
        toggle.setBorder(new EmptyBorder(8, 0, 8, 0));
        toggle.setForeground(BLUE);
        toggle.setFont(BODY.deriveFont(13f));
        toggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(toggle);
        if (!window.hosting) {
            JButton connection = button("Kết nối phòng chơi…", PANEL, actions::connectionDialog);
            connection.setBorder(new EmptyBorder(5, 0, 5, 0));
            connection.setFont(BODY.deriveFont(12f));
            connection.setForeground(MUTED);
            connection.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(connection);
        }
        card.add(Box.createVerticalStrut(10));
        authStatus = label(" ", 12, MUTED);
        authStatus.setPreferredSize(new Dimension(340, 48));
        authStatus.setMinimumSize(new Dimension(340, 48));
        authStatus.setMaximumSize(new Dimension(340, 48));
        authStatus.setVerticalAlignment(SwingConstants.TOP);
        card.add(authStatus);
        window.content.add(card, new GridBagConstraints());
        window.refresh();
        name.requestFocusInWindow();
    }

    void authenticate(boolean signup) {
        actions.authenticate(
                signup,
                name.getText(),
                new String(password.getPassword()),
                new String(confirm.getPassword()),
                host.getText(),
                port.getText());
    }

    void authField(JPanel card, String title, JTextField input) {
        card.add(label(title, 12, MUTED));
        card.add(Box.createVerticalStrut(6));
        input.setFont(BODY);
        input.setBackground(BG);
        input.setForeground(INK);
        input.setCaretColor(BLUE);
        input.setBorder(new CompoundBorder(new LineBorder(SOFT), new EmptyBorder(9, 12, 9, 12)));
        input.setAlignmentX(Component.LEFT_ALIGNMENT);
        input.setMaximumSize(new Dimension(1000, 42));
        input.setPreferredSize(new Dimension(340, 42));
        card.add(input);
        card.add(Box.createVerticalStrut(14));
    }

    void connectionDialog() {
        String oldHost = host.getText(), oldPort = port.getText();
        JPanel fields = new JPanel(new GridLayout(2, 2, 12, 12));
        fields.setBackground(PANEL);
        field(fields, "IP Radmin của máy chủ phòng", host);
        field(fields, "Cổng kết nối", port);
        int result =
                JOptionPane.showConfirmDialog(
                        window,
                        fields,
                        "Kết nối phòng chơi",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION)
            actions.notice("Phòng chơi: " + host.getText().trim() + ":" + port.getText().trim());
        else {
            host.setText(oldHost);
            port.setText(oldPort);
        }
    }
}
