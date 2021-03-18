package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Server {



    static boolean status = true;
    //key=String, value=list[id, ip, port]
    static HashMap<String, ArrayList<String>> client_information = new HashMap<String, ArrayList<String>>();
    public static void main(String[] args) throws IOException, InterruptedException {

        ServerSocket listener = new ServerSocket(9090);
        while(status){
            System.out.println("[Server] Server waiting for client connection");
            Socket client = listener.accept();
            ClientHandler clientHandler = new ClientHandler(client);
            Thread thread = new Thread(clientHandler);
            thread.start();
            thread.join();

        }
        listener.close();

    }

}
