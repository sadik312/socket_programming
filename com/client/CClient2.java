package client;



import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.*;



public class CClient2 {

    private final String id;
    private final int port;
    private final String ip;
    private boolean isCoordinator;

    private final Thread coordinatorCheck;

    private final Scanner keyboard;
    private Socket socket;

    private final DataOutputStream out;
    private final DataInputStream in;

    private static boolean status = false;

    HashMap<String, ArrayList<String>> clientInformation = new HashMap<>();

    public CClient2(String id, int port, String ip, String s_ip, int s_port) throws IOException, InterruptedException {

        //assigning id ip and port
        this.id = id;
        this.port = port;
        this.ip = ip;

        boolean success = false;
        while (!success){
            try{
                this.socket = new Socket(s_ip, s_port);
                success = true;
            }catch (Exception ConnectException){
                System.out.println("[Client] Connection Refused");
                System.out.println("[client] Trying again to Ip:" + s_ip + " and PORT:" + s_port );
                Thread.sleep(2000);
            }
        }
        this.keyboard = new Scanner(System.in);
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());


        this.coordinatorCheck = new Thread( () -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
            while(isCoordinator){

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    break;
                }
                Header header = new Header((byte)0x03);
                Packet sendPacket = new Packet(header);
                try {
                    out.write(sendPacket.bytePackage());
                    out.flush();
                } catch (SocketException s) {
                    System.out.println("[Client] Connection to server disconnected");
                }catch (IOException e){
                    e.printStackTrace();
                }


            }
        });

    }

    //Main function
    public static void main(String[] args) throws IOException, InterruptedException {
        //CClient2 client = new CClient2(args[0], Integer.parseInt(args[1]), args[2], args[3], Integer.parseInt(args[4]));
        CClient2 client = new CClient2("1", 9091, "192.168.0.27", "192.168.0.26", 9090);

        //This is the initial packet sent to the server about its id, ip and port
        client.sendInitial();


        //THis will catch the CTR+C disconnect
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run(){
                try {
                    client.disconnect();
                } catch (IOException e) {
                    //System.out.println("[client"+client.getId()+"] Disconnected unsafely" );
                }
            }
                                             }
        );

        //This thread instantiation will run on the background which is the user display
        Thread backend = new Thread( () -> {
            System.out.println("Welcome!");

            //Needed to print "first client" if its the first client
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            while(!status) {
                try {
                    client.commands(client.display());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        backend.start();


        while (!status) {
            try {
                if(client.socket.getInputStream().available() > 2){
                    Packet packet = new Packet(
                            new byte[]{client.in.readByte(), client.in.readByte(), client.in.readByte()}, client.in);
                    //System.out.println("opcode received: 0x" + Integer.toHexString(packet.getHeader().getOpcode()));
                    client.instructions(packet.getHeader().getOpcode(), packet);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        //Keep this here as it is in order
        backend.join();
    }


    public String getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }



    private byte display(){

        System.out.println("""
                ---------------------------
                [1] Get Members
                [2] Get Coordinator 
                [3] Send Message
                [4] Disconnect""");
        System.out.print("> ");
        byte b = keyboard.nextByte();
        return b;
    }

    private void commands(byte cmd) throws IOException {
        switch (cmd) {
            case 1 -> getMembers();
            case 2 -> getCoordinator();
            case 3 -> initialiseMessage();
            case 4 -> disconnect();
        }

    }

    public void sendInitial() throws IOException {
        byte[] payload = new byte[7];
        String[] splitIp = ip.split("\\.");
        payload[0] = (byte)Integer.parseInt(id);
        payload[1] = (byte)Integer.parseInt(splitIp[0]);
        payload[2] = (byte)Integer.parseInt(splitIp[1]);
        payload[3] = (byte)Integer.parseInt(splitIp[2]);
        payload[4] = (byte)Integer.parseInt(splitIp[3]);
        payload[5] = (byte)(0xff & (port>>8));
        payload[6] = (byte)(0xff & port);
        Header header = new Header((byte)0x01, (short)payload.length);
        Packet packet = new Packet(header, payload);
        out.write(packet.bytePackage());
        out.flush();
    }
    //starting number of opcode hex is 8: Server to -> Client || if number 0 Client to -> Server
    private void instructions(byte opCode, Packet packet) throws IOException {
        switch (opCode) {
            case (byte)0x81 -> System.out.println("[Server] Connection rejected as ID already used. Change ID.");
            case (byte)0x82 -> addClient(packet);
            case (byte)0x83 -> removeClient(packet);
            case (byte)0x84 -> updateCoordinator(packet);
            case (byte)0x85 -> printFirstClient();
            case (byte)0x86 -> message(packet);
            case (byte)0x87 -> setCoordinator(packet);
            case (byte)0x88 -> respondActive();
            case (byte)0x89 -> acceptInitial(packet);
            default -> throw new IllegalStateException("Unexpected value: " + Byte.toUnsignedInt(opCode));
        }
    }
    private void printFirstClient(){
        System.out.println("[Server] You are the first client");
    }

    private void setCoordinator(Packet packet) {
        byte[] data = packet.getData();
        //The data[0] will be equals to 0 so we skip it: [0, 1, -64, -88, 0, 27, 35, -125]
        String id = String.valueOf(Byte.toUnsignedInt(data[1]));
        String ip = Byte.toUnsignedInt(data[2]) + "." + Byte.toUnsignedInt(data[3]) + "." +
                Byte.toUnsignedInt(data[4]) + "." + Byte.toUnsignedInt(data[5]);
        String port = Integer.toString ((0xff00 & (data[6]<<8) ) | (255 & data[7]));

        clientInformation.remove("coordinator");
        clientInformation.put("coordinator", new ArrayList<>(List.of(id, ip, port)));

        if(id.equals(this.id)){
            this.isCoordinator = true;
            checkActive();
        }


    }

    private void message(Packet packet) throws IOException {
        ByteArrayOutputStream finalMessage = new ByteArrayOutputStream();
        finalMessage.write(packet.getData());
        System.out.println(finalMessage.toString());
    }

    private void removeClient(Packet packet) {
        String id = String.valueOf(Byte.toUnsignedInt(packet.getData()[0]));
        clientInformation.remove(id);
        System.out.println("\n[Server] Client " + id + " disconnected.");
    }

    private void updateCoordinator(Packet packet) {
        String id = String.valueOf(Byte.toUnsignedInt(packet.getData()[0]));
        clientInformation.replace("coordinator", clientInformation.get(id));
    }

    private void addClient(Packet packet) {
        byte[] data = packet.getData();
        String id = String.valueOf(Byte.toUnsignedInt(data[0]));
        String ip = Byte.toUnsignedInt(data[1]) + "." + Byte.toUnsignedInt(data[2]) + "." +
                Byte.toUnsignedInt(data[3]) + "." + Byte.toUnsignedInt(data[4]);
        String port = Integer.toString ((0xff00 & (data[5]<<8) ) | ((255 & data[6])));

        clientInformation.put(id, new ArrayList<>(List.of(id, ip, port)));
        System.out.println("\n[Server] Client "+id+" added");
        System.out.println(clientInformation.get(id));

   }

    private void respondActive(){
        //useless
    }

    private void acceptInitial(Packet packet){
        byte[] data = packet.getData();
        int size = Byte.toUnsignedInt(data[0]);
        int number =1;
        String idKey;
        String id;
        String ip;
        String port;

        for(int i=1; i<size+1; i++){
            idKey = String.valueOf(Byte.toUnsignedInt(data[number]));
            if(idKey.equals("0")){
                idKey = "coordinator";
            }
            id = String.valueOf(Byte.toUnsignedInt(data[number+1]));
            ip = Byte.toUnsignedInt(data[number+2]) + "." + Byte.toUnsignedInt(data[number+3]) + "." +
                    Byte.toUnsignedInt(data[number+4]) + "." + Byte.toUnsignedInt(data[number+5]);
            port = Integer.toString (((0xff00 & data[number+6]<<8) ) | (255 & data[number+7]));
            clientInformation.put(idKey, new ArrayList<>(List.of(id, ip, port)));
        }
    }

    private void disconnect() throws IOException {
        status = true;
        Header header = new Header((byte)0x04, (byte)1);
        Packet packet = new Packet(header, new byte[]{(byte)Integer.parseInt(id)});
        out.write(packet.bytePackage());
        out.flush();
        //terminating the timer Thread if client is coordinator.
        if(coordinatorCheck.isAlive()){
            coordinatorCheck.interrupt();
            try {
                coordinatorCheck.join();
            } catch (InterruptedException interruptedException) {
                //interruptedException.printStackTrace();
            }
        }

        in.close();
        out.close();
        socket.close();
        System.out.println("Connection to Server closed");
    }

    private void getMembers(){

        if(clientInformation.size()==0){
            System.out.println("[Client] No members");
        }else{
            for(String id : clientInformation.keySet()){
                System.out.println("---------------------------");
                System.out.println("ID, IP, PORT");
                System.out.println("[ID]: " + id);
                System.out.print("[");
                for(String field : clientInformation.get(id)){
                    if(field.length() == 4){
                        System.out.println(field+"]");
                    }else{
                        System.out.print(field + ", ");
                    }
                }
            }

        }
    }

    private void getCoordinator() throws IOException {
        System.out.println("-----Coordinator------");
        System.out.println("coordinator: " +
                clientInformation.get("coordinator"));
    }


    /**
     * Used for sending message
     * Initialises the command line to send they typed message to the specified client
     * @throws IOException if the socket is broken to the server
     */
    private void initialiseMessage() throws IOException {

        getMembers();
        System.out.println("[Server] Enter the ID of the member to communicate with:");
        System.out.print("> ");
        String connectingClient = keyboard.next();
        while(!connectingClient.equals("exit")){
            if(clientInformation.containsKey(connectingClient)){
                System.out.println("[Server] Connecting with client "+connectingClient);
                break;
            }else{
                System.out.println("[Server] Client not found, enter again");
                System.out.println("          Or enter \"exit\" to exit");
            }
            System.out.print("> ");
            connectingClient = keyboard.next();
        }
        System.out.print("> ");

        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        Header header;
        Packet packet;
        String message;
        payload.write((byte)Integer.parseInt(connectingClient));
        message = keyboard.nextLine();
        while(!message.equals("exit")){
            payload.write((byte)Integer.parseInt(connectingClient));
            payload.write(message.getBytes(StandardCharsets.UTF_8));
            header = new Header((byte)0x02, (short)payload.size());
            packet = new Packet(header, payload.toByteArray());
            out.write(packet.bytePackage());
            out.flush();
            payload.reset();
            message = keyboard.nextLine();
        }
    }
    //Testing for the sent message
    public String testMessaging(String message) throws IOException {

        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        Header header;
        Packet packet;
        payload.write(message.getBytes(StandardCharsets.UTF_8));
        header = new Header((byte)0x02, (short) payload.size());
        packet = new Packet(header, payload.toByteArray());
        out.write(packet.bytePackage());
        return payload.toString();
  }

    private void checkActive(){
        coordinatorCheck.start();
    }

}
