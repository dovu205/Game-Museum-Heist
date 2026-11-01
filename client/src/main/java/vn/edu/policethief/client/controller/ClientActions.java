package vn.edu.policethief.client.controller;

/** User intentions. Views do not know sockets or wire message formats. */
public interface ClientActions {
    void showAuth(boolean signup);

    void showLobby();

    void authenticate(
            boolean signup,
            String name,
            String password,
            String confirmation,
            String host,
            String port);

    void chooseMode(String mode);

    void chooseDirection(String direction);

    void submit();

    void refreshPlayers();

    void invitePlayer(String name);

    void answerInvite(String name, boolean accept);

    void logout();

    void leaveMatch();

    void replay();

    void request(String type);

    void requestPage(String type, int page);

    void notice(String message);

    void rules();

    void connectionDialog();

    void mute(boolean muted);

    void volume(int value);

    void tick();

    void close();
}
