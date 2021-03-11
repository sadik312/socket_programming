

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class Server {

    public static final int port = 9090;
    static String ip;
    final ServerSocket serverSocket = new ServerSocket(port);
    String coordinator = null;
    HashMap<String, String> members = new HashMap<String, String>();

    public Server() throws IOException {
        InetAddress localhost = InetAddress.getLocalHost();
        ip = localhost.getHostAddress();


    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }


    public void serverStart() throws IOException {
        while (true) {
            System.out.println("server started");
            Socket clientsocket = serverSocket.accept();
            System.out.println("Accepted connection from " + clientsocket);


            Thread t = new Thread() {
                public void run(){
                    handleClient(clientsocket);
                }

            };
            t.start();
        }




    }


    public void handleClient(String client){
        //checkFirst();
        //members.put(client.ip, coordinator)

    }

    private void checkFirst(String client){
        if(coordinator == null){
            //print fist client and new coordinator
            setCoordinator(client);

        }
    }


    private void setCoordinator(String client){
        coordinator = client;
    }


    public String getCoordinator(){
        return coordinator;
    }


    public String getCoordinator(boolean ip){
        return coordinator;
    }

}



