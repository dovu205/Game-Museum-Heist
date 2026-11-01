package vn.edu.policethief.server.lobby;

import vn.edu.policethief.server.Store;
import vn.edu.policethief.server.game.*;
import vn.edu.policethief.server.network.*;
import vn.edu.policethief.shared.GameRules;
import vn.edu.policethief.shared.model.*;

import java.io.IOException;
import java.util.*;

/** Two-round match lifecycle, guarded by the owning server monitor. */
public final class Room {
    private final TcpServer server;
    private final TurnManager turns = new TurnManager();
    private final VisibilityService visibility = new VisibilityService();
    private final TrapService traps = new TrapService();
    public final String id = UUID.randomUUID().toString();
    public final long start = System.currentTimeMillis();
    public final ClientHandler[] players;
    public final int firstPolice;
    public int round = 1, police;
    public final int[] score = {0, 0};
    public String phase = "";
    public long deadline, phaseEnd;
    public GameSession board;
    public PlayerAction[] actions = new PlayerAction[2];
    public boolean[] again = new boolean[2];

    public Room(TcpServer server, ClientHandler a, ClientHandler b) {
        this.server = server;
        players = new ClientHandler[] {a, b};
        firstPolice = server.random.nextInt(2);
    }

    public int index(ClientHandler p) {
        return players[0] == p ? 0 : 1;
    }

    public void both(String type, Object... args) {
        for (ClientHandler p : players) p.send(type, args);
    }

    public void prepare() {
        police = (firstPolice + round - 1) % 2;
        board = new MapGenerator().generate(server.random);
        beginTurn(
                "Ván "
                        + round
                        + ": "
                        + (round == 1 ? "Giữ bí mật hành động của bạn." : "Hai người đã đổi vai."));
    }

    public void beginTurn(String note) {
        phase = "TURN";
        actions = new PlayerAction[2];
        long now = System.currentTimeMillis();
        deadline = turns.deadline(now);
        for (int k = 0; k < 2; k++) {
            boolean cop = k == police;
            players[k].send(
                    "STATE",
                    id,
                    round,
                    board.turn,
                    cop ? "POLICE" : "THIEF",
                    players[1 - k].user.name,
                    score[k],
                    score[1 - k],
                    cop ? board.cop : board.thief,
                    GameRules.FIXED_EXIT,
                    cop ? -1 : board.relicMask,
                    cop ? -1 : board.carrying ? 1 : 0,
                    cop ? board.trapsLeft : -1,
                    cop ? -1 : board.slipsLeft,
                    cop ? board.clueText() : "",
                    deadline,
                    now,
                    note,
                    board.relicText(),
                    visibility.opponent(board, cop, false),
                    traps.visible(board, cop),
                    cop ? 0 : board.heldTurns,
                    cop ? board.intelligence : "",
                    GameRules.cells(Arrays.stream(GameRules.EXITS).boxed().toList()));
        }
    }

    public String pathText(List<Integer> path) {
        return GameRules.cells(path);
    }

    public void resolve() {
        if (!phase.equals("TURN")) return;
        boolean[] timeout = {actions[0] == null, actions[1] == null};
        for (int k = 0; k < 2; k++) actions[k] = turns.effective(actions[k]);
        GameState.Result result = board.resolve(actions[police], actions[1 - police]);
        boolean ended = !result.ending().equals("NONE");
        for (int k = 0; k < 2; k++) {
            boolean cop = k == police;
            String note = timeout[k] ? "Hết 8 giây: bạn tự động đứng yên. " : "";
            if (cop) {
                if (actions[k].kind().equals("TRAP"))
                    note += "Đã đặt bẫy; có hiệu lực từ nhịp tiếp theo. ";
                if (result.noise()) note += "Có tiếng động mới trên bản đồ. ";
                if (result.trace()) note += "Phát hiện dấu chân. ";
                if (result.collected()) note += "Chuông bảo tàng: một báu vật đã bị lấy. ";
                if (result.trapped())
                    note += "Bẫy kích hoạt: trộm tại " + GameRules.cellName(board.thief) + ". ";
                if (!board.intelligence.isBlank()) note += board.intelligence;
            } else {
                if (result.collected()) note += "Đã lấy báu vật! Hãy tới một cửa thoát. ";
                if (actions[k].kind().equals("DASH"))
                    note += "Bạn chạy nhanh và để lại tiếng động tại ô xuất phát. ";
                if (actions[k].kind().equals("SLIP")) note += "Đã luồn lách. ";
                if (result.trapped())
                    note +=
                            "Bạn mắc bẫy tại "
                                    + GameRules.cellName(board.thief)
                                    + "; phải đứng yên nhịp tiếp theo. ";
            }
            if (note.isBlank()) note = "Đã thực hiện hành động.";
            players[k].send(
                    "UPDATE",
                    id,
                    round,
                    board.turn,
                    pathText(cop ? result.copPath() : result.thiefPath()),
                    cop ? board.cop : board.thief,
                    cop ? board.clueText() : "",
                    cop ? -1 : board.relicMask,
                    cop ? -1 : board.carrying ? 1 : 0,
                    cop ? board.trapsLeft : -1,
                    cop ? -1 : board.slipsLeft,
                    note,
                    cop ? board.intelligence : "",
                    result.ending(),
                    visibility.opponent(board, cop, ended),
                    ended ? pathText(cop ? result.thiefPath() : result.copPath()) : "",
                    GameRules.FIXED_EXIT,
                    System.currentTimeMillis(),
                    traps.visible(board, cop),
                    cop ? 0 : board.heldTurns,
                    GameRules.cells(Arrays.stream(GameRules.EXITS).boxed().toList()));
        }
        if (ended) {
            int winner = result.ending().equals("ESCAPE") ? 1 - police : police;
            score[winner]++;
            phase = "ROUND_END";
            phaseEnd = System.currentTimeMillis() + 3800;
            String reason =
                    switch (result.ending()) {
                        case "CAPTURE" -> "Cảnh sát đã bắt được trộm.";
                        case "ESCAPE" -> "Trộm đã mang báu vật ra khỏi bảo tàng.";
                        default -> "Hết 32 nhịp: cảnh sát bảo vệ thành công.";
                    };
            for (int k = 0; k < 2; k++)
                players[k].send(
                        "ROUND_END",
                        k == winner ? "WIN" : "LOSE",
                        round,
                        score[k],
                        score[1 - k],
                        reason,
                        board.cop,
                        board.thief,
                        GameRules.FIXED_EXIT);
        } else {
            phase = "RESOLVE";
            phaseEnd = System.currentTimeMillis() + 1200;
        }
    }

    public void finish(int winner, String reason) {
        if (phase.equals("FINISHED")) return;
        phase = "FINISHED";
        phaseEnd = System.currentTimeMillis() + 60000;
        Store.Match result =
                new Store.Match(
                        id,
                        players[0].user.name,
                        players[1].user.name,
                        score[0],
                        score[1],
                        round,
                        winner < 0 ? "" : players[winner].user.name,
                        start,
                        System.currentTimeMillis(),
                        "Bảo tàng · " + reason);
        try {
            server.store.finish(result);
        } catch (IOException e) {
            server.pendingResults.put(id, result);
            server.nextSaveRetry = System.currentTimeMillis() + 5000;
            both("ERROR", "Chưa lưu được kết quả. Server sẽ thử lại; hãy giữ server mở.");
            e.printStackTrace();
        }
        for (int k = 0; k < 2; k++)
            players[k].send(
                    "FINISHED",
                    winner < 0 ? "DRAW" : winner == k ? "WIN" : "LOSE",
                    score[k],
                    score[1 - k],
                    reason);
        server.lobbies();
    }

    public void tick(long now) {
        switch (phase) {
            case "TURN" -> {
                if (turns.ready(actions, now, deadline)) resolve();
            }
            case "RESOLVE" -> {
                if (now >= phaseEnd) {
                    board.turn++;
                    beginTurn("Chọn hành động cho nhịp tiếp theo.");
                }
            }
            case "ROUND_END" -> {
                if (now >= phaseEnd) {
                    if (round == 1) {
                        round = 2;
                        prepare();
                    } else
                        finish(
                                score[0] == score[1] ? -1 : score[0] > score[1] ? 0 : 1,
                                "Hoàn tất hai ván đổi vai");
                }
            }
            case "FINISHED" -> {
                if (now >= phaseEnd) {
                    for (ClientHandler p : players) {
                        p.room = null;
                        p.send("BACK", "Phòng đã đóng sau 60 giây.");
                    }
                    server.rooms.remove(this);
                    server.lobbies();
                }
            }
        }
    }
}
