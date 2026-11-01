package vn.edu.policethief.server;

import vn.edu.policethief.server.network.TcpServer;

import java.net.BindException;
import java.nio.file.*;
import java.util.*;

import javax.swing.*;

/** Starts the host before opening the player UI; no server controls in gameplay. */
public final class HostMain {
    public static void main(String[] args) throws Exception {
        if (!Arrays.asList(args).contains("--host")) {
            Class.forName("vn.edu.policethief.client.ClientMain")
                    .getMethod("main", String[].class)
                    .invoke(null, (Object) args);
            return;
        }
        TcpServer server = null;
        try {
            int port = Integer.parseInt(System.getProperty("nightchase.port", "5555"));
            if (port < 1 || port > 65535)
                throw new IllegalArgumentException("Cổng phải trong khoảng 1–65535.");
            server = new TcpServer(port, Path.of("data"));
            TcpServer active = server;
            Thread thread =
                    new Thread(
                            () -> {
                                try {
                                    active.run();
                                } catch (Exception e) {
                                    SwingUtilities.invokeLater(
                                            () ->
                                                    JOptionPane.showMessageDialog(
                                                            null,
                                                            "Dịch vụ phòng chơi đã dừng: "
                                                                    + e.getMessage(),
                                                            "GameState Heist",
                                                            JOptionPane.ERROR_MESSAGE));
                                }
                            },
                            "embedded-host");
            thread.setDaemon(true);
            thread.start();
            Runtime.getRuntime().addShutdownHook(new Thread(active::close));
            Class.forName("vn.edu.policethief.client.ClientMain")
                    .getMethod("launch", String[].class, AutoCloseable.class, int.class)
                    .invoke(null, args, active, active.port());
        } catch (Exception e) {
            if (server != null) server.close();
            String message =
                    e instanceof BindException
                            ? "Phòng chơi không mở được vì cổng đang bận. Hãy đóng bản game chủ"
                                  + " phòng đang chạy rồi mở lại. Chỉ chạy một chủ phòng trên máy"
                                  + " 1."
                            : e instanceof AccessDeniedException
                                    ? "Không ghi được dữ liệu. Hãy giải nén game vào thư mục có"
                                          + " quyền ghi rồi mở lại."
                                    : "Không mở được phòng chơi: "
                                            + e.getClass().getSimpleName()
                                            + ": "
                                            + e.getMessage();
            JOptionPane.showMessageDialog(
                    null, message, "GameState Heist", JOptionPane.ERROR_MESSAGE);
        }
    }
}
