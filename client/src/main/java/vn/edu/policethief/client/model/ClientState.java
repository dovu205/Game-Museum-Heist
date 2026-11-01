package vn.edu.policethief.client.model;

import vn.edu.policethief.shared.model.GameState;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-visible state only. The server never sends hidden opponent information here. Mutated by
 * GameController on Swing's event thread; views read it to render.
 */
public final class ClientState {
    public int[] relics = GameState.RELICS.clone();
    public List<OnlinePlayer> onlinePlayers = List.of();
    public String username = "", view = "LOGIN", match = "", opponent = "";
    public String role = "POLICE",
            kind = "MOVE",
            direction = "STAY",
            ending = "",
            secondDirection = "STAY";
    public int round, turn, score, otherScore, pos;
    public int exitCell = -1, relicMask = -1, carrying = -1, trapsLeft = 2, slipsLeft = 3;
    public int enemyPos = -1, lastTick = -1;
    public boolean locked = true, finished;
    public String intelligence = "";
    public int[] exits = GameState.EXITS.clone(), visibleTraps = new int[0];
    public int heldTurns;
    public long deadline, serverOffset, animationStart;
    public int animationFrom;
    public int[] animationPath = {};
    public final List<GameState.Clue> clues = new ArrayList<>();

    public boolean isAuth() {
        return view.equals("LOGIN") || view.equals("REGISTER");
    }
}
