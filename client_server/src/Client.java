import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public String ip = "fasdf";
    
    public static void main(String[] args) throws IOException {
    	
    	Socket socket = new Socket("192.168.1.105", 9090);
    	Scanner in = new Scanner(socket.getInputStream());
    	System.out.println("Server response: " + in.nextLine());
    }
}
