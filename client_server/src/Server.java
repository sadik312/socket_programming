

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public static final int port = 9090;
    static String ip;
    final ServerSocket serverSocket = new ServerSocket(port);
    Client coordinator = null;


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
            System.out.println("server starting");
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


    public void handleClient(Socket client){
        checkFirst();

    }

    private void checkFirst(Client client){
        if(coordinator == null){
            //print fist client and new coordinator
            setCoordinator(client);

        }
    }


    private void setCoordinator(Client client){
        coordinator = client;
    }


    public Client getCoordinator(){
        return coordinator;
    }


    public String getCoordinator(boolean ip){
        return coordinator.ip;
    }

}



