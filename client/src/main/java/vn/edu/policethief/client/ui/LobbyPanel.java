package vn.edu.policethief.client.ui;

import static vn.edu.policethief.client.ui.UiTheme.*;

import vn.edu.policethief.client.controller.ClientActions;
import vn.edu.policethief.client.model.ClientState;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/** Swing presentation for LobbyPanel; client actions are delegated to the controller. */
final class LobbyPanel {
    private final MainFrame window;
    private final ClientState state;
    private final ClientActions actions;
    JTable online;
    DefaultTableModel onlineModel;

    LobbyPanel(MainFrame window, ClientState state, ClientActions actions) {
        this.window = window;
        this.state = state;
        this.actions = actions;
    }

    void showLobby() {
        window.setShell(true);
        window.getRootPane().setDefaultButton(null);
        window.clear("Bảo tàng lúc nửa đêm", "Một người lấy báu vật. Một người lần theo dấu vết.");
        JPanel body = box();
        body.setLayout(new BorderLayout(0, 18));
        body.add(label("NGƯỜI CHƠI TRỰC TUYẾN", 13, GOLD), BorderLayout.NORTH);
        onlineModel = model(new String[] {"Người chơi", "Điểm", "Trạng thái"});
        online = table(onlineModel);
        body.add(scroll(online));
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionsPanel.setOpaque(false);
        actionsPanel.add(
                button(
                        "Thách đấu →",
                        BLUE,
                        () -> {
                            int row = online.getSelectedRow();
                            if (row < 0) {
                                actions.notice("Chọn một người chơi trong danh sách.");
                                return;
                            }
                            String target = String.valueOf(online.getValueAt(row, 0));
                            actions.invitePlayer(target);
                        }));
        actionsPanel.add(button("Làm mới", SOFT, () -> actions.refreshPlayers()));
        actionsPanel.add(button("Đăng xuất", SOFT, () -> actions.logout()));
        body.add(actionsPanel, BorderLayout.SOUTH);
        window.content.add(body);
        window.refresh();
    }
}
