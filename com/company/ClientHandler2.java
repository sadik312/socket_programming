package company;


import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import static company.Server.*;

public class ClientHandler2 implements Runnable{

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;


    private String coordinator = null;
    private boolean status = false;

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

            //letting client know they are the first client if they are the first client
            //System.out.println(client_information);
            if(client_information.isEmpty()){ firstClient();}

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
        try {
            in.close();
            out.close();
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public String getClientId(){
        return this.clientId;
    }

    public String getCoordinator(){
        return this.coordinator;
    }

    private void instructions(byte opCode, Packet packet) throws IOException {
        switch (opCode) {
            case (byte)0x01 -> addClient(packet); //00000001 00000000 0000001
            case (byte)0x02 -> relayMessage(packet);
            case (byte)0x03 -> checkActive();
            case (byte)0x04 -> disconnected(packet);
            default -> throw new IllegalStateException("Unexpected value: " + opCode);
        }
    }


    private void addClient(Packet packet) throws IOException {

        byte[] data = packet.getData();
        String id = String.valueOf(Byte.toUnsignedInt(data[0]));
        String ip = Byte.toUnsignedInt(data[1]) + "." + Byte.toUnsignedInt(data[2]) + "." +
                Byte.toUnsignedInt(data[3]) + "." + Byte.toUnsignedInt(data[4]);
        String port = Integer.toString ((0xff00 & (data[5]<<8) ) | (255 & data[6]));

        //Checking case for when client tries to connect with same id
        //  or when new client tries to connect with existing id
        Header header1 = new Header((byte)0x81);
        Packet sendPacket1 = new Packet(header1);
        if(client_information.containsKey(id)){
            if(ip.equals(client_information.get(id).get(1))){
                //only socket_clients hashmap will need updating as client_information will remain the same
                socket_clients.remove(id);
                //This will terminate the before thread that was running for the same client that is reconnecting
                clientObject.get(id).setStatus();
                //This will change the obj reference from the old thread to this thread
                clientObject.put(id, currentObj);
                socket_clients.put(id, out);
            }else{
                out.write(sendPacket1.bytePackage());
                out.flush();
            }
            return;
        }


        //Sending packet to all the clients connected to let them update their client hash table
        Header header = new Header((byte)0x82, (byte)1);
        Packet sendPacket = new Packet(header, data);

        for(String id2 : client_information.keySet()){
            try{
                socket_clients.get(id2).write(sendPacket.bytePackage());
                socket_clients.get(id2).flush();
            }catch (SocketException s){
                disconnected(id2);
                if(id2.equals(coordinator)){
                    this.coordinator = null;
                }
            }
        }

        //Adding this client's socket object to the hashmap linked with the id of the client
        socket_clients.put(id, out);
        client_information.put(id, new ArrayList<>(List.of(id, ip, port)));

        if(this.coordinator == null){
            this.coordinator=id;
            //this.coordinatorChecker.start();
            client_information.remove("coordinator");
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


    private void relayMessage(Packet packet) throws IOException {
        byte[] data = packet.getData();
        String toID = String.valueOf((byte)Byte.toUnsignedInt(data[0]));

        Header header = new Header((byte)0x86, packet.getHeader().getLength());
        Packet sendPacket = new Packet(header, data);
        socket_clients.get(toID).write(sendPacket.bytePackage());
        socket_clients.get(toID).flush();

    }

    private void checkActive() throws IOException {

        Header header = new Header((byte)0x88);
        Packet sendPacket = new Packet(header);

        for(String id : socket_clients.keySet()){
            try{
                socket_clients.get(id).write(sendPacket.bytePackage());
            }catch (SocketException s){
                disconnected(id);
            }
        }
    }
    //safe disconnect
    private void disconnected(Packet packet) throws IOException {

        // sets status=false which will stop the while loop from checking read bytes
        setStatus();
        String id = Byte.toString(packet.getData()[0]);
        client_information.remove(id);
        socket_clients.remove(id);

        if(id.equals(this.coordinator)){

            assignCoordinator();
        }

        Header header = new Header((byte)0x83, (byte)1);
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
    //overloading the disconnected method
    private void disconnected(String id) throws IOException {

        client_information.remove(id);
        socket_clients.remove(id);

        Header header = new Header((byte)0x83, (byte)1);
        Packet sendPacket = new Packet(header, new byte[]{(byte)Integer.parseUnsignedInt(id)});

        //iterates through all the clients currently connected and tells them client disconnected
        for(DataOutputStream output : socket_clients.values()){
            System.out.println("hello");
            output.write(sendPacket.bytePackage());
        }
        //This will set status to true, exiting the while loop in run() on the thread that's communicating
        // with disconnected client
        clientObject.get(id).setStatus();
        System.out.println("[Server"+this.clientId+"] Client " + id + " catastrophically disconnected");
    }

    private void getMessage(){

    }


    private void assignCoordinator() throws IOException {
        this.coordinator=null;
        ArrayList<String> keySet = new ArrayList<>(client_information.keySet());
        /*
        This is needed when in the case of the last client being the coordinator disconnects and the coordinator
        keySet wont be removed, so if keySet size == 1, this means that the coordinator is the only entry on the hashmap
         and should be removed from the list so that a new client joining can be set as a coordinator
        But if the keySet size is not 1 then the coordinator can be assigned to someone else
         */
        if(keySet.size()<=1){
            client_information.remove("coordinator");
        }else{
            this.coordinator = keySet.get(0);
            client_information.replace("coordinator", client_information.get(this.coordinator));
            setCoordinator();
        }

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
                (0xff & Integer.parseInt(client_information.get(coordinator).get(2)) >>8))));
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
        //white methods to select new coordinator and start its CoordinatorCheck thread

        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        String[] splitIp;
        payload.write((byte)Integer.parseInt("0"));
        payload.write((byte)
                Integer.parseInt(client_information.get("coordinator").get(0)));
        splitIp = client_information.get("coordinator").get(1).split("\\.");
        payload.write((byte)Integer.parseInt(splitIp[0]));
        payload.write((byte)Integer.parseInt(splitIp[1]));
        payload.write((byte)Integer.parseInt(splitIp[2]));
        payload.write((byte)Integer.parseInt(splitIp[3]));
        payload.write((byte)((
                (0xff & Integer.parseInt(client_information.get("coordinator").get(2)) >>8))));
        payload.write((byte)(
                (0xff & Integer.parseInt(client_information.get("coordinator").get(2)))));
        Header header = new Header((byte)0x84, (byte)payload.size());
        Packet sendPacket = new Packet(header, payload.toByteArray());

        for(DataOutputStream output : socket_clients.values()){
            output.write(sendPacket.bytePackage());
        }
    }

}
