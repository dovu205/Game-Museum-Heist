package vn.edu.policethief.client;

import org.junit.Test;

public class ClientSuite {
    @Test
    public void configuration() throws Exception {
        ClientConfigTest.main(new String[0]);
    }

    @Test
    public void ui() throws Exception {
        vn.edu.policethief.client.ui.UiTest.main(new String[0]);
    }

    @Test
    public void tcpFlow() throws Exception {
        vn.edu.policethief.client.ui.ClientFlowTest.main(new String[0]);
    }
}
