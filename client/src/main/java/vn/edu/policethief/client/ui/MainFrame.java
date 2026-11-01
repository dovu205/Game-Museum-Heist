package vn.edu.policethief.client.ui;

import static vn.edu.policethief.client.ui.UiTheme.*;

import vn.edu.policethief.client.controller.ClientActions;
import vn.edu.policethief.client.model.ClientState;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.*;

/** Owns the Swing window and screens. No sockets, database or game-rule mutations. */
public final class MainFrame extends JFrame {
    public static void configureTheme() {
        UIManager.put("Label.font", BODY);
        UIManager.put("Button.font", BODY);
        UIManager.put("OptionPane.background", PANEL);
        UIManager.put("Panel.background", PANEL);
        UIManager.put("OptionPane.messageForeground", INK);
    }

    final boolean hosting;
    final ClientState state;
    final JPanel root = new JPanel(new BorderLayout());
    final JPanel content = new JPanel(new BorderLayout());
    final AuthView auth;
    final LobbyPanel lobby;
    final GamePanel game;
    final ShellView shell;
    final ClientDialogs dialogs;
    final Map<String, DataView> dataViews = new HashMap<>();
    private final ClientActions actions;
    private final Timer uiTimer;

    public MainFrame(boolean hosting, ClientState state, ClientActions actions) {
        super("GameState Heist • Truy bắt trong bảo tàng");
        this.hosting = hosting;
        this.state = state;
        this.actions = actions;
        auth = new AuthView(this, state, actions);
        lobby = new LobbyPanel(this, state, actions);
        game = new GamePanel(this, state, actions);
        shell = new ShellView(this, state, actions);
        dialogs = new ClientDialogs(this, state, actions);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(640, 760);
        setLocationRelativeTo(null);
        root.setBackground(BG);
        setContentPane(root);
        shell.sidePanel = shell.sidebar();
        root.add(shell.sidePanel, BorderLayout.WEST);
        content.setOpaque(false);
        root.add(content);
        shell.footerPanel = new JPanel(new BorderLayout());
        shell.footerPanel.setBackground(PANEL);
        shell.footerPanel.setBorder(new javax.swing.border.EmptyBorder(9, 20, 9, 20));
        shell.status.setForeground(MUTED);
        shell.footerPanel.add(shell.status);
        root.add(shell.footerPanel, BorderLayout.SOUTH);
        addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        if (!state.match.isEmpty() && !state.finished && !confirmLeave()) return;
                        actions.close();
                    }
                });
        uiTimer = new Timer(33, e -> actions.tick());
        uiTimer.start();
    }

    public boolean confirmLeave() {
        return JOptionPane.showConfirmDialog(
                        this,
                        "Rời trận sẽ tính là đầu hàng. Tiếp tục?",
                        "Rời trận",
                        JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    public void configureEndpoint(String host, String port) {
        auth.host.setText(host);
        auth.port.setText(port);
    }

    public void showAuth(boolean signup) {
        auth.showAuth(signup);
    }

    public void showLobby() {
        lobby.showLobby();
    }

    public void showGame() {
        game.showGame();
    }

    public void connectionDialog() {
        auth.connectionDialog();
    }

    public void rules() {
        dialogs.rules();
    }

    public void invite(String from) {
        dialogs.invite(from);
    }

    void clear(String title, String subtitle) {
        shell.clear(title, subtitle);
    }

    void setShell(boolean playing) {
        shell.setShell(playing);
    }

    void refresh() {
        content.revalidate();
        content.repaint();
    }

    public void closeInvitation() {
        if (dialogs.invitation != null) dialogs.invitation.dispose();
    }

    public void clearPasswords() {
        auth.password.setText("");
        auth.confirm.setText("");
    }

    public void setAuthEnabled(boolean enabled) {
        auth.login.setEnabled(enabled);
        auth.register.setEnabled(enabled);
    }

    public void setIdentity(String name) {
        shell.identity.setText(name.isEmpty() ? "MUSEUM HEIST" : "● " + name);
    }

    public void updatePlayers() {
        if (lobby.onlineModel == null) return;
        lobby.onlineModel.setRowCount(0);
        for (var player : state.onlinePlayers) {
            lobby.onlineModel.addRow(
                    new Object[] {player.name(), player.points(), player.status()});
        }
    }

    public void showData(TablePage message) {
        String type = message.type();
        boolean ranking = type.equals("RANKING");
        String[] columns =
                ranking
                        ? new String[] {
                            "Hạng", "Người chơi", "Điểm", "Thắng", "Thua", "Số trận", "Tỷ lệ"
                        }
                        : new String[] {
                            "Mã trận",
                            "Người A",
                            "Người B",
                            "Điểm",
                            "Ván",
                            "Thắng",
                            "Kết thúc (UTC)",
                            "Lý do"
                        };
        DataView data = dataViews.get(type);
        if (data == null || !data.isDisplayable()) {
            data =
                    new DataView(
                            this, actions, type, ranking ? "Bảng xếp hạng" : "Lịch sử", columns);
            dataViews.put(type, data);
        }
        data.update(message);
        data.setVisible(true);
    }

    void closeDataViews() {
        for (DataView view : dataViews.values()) view.dispose();
        dataViews.clear();
    }

    public void renderTurn() {
        game.roleLabel.setText(state.role.equals("POLICE") ? "● CẢNH SÁT" : "● KẺ TRỘM");
        game.roleLabel.setForeground(state.role.equals("POLICE") ? BLUE : GOLD);
        game.roundLabel.setText("Ván " + state.round + " / 2");
        game.turnLabel.setText("Nhịp " + state.turn + " / 32");
        renderScore();
        game.again.setVisible(false);
        updateControls();
    }

    public void renderScore() {
        game.scoreLabel.setText(state.score + " : " + state.otherScore);
    }

    public void setCountdown(String text) {
        game.countdown.setText(text);
    }

    public void showReplay() {
        game.again.setVisible(true);
        game.again.setEnabled(true);
    }

    public void updateControls() {
        game.updateControls();
    }

    public void clearLog() {
        game.log.setText("");
    }

    public void appendLog(String message) {
        if (message == null || message.isBlank()) return;
        String prefix = "V" + state.round + " · " + state.turn + ": ";
        game.log.append((game.log.getText().isBlank() ? "" : "\n\n") + prefix + message);
        game.log.setCaretPosition(game.log.getDocument().getLength());
    }

    public void updateClock() {
        if (game.canvas != null && state.view.equals("GAME")) game.canvas.repaint();
        if (!state.view.equals("GAME") || state.deadline == 0 || state.finished) return;
        long left = Math.max(0, state.deadline - System.currentTimeMillis() - state.serverOffset);
        int seconds = (int) Math.ceil(left / 1000.0);
        setCountdown(state.locked ? "ĐÃ KHÓA" : String.format("%02d giây", seconds));
        game.timebar.setValue((int) left);
        game.timebar.setForeground(seconds <= 2 ? RED : BLUE);
    }

    public void notice(String message) {
        shell.status.setText(message);
        shell.status.setToolTipText(message);
        if (state.isAuth() && auth.authStatus != null) {
            String safe = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            auth.authStatus.setText("<html><div style='width:245px'>" + safe + "</div></html>");
            auth.authStatus.setToolTipText(message);
        }
    }

    public void close() {
        uiTimer.stop();
        closeDataViews();
        closeInvitation();
        dispose();
    }

    public void snapshot(Path path) throws IOException {
        root.setSize(getWidth(), getHeight());
        layoutTree(root);
        BufferedImage image =
                new BufferedImage(root.getWidth(), root.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        root.printAll(graphics);
        graphics.dispose();
        ImageIO.write(image, "png", path.toFile());
    }

    void layoutTree(Container container) {
        container.invalidate();
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) layoutTree(nested);
        }
    }
}
