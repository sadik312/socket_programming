package com.company;


import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static com.company.Server.client_information;
import static com.company.Server.socket_clients;


public class ClientHandler2 implements Runnable{

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    private String coordinator = null;
    private boolean status = false;
    private ArrayList<String> counting;

    private String clientId;

    public ClientHandler2(Socket client) throws IOException {
            this.socket = client;
            this.in = new DataInputStream(client.getInputStream());
            this.out = new DataOutputStream(client.getOutputStream());

    }
    @Override
    public void run() {

        //This runs the first time the connects
        try {
            byte id = in.readByte();
            this.clientId = String.valueOf(Byte.toUnsignedInt(id));
            Packet packet = new Packet(new byte[]{id, in.readByte(), in.readByte()}, in);
            instructions(packet.getHeader().getOpcode(), packet);
            firstClient();
            sendClient();
            System.out.println("[Server"+this.clientId+"] Client connected with Id: " +
                    client_information.get(String.valueOf(Byte.toUnsignedInt(id))).get(0));
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(!status){
            try {
                if( in.available() > 2) {
                    Packet packet = new Packet(new byte[]{in.readByte(), in.readByte(), in.readByte()}, in);
                    instructions(packet.getHeader().getOpcode(), packet);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void instructions(byte opCode, Packet packet) throws IOException {
        switch (opCode) {
            case (byte)0x01 -> addClient(packet); //00000001 00000000 0000001
            case (byte)0x02 -> relayMessage(packet);
            case (byte)0x03 -> checkActive();
            case (byte)0x04 -> disconnected(packet);
            case (byte)0x05,(byte)0x06 -> countingResponse(packet);
            default -> throw new IllegalStateException("Unexpected value: " + opCode);
        }
    }


    private void addClient(Packet packet) throws IOException {

        byte[] data = packet.getData();
        String id = String.valueOf(Byte.toUnsignedInt(data[0]));
        String ip = Byte.toUnsignedInt(data[1]) + "." + Byte.toUnsignedInt(data[2]) + "." +
                Byte.toUnsignedInt(data[3]) + "." + Byte.toUnsignedInt(data[4]);
        String port = Integer.toString ((0xff00 & (data[5]<<8) ) | (255 & data[6]));

        //Sending packet to all the clients connected to let them update their client hash table
        Header header = new Header((byte)0x82, (byte)1);
        Packet sendPacket = new Packet(header, data);
        for(DataOutputStream output : socket_clients.values()){
            output.write(sendPacket.bytePackage());
            output.flush();
        }
        //Adding this client's socket object to the hashmap linked with the id of the client
        socket_clients.put(id, out);
        client_information.put(id, new ArrayList<>(List.of(id, ip, port)));

        if(this.coordinator == null){
            this.coordinator=id;
            setCoordinator();
        }

        System.out.println("[Server"+this.clientId+"] Client added");
        System.out.println(client_information.get(id));
    }

    private void sendClient() throws IOException {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        String[] splitIp;
        payload.write((byte)client_information.size());

        for(String id : client_information.keySet()) {
            if(id.equals("coordinator")){

                payload.write((byte)Integer.parseInt("0"));
                payload.write((byte)
                        Integer.parseInt(client_information.get(coordinator).get(0)));
            }else{
                payload.write((byte)Integer.parseInt(id));
                payload.write((byte)Integer.parseInt(id));
            }
            splitIp = client_information.get(id).get(1).split("\\.");
            payload.write((byte)Integer.parseInt(splitIp[0]));
            payload.write((byte)Integer.parseInt(splitIp[1]));
            payload.write((byte)Integer.parseInt(splitIp[2]));
            payload.write((byte)Integer.parseInt(splitIp[3]));
            payload.write((byte)((
                   (0xff & Integer.parseInt(client_information.get(id).get(2)) >>8))));
            payload.write((byte)(
                    (0xff & Integer.parseInt(client_information.get(id).get(2)))));
        }

        Header header = new Header((byte)0x89, (short) payload.size());
        Packet sendPacket = new Packet(header, payload.toByteArray());
        out.write(sendPacket.bytePackage());

    }


    private void relayMessage(Packet packet){

    }

    private void checkActive() throws IOException {

        Header header = new Header((byte)0x88, (byte)1);
        Packet sendPacket = new Packet(header);

        for(DataOutputStream output : socket_clients.values()){
            output.write(sendPacket.bytePackage());
        }
    }

    private void disconnected(Packet packet) throws IOException {

        // sets status=false which will stop the while loop from checking read bytes
        setStatus();
        String id = Byte.toString(packet.getData()[0]);
        client_information.remove(id);
        socket_clients.remove(id);

        Header header = new Header((byte) 0x83, (byte)1);
        Packet sendPacket = new Packet(header, new byte[]{packet.getData()[0]});

        //iterates through all the clients currently connected and tells them client disconnected
        for(DataOutputStream output : socket_clients.values()){
            output.write(sendPacket.bytePackage());
        }

        in.close();
        out.close();
        socket.close();
        System.out.println("[Server"+this.clientId+"] Client " + id + " disconnected successfully");


    }

    private void respondActive(){

    }

    private void getMessage(){

    }

    private void firstClient() throws IOException {
        Header header = new Header((byte)0x85);
        Packet packet = new Packet(header);
        out.write(packet.bytePackage());

    }

    private void setCoordinator() throws IOException {

        client_information.put("coordinator", client_information.get(this.coordinator));
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        String[] splitIp; //192.168.0.21
        payload.write((byte)Integer.parseInt("0"));
        payload.write((byte)
                Integer.parseInt(client_information.get(coordinator).get(0)));
        splitIp = client_information.get(coordinator).get(1).split("\\.");
        payload.write((byte)Integer.parseInt(splitIp[0]));
        payload.write((byte)Integer.parseInt(splitIp[1]));
        payload.write((byte)Integer.parseInt(splitIp[2]));
        payload.write((byte)Integer.parseInt(splitIp[3]));
        payload.write((byte)((
                (0xff & Integer.parseInt(client_information.get(coordinator).get(2)) >>8))));  //9090 00000000=256 | 0000010>> 00000001 00000000 = 65562
        payload.write((byte)(
                (0xff & Integer.parseInt(client_information.get(coordinator).get(2)))));

        Header header = new Header((byte)0x87, (byte)payload.size());
        Packet packet = new Packet(header, payload.toByteArray());
        out.write(packet.bytePackage());
        out.flush();
    }

    private void setStatus(){
        this.status = true;
    }

    private void updateCoordinator() throws IOException {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        String[] splitIp;
        payload.write((byte)Integer.parseInt("0"));
        payload.write((byte)
                Integer.parseInt(client_information.get(coordinator).get(0)));
        splitIp = client_information.get(coordinator).get(1).split("\\.");
        payload.write((byte)Integer.parseInt(splitIp[0]));
        payload.write((byte)Integer.parseInt(splitIp[1]));
        payload.write((byte)Integer.parseInt(splitIp[2]));
        payload.write((byte)Integer.parseInt(splitIp[3]));
        payload.write((byte)((
                (0xff & Integer.parseInt(client_information.get(coordinator).get(2)) >>8))));
        payload.write((byte)(
                (0xff & Integer.parseInt(client_information.get(coordinator).get(2)))));
        Header header = new Header((byte)0x84, (byte)payload.size());
        Packet sendPacket = new Packet(header, payload.toByteArray());

        for(DataOutputStream output : socket_clients.values()){
            output.write(sendPacket.bytePackage());
        }
    }
    //need to find a solution
    private void countingResponse(Packet packet){

        counting.add(String.valueOf(Byte.toUnsignedInt(packet.getData()[0])));

    }
}
