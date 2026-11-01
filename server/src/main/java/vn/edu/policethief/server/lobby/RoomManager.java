package vn.edu.policethief.server.lobby;

import vn.edu.policethief.server.network.*;

import java.util.ArrayList;

/** Match collection owned by the TCP server's monitor. */
public final class RoomManager extends ArrayList<Room> {
    public Room create(TcpServer server, ClientHandler a, ClientHandler b) {
        Room room = new Room(server, a, b);
        add(room);
        a.room = room;
        b.room = room;
        return room;
    }
}
