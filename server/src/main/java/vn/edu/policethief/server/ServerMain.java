package vn.edu.policethief.server;

import vn.edu.policethief.server.network.TcpServer;

import java.nio.file.Path;

public final class ServerMain {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5555;
        TcpServer server = new TcpServer(port, Path.of("data"));
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        server.run();
    }
}
