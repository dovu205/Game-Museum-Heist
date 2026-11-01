package vn.edu.policethief.client;

import vn.edu.policethief.client.controller.GameController;
import vn.edu.policethief.client.ui.MainFrame;
import vn.edu.policethief.client.ui.preview.ClientPreview;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;

import javax.swing.*;

/** Client entry point: configuration and application startup only. */
public final class ClientMain {
    private ClientMain() {}

    public static void main(String[] args) {
        launch(args, null, 0);
    }

    public static String[] connectionDefaults() {
        Properties defaults = new Properties();
        try (InputStream in = ClientMain.class.getResourceAsStream("/client.properties")) {
            if (in != null) defaults.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("Không đọc được cấu hình client: " + e.getMessage());
        }
        return new String[] {
            System.getProperty("nightchase.host", defaults.getProperty("server.host", "127.0.0.1")),
            System.getProperty("nightchase.port", defaults.getProperty("server.port", "5555"))
        };
    }

    public static void launch(String[] args, AutoCloseable embedded, int port) {
        SwingUtilities.invokeLater(
                () -> {
                    try {
                        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    } catch (Exception ignored) {
                    }
                    MainFrame.configureTheme();
                    GameController controller = new GameController(embedded != null, embedded);
                    if (embedded != null) {
                        controller.window().configureEndpoint("127.0.0.1", String.valueOf(port));
                    } else {
                        String[] endpoint = connectionDefaults();
                        controller.window().configureEndpoint(endpoint[0], endpoint[1]);
                    }
                    if (args.length == 2 && args[0].equals("--preview")) {
                        try {
                            ClientPreview.render(controller, Path.of(args[1]));
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            controller.close();
                        }
                    } else {
                        controller.window().setVisible(true);
                    }
                });
    }
}
