package vn.edu.policethief.client.ui.preview;

import vn.edu.policethief.client.controller.GameController;

import java.nio.file.*;

/** Deterministic sample scenes for exported screenshots, separate from gameplay. */
public final class ClientPreview {
    private ClientPreview() {}

    public static void render(GameController c, Path dir) throws Exception {
        Files.createDirectories(dir);
        c.window().snapshot(dir.resolve("login.png"));
        c.showAuth(true);
        c.window().snapshot(dir.resolve("signup.png"));
        c.showAuth(false);
        c.notice("Tên tài khoản cần 3–20 ký tự, mật khẩu tối thiểu 6 ký tự.");
        c.window().snapshot(dir.resolve("login-error.png"));
        c.state().username = "Tuan886";
        c.window().setIdentity("Tuan886");
        c.showLobby();
        c.receive(new String[] {"LOBBY", "Tuan886|12|Rỗi\nNightFox|8|Rỗi"});
        c.window().snapshot(dir.resolve("lobby.png"));
        long now = System.currentTimeMillis();
        c.receive(
                new String[] {
                    "STATE",
                    "preview",
                    "1",
                    "5",
                    "THIEF",
                    "NightFox",
                    "0",
                    "0",
                    "42",
                    "42",
                    "1",
                    "0",
                    "-1",
                    "3",
                    "",
                    String.valueOf(now + 8000),
                    String.valueOf(now),
                    "Tìm báu vật.",
                    "29,19",
                    "-1",
                    "",
                    "0",
                    "",
                    "42,6"
                });
        c.chooseMode("SLIP");
        c.chooseDirection("RIGHT");
        c.chooseDirection("UP");
        c.window().snapshot(dir.resolve("thief.png"));
        c.window().setSize(1080, 740);
        c.window().snapshot(dir.resolve("compact.png"));
        c.window().setSize(1280, 880);
        c.receive(
                new String[] {
                    "STATE",
                    "preview",
                    "2",
                    "8",
                    "POLICE",
                    "NightFox",
                    "1",
                    "0",
                    "16",
                    "42",
                    "-1",
                    "-1",
                    "1",
                    "-1",
                    "29,NOISE,2;24,TRACE,1;",
                    String.valueOf(now + 8000),
                    String.valueOf(now),
                    "Tình báo: trộm ở khu Nam.",
                    "29,19",
                    "-1",
                    "10",
                    "0",
                    "Tình báo: trộm ở khu Nam.",
                    "42,6"
                });
        c.chooseMode("TRAP");
        c.chooseDirection("DOWN");
        c.window().snapshot(dir.resolve("police.png"));
        c.state().visibleTraps = new int[] {10, 24};
        c.state().enemyPos = 24;
        c.window().appendLog("Bẫy kích hoạt: trộm tại D4.");
        c.window().snapshot(dir.resolve("trap.png"));
        c.state().ending = "CAPTURE";
        c.state().enemyPos = c.state().pos;
        c.state().locked = true;
        c.window().snapshot(dir.resolve("capture.png"));
    }
}
