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


    public static HashMap<String, ArrayList<String>> client_information = new HashMap<>();  //store clients id with their [id, ip, port] as array
    public static HashMap<String, DataOutputStream> socket_clients = new HashMap<>(); //stores client id with their output stream
    public static HashMap<String, ClientHandler2> clientObject = new HashMap<>();
    //stores client id with their corresponding clientHandler2 object to end thread and close open streams.

    public static void main(String[] args) throws IOException, InterruptedException {

        ServerSocket listener = new ServerSocket(9090);
        while (!status) {
            System.out.println("[Server] Server waiting for client connection...");
            Socket client = listener.accept();
            ClientHandler2 clientHandler2 = new ClientHandler2(client);
            currentObj = clientHandler2;
            Thread thread = new Thread(clientHandler2);
            thread.start();

        }
        listener.close();
   }
}
