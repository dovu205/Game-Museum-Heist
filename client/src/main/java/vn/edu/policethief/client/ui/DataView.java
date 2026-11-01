package vn.edu.policethief.client.ui;

import static vn.edu.policethief.client.ui.UiTheme.*;

import vn.edu.policethief.client.controller.ClientActions;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

final class DataView extends JDialog {
    private final ClientActions actions;
    final DefaultTableModel rows;
    final JButton previous, next;
    final JLabel pageLabel;
    final String type;
    int page;

    DataView(MainFrame window, ClientActions actions, String type, String title, String[] columns) {
        super(window, title, false);
        this.actions = actions;
        this.type = type;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        rows = model(columns);
        add(scroll(table(rows)), BorderLayout.CENTER);
        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 10));
        navigation.setBackground(PANEL);
        previous = button("← Trang trước", SOFT, () -> load(page - 1));
        next = button("Trang sau →", SOFT, () -> load(page + 1));
        pageLabel = label("", 13, INK);
        navigation.add(previous);
        navigation.add(pageLabel);
        navigation.add(next);
        add(navigation, BorderLayout.SOUTH);
        setSize(columns.length > 7 ? 1100 : 850, 450);
        setLocationRelativeTo(window);
    }

    void load(int target) {
        actions.requestPage(type, target);
    }

    void update(TablePage message) {
        fill(rows, message.rows());
        page = message.page();
        int pages = message.pages(), total = message.total();
        pageLabel.setText("Trang " + (page + 1) + " / " + pages + " · " + total + " bản ghi");
        previous.setEnabled(page > 0);
        next.setEnabled(page + 1 < pages);
    }
}
