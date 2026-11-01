package vn.edu.policethief.client.ui;

import javax.swing.*;
import javax.swing.border.*;

/** Player action selector and private event log. */
final class ActionPanel extends JPanel {
    ActionPanel() {
        setBackground(UiTheme.PANEL);
        setBorder(new EmptyBorder(17, 17, 16, 17));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }
}
