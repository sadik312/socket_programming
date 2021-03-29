package tests;

import client.CClient2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import java.io.IOException;
import company.Server;

import static company.Server.*;

public class ClientTests {
    public void checkCoordinator() throws IOException, InterruptedException {
        Server server = new Server();

        CClient2 client = new CClient2("1", 9091, "192.168.0.27", "192.168.1.103", 9090);
        System.out.println(client_information);
        String coordinatorId = client_information.get("coordinator").get(0);

        Assertions.assertEquals("1", coordinatorId);
    }
    @Test
    public void messaging() throws IOException {
        String test1Message = "hello";
        String serverMsg = testingMessage;
    }
}
