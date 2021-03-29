package company;


import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Server {
    static boolean status = false;
    static ClientHandler2 currentObj;

    private final Scanner keyboard = new Scanner(System.in);

    private final Thread mainThread;

    public static HashMap<String, ArrayList<String>> client_information = new HashMap<>();
    public static HashMap<String, DataOutputStream> socket_clients = new HashMap<>();
    public static HashMap<String, ClientHandler2> clientObject = new HashMap<>();
    public static ClientHandler2 serverTest;
    public static String testingMessage;

    public static void main(String[] args) throws IOException, InterruptedException {

        Server server = new Server();
        Thread.sleep(100);
        while(!status){
            server.start();
        }
    }


    public Server(){
        mainThread = new Thread(){
            public void run(){
                try {
                    listenStart();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        mainThread.start();
    }

    private void start() throws IOException, InterruptedException {
        commands(display());
    }


    private void listenStart() throws IOException {

        ServerSocket listener = new ServerSocket(9090);
        while (!status) {
            System.out.println("[Server] Server waiting for client connection...");
            Socket client = listener.accept();
            ClientHandler2 clientHandler2 = new ClientHandler2(client);
            serverTest = clientHandler2;
            currentObj = clientHandler2;
            Thread thread = new Thread(clientHandler2);
            thread.start();

        }
        listener.close();
    }

    private byte display(){

        System.out.println("""
                ---------------------------
                [1] Get Members
                [2] Get Coordinator 
                [3] Disconnect""");
        System.out.print("> ");
        byte b = keyboard.nextByte();
        return b;
    }

    private void commands(byte cmd) throws IOException, InterruptedException {
        switch (cmd) {
            case 1 -> getClients();
            case 2 -> getCoordinator();
            case 3 -> disconnect();
        }

    }

    private void getClients(){
        System.out.println(client_information);
    }

    private void getCoordinator(){
        if(client_information.isEmpty()){
            System.out.println("No connected client");
        }else{
            System.out.println("[Server] Coordinator:" + client_information.get("coordinator").toString());
        }
    }

    private void disconnect() throws InterruptedException {
        status = true;
        mainThread.interrupt();
        System.out.println("Shutting down");
    }


}
