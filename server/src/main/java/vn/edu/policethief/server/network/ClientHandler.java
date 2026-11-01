package vn.edu.policethief.server.network;

import vn.edu.policethief.server.Store;
import vn.edu.policethief.server.lobby.Room;
import vn.edu.policethief.shared.protocol.Packet;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** One TCP player connection; dispatches game mutations under the server monitor. */
public final class ClientHandler {
    private final TcpServer server;
    public final Socket socket;
    public final BlockingQueue<String> out = new ArrayBlockingQueue<>(256);
    public Store.User user;
    public Room room;
    public volatile boolean closed;
    public int failures;
    public long retryAt;

    public ClientHandler(TcpServer server, Socket s) {
        this.server = server;
        socket = s;
    }

    public void start() {
        Thread writer =
                new Thread(
                        () -> {
                            try (BufferedWriter w =
                                    new BufferedWriter(
                                            new OutputStreamWriter(
                                                    socket.getOutputStream(),
                                                    StandardCharsets.UTF_8))) {
                                while (!closed) {
                                    String line = out.take();
                                    w.write(line);
                                    w.newLine();
                                    w.flush();
                                }
                            } catch (Exception e) {
                                server.disconnect(this);
                            }
                        },
                        "client-writer");
        writer.setDaemon(true);
        writer.start();
        Thread reader =
                new Thread(
                        () -> {
                            try (BufferedReader r =
                                    new BufferedReader(
                                            new InputStreamReader(
                                                    socket.getInputStream(),
                                                    StandardCharsets.UTF_8))) {
                                String line;
                                while (!closed && (line = Packet.read(r)) != null) {
                                    try {
                                        String[] m = Packet.decode(line);
                                        synchronized (server) {
                                            server.handle(this, m);
                                        }
                                    } catch (IllegalArgumentException
                                            | IndexOutOfBoundsException e) {
                                        send("ERROR", "Yêu cầu không hợp lệ.");
                                    }
                                }
                            } catch (IOException e) {
                            } finally {
                                server.disconnect(this);
                                writer.interrupt();
                            }
                        },
                        "client-reader");
        reader.setDaemon(true);
        reader.start();
    }

    public void send(String type, Object... args) {
        if (!closed && !out.offer(Packet.encode(type, args))) server.disconnect(this);
    }
}
