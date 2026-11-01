package vn.edu.policethief.server;

import org.junit.Test;

import vn.edu.policethief.server.network.*;

public class ServerSuite {
    @Test
    public void engine() {
        vn.edu.policethief.server.game.EngineTest.main(new String[0]);
    }

    @Test
    public void storage() throws Exception {
        StoreTest.main(new String[0]);
    }

    @Test
    public void network() throws Exception {
        NetworkTest.main(new String[0]);
    }

    @Test
    public void newRulesNetwork() throws Exception {
        NewRulesNetworkTest.main(new String[0]);
    }

    @Test
    public void multiplayer() throws Exception {
        MultiClientTest.main(new String[0]);
    }

    @Test
    public void pages() throws Exception {
        PaginationTest.main(new String[0]);
    }
}
