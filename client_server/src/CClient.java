import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.Buffer;
import java.util.*;


public class CClient {

    private final String id;
    private final int port;
    private final String ip;

    private final Scanner keyboard;
    private Socket socket;
    private final BufferedReader in;
    private final PrintWriter output;
    private static boolean status = true;

    HashMap<String, ArrayList<String>> clientInformation = new HashMap<String, ArrayList<String>>();

    public CClient(String id, int port, String ip, String s_ip, int s_port) throws IOException, InterruptedException {
        this.id = id;
        this.port = port;
        this.ip = ip;

        System.out.println("[Client] Connecting to Server...");

        boolean success = false;

        while (!success){
            try{
                socket = new Socket(s_ip, s_port);
                success = true;
            }catch (Exception ConnectException){
                System.out.println("[Client] Connection Refused");
                System.out.println("[client] Trying again to Ip:" + s_ip + " and PORT:" + s_port );
                Thread.sleep(2000);
            }
        }

        System.out.println("[Client] Client successfully connected to the Server");

        this.keyboard = new Scanner(System.in);
        this.in = new BufferedReader(
                new InputStreamReader( socket.getInputStream()));
        this.output = new PrintWriter(socket.getOutputStream(), true);
    }

    public static void main (String[] args) throws IOException, InterruptedException {

        //CClient client = new CClient(args[0], args[1], args[2], args[3], args[4], args[5]);
        CClient client = new CClient("1", 9091, "192.168.0.27", "192.168.0.26", 9090 );


        // sends id, ip, port
        client.sendInitial();
        //accepting the hashmap clients form server
        client.initialAccept();

        System.out.println("Welcome!");
        while(status) {
            client.instruction(client.display());
        }

    }

    public void sendInitial(){
        output.println(this.id);
        output.println(this.ip);
        output.println(this.port);
    }

    private byte display(){
        System.out.println("""
                ---------------------------
                [1] Get Members
                [2] Get Coordinator 
                [3] Send Message
                [4] Get all keys
                [5] Disconnect""");
        System.out.print("> ");
        byte command = keyboard.nextByte();
        return command;
    }

    private void instruction(byte cmd) throws IOException {
        switch (cmd) {
            case 1 -> getMembers();
            case 2 -> getCoordinator();
            case 3 -> message();
            case 4 -> getAllKey();
            case 5 -> disconnect();
        }

    }

    private void disconnect() throws IOException {
        status = false;
        socket.close();
        in.close();
        output.close();
        System.out.println("Connection to Server closed");
    }


    private void getMembers(){
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

    private void getCoordinator() throws IOException {
        System.out.println("-----Coordinator------");
        System.out.println("coordinator: " +
                clientInformation.get("coordinator"));
    }

    private void message(){


    }

    private void setMembers(){

    }

    private void updateMembers(){


    }

    private void initialAccept() throws IOException {

        int clientInfoLength = Integer.parseInt(in.readLine());
        //System.out.println(clientInfoLength); //testing
        for(int i =0; i<clientInfoLength; i++){
            String key = in.readLine();
            String id = in.readLine();
            String ip = in.readLine();
            String port = in.readLine();

            //testing
            //System.out.println(id);
            //System.out.println(ip);
            //System.out.println(port);
            //testing


            clientInformation.put(key,
                    new ArrayList<>(List.of(id, ip, port)));
        }
    }


    private static void connectionFailed(){
        System.out.println("[Client] Connection Refused");
        System.out.println("""
                [1] Try again
                [2] Enter different IP and PORT
                [3] Disconnect
                """);
    }

    //hashmap methods
    private void getAllKey(){
        for( String key : clientInformation.keySet()){
            System.out.println("-: " + key);
        }
    }


}
