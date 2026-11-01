package vn.edu.policethief.client;


public final class ClientConfigTest {
    public static void main(String[] args) {
        String expected = args.length == 0 ? "127.0.0.1" : args[0];
        String[] endpoint = ClientMain.connectionDefaults();
        if (!endpoint[0].equals(expected) || !endpoint[1].equals("5555"))
            throw new AssertionError(
                    "Wrong default endpoint: " + java.util.Arrays.toString(endpoint));
        String oldHost = System.getProperty("nightchase.host"),
                oldPort = System.getProperty("nightchase.port");
        try {
            System.setProperty("nightchase.host", "192.168.1.2");
            System.setProperty("nightchase.port", "5556");
            endpoint = ClientMain.connectionDefaults();
            if (!endpoint[0].equals("192.168.1.2") || !endpoint[1].equals("5556"))
                throw new AssertionError("Explicit endpoint override failed");
        } finally {
            if (oldHost == null) System.clearProperty("nightchase.host");
            else System.setProperty("nightchase.host", oldHost);
            if (oldPort == null) System.clearProperty("nightchase.port");
            else System.setProperty("nightchase.port", oldPort);
        }
        System.out.println("PASS: default server " + expected + ":5555 and explicit override.");
    }
}
