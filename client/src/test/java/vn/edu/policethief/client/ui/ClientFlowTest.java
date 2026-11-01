package vn.edu.policethief.client.ui;

import vn.edu.policethief.client.controller.GameController;
import vn.edu.policethief.server.network.TcpServer;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

/** Exercises real screen callbacks through controllers and TCP, not injected UI events. */
public final class ClientFlowTest {
    private static int checks;

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }

    private static void await(BooleanSupplier condition, String message) throws Exception {
        long limit = System.currentTimeMillis() + 12000;
        while (System.currentTimeMillis() < limit) {
            AtomicReference<Boolean> ready = new AtomicReference<>(false);
            SwingUtilities.invokeAndWait(() -> ready.set(condition.getAsBoolean()));
            if (ready.get()) {
                checks++;
                return;
            }
            Thread.sleep(25);
        }
        throw new AssertionError(message);
    }

    private static JButton button(Container parent, String text) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton found && found.getText().equals(text)) return found;
            if (child instanceof Container nested) {
                JButton found = button(nested, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void registerAndLogin(GameController client, String name) throws Exception {
        SwingUtilities.invokeAndWait(
                () -> {
                    client.showAuth(true);
                    AuthView form = client.window().auth;
                    form.name.setText(name);
                    form.password.setText("secret123");
                    form.confirm.setText("secret123");
                    form.register.doClick();
                });
        await(() -> client.state().view.equals("LOGIN"), "registration must return to login");
        SwingUtilities.invokeAndWait(
                () -> {
                    AuthView form = client.window().auth;
                    form.password.setText("secret123");
                    form.login.doClick();
                });
        await(
                () -> client.state().username.equals(name) && client.state().view.equals("LOBBY"),
                "login must reach lobby through real TCP");
    }

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("work"));
        Path directory = Files.createTempDirectory(Path.of("work"), "client-flow-");
        try (TcpServer server = new TcpServer(0, directory)) {
            Thread host =
                    new Thread(
                            () -> {
                                try {
                                    server.run();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            },
                            "test-host");
            host.setDaemon(true);
            host.start();
            GameController[] clients = new GameController[2];
            try {
                SwingUtilities.invokeAndWait(
                        () -> {
                            for (int i = 0; i < clients.length; i++) {
                                clients[i] = new GameController(false);
                                clients[i]
                                        .window()
                                        .configureEndpoint(
                                                "127.0.0.1", String.valueOf(server.port()));
                            }
                        });
                GameController alice = clients[0], bob = clients[1];
                registerAndLogin(alice, "alice");
                registerAndLogin(bob, "bob");
                await(
                        () -> alice.window().lobby.onlineModel.getRowCount() == 2,
                        "lobby must receive both players");
                SwingUtilities.invokeAndWait(
                        () -> {
                            LobbyPanel lobby = alice.window().lobby;
                            for (int row = 0; row < lobby.onlineModel.getRowCount(); row++) {
                                if (lobby.onlineModel.getValueAt(row, 0).equals("bob"))
                                    lobby.online.setRowSelectionInterval(row, row);
                            }
                            button(alice.window().content, "Thách đấu →").doClick();
                        });
                await(
                        () -> bob.window().dialogs.invitation != null,
                        "invite callback must open opponent dialog");
                SwingUtilities.invokeAndWait(
                        () -> button(bob.window().dialogs.invitation, "Chấp nhận").doClick());
                await(
                        () -> alice.state().view.equals("GAME") && bob.state().view.equals("GAME"),
                        "accept must start both game screens");
                SwingUtilities.invokeAndWait(
                        () -> {
                            check(
                                    !alice.state().role.equals(bob.state().role),
                                    "players receive opposite roles");
                            check(
                                    alice.state().enemyPos == -1 && bob.state().enemyPos == -1,
                                    "opponent positions stay hidden");
                            for (GameController client : clients) {
                                client.window()
                                        .root
                                        .getActionMap()
                                        .get("SPACE")
                                        .actionPerformed(null);
                                client.window().game.send.doClick();
                            }
                        });
                await(
                        () -> alice.state().turn >= 2 && bob.state().turn >= 2,
                        "keyboard and submit callbacks must advance the real match");
                SwingUtilities.invokeAndWait(bob::close);
                await(
                        () -> alice.state().view.equals("LOBBY") && alice.state().match.isEmpty(),
                        "closing opponent must return the remaining player to lobby");
                check(
                        new vn.edu.policethief.server.Store(directory)
                                        .login("alice", "secret123")
                                        .wins
                                == 1,
                        "opponent disconnect must persist the remaining player's win");
                SwingUtilities.invokeAndWait(alice::logout);
                await(
                        () ->
                                alice.state().view.equals("LOGIN")
                                        && alice.state().username.isEmpty(),
                        "logout must reset session and screen");
            } finally {
                SwingUtilities.invokeAndWait(
                        () -> {
                            for (GameController client : clients)
                                if (client != null) client.close();
                        });
            }
        }
        System.out.println("PASS: " + checks + " real client flow assertions.");
    }
}
