package com.company;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Server {
    static boolean status = true;

    public static HashMap<String, ArrayList<String>> client_information = new HashMap<>();
    public static HashMap<String, DataOutputStream> socket_clients = new HashMap<>();
    public static HashMap<String, ClientHandler2> clientObject = new HashMap<>();
    public static void main(String[] args) throws IOException {
        ServerSocket listener = new ServerSocket(9090);
        while(status){
            System.out.println("[Server] Server waiting for client connection...");
            Socket client = listener.accept();
            ClientHandler2 clientHandler2 = new ClientHandler2(client);
            clientObject.put(clientHandler2.getClientId(), clientHandler2);
            Thread thread = new Thread(clientHandler2);
            thread.start();
        }
        listener.close();
    }
}
