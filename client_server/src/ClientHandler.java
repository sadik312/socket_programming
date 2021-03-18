package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.company.Server.client_information;

public class ClientHandler implements Runnable{

    private final BufferedReader in;
    private final PrintWriter out;
    private String coordinator = null;

    public ClientHandler(Socket clientSocket) throws IOException {
        this.in = new BufferedReader(
                new InputStreamReader(
                        clientSocket.getInputStream()
                )
        );
        this.out = new PrintWriter( clientSocket.getOutputStream(), true);
    }


    @Override
    public void run() {
        //add new client
        try {
            //adds to hashmap id(string), ip(string), port(int)
            String id = in.readLine();
            String ip = in.readLine();
            int port = Integer.parseInt(in.readLine());

            //testing
            //System.out.println(id);
            //System.out.println(ip);
            //System.out.println(port);
            //testing

            //adding to hash map
            addClient(id, ip, port);

            if(getCoordinator() == null){
                setCoordinator(id);
            }

            System.out.println("[Server] Client connected with Id: " + client_information.get(id).get(0) );
        } catch (IOException e) {
            e.printStackTrace();
        }


        //sendClients
        //System.out.println("coordinator: " + client_information.get("coordinator"));

        //Sends all the connected clients that are in hashmap
        sendClients();
    }

    private void exit(){
        boolean status = true;
    }

    private void addClient(String id, String ip, int port){
        client_information.put(id.trim(),
                new ArrayList<>(List.of(id, ip, Integer.toString(port))));
    }

    private void sendClients(){
        String clientInfoLength = Integer.toString(client_information.size());

        //testing
        //System.out.println("clientInfoLength should not be 1:" + clientInfoLength);
        //System.out.println("hash length:" + clientInfoLength);
        //System.out.println(clientInfoLength);

        out.write((clientInfoLength+"\n"));

        for(String id : client_information.keySet()){
            //System.out.println(id); //testing
            out.println(id);
            for(String field : client_information.get(id)){
                //System.out.println(field); //testing
                out.println(field);
            }
        }
    }
    //update this so it can extract individual attribute from the list.
    private ArrayList<String> getClient(String client_id){
        return client_information.get(client_id);
    }

    private void setCoordinator(String id){
        coordinator = id;
        client_information.put("coordinator",
                client_information.get(id));
    }

    private String  getCoordinator(){
        return coordinator;
    }

    private void getAllKey(){
        System.out.println("print key-set");
        for( String key: client_information.keySet()){
            System.out.println(key);
        }
    }






}
