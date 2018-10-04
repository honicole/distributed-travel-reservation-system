package Client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class TCPClient extends Client {
  private static String s_serverHost = "localhost";
  private static int s_serverPort = 1099;

  private Socket socket;

  public TCPClient(String host, int port) throws Exception {
    super();
    this.socket = new Socket(InetAddress.getByName(host), port);
  }
  
  public TCPClient(Socket socket) throws Exception {
    super();
    this.socket = socket;
  }

  public static void main(String args[]) {
    if (args.length > 0) {
      s_serverHost = args[0];
    }
    if (args.length > 1) {
      s_serverPort = Integer.valueOf(args[1]);
    }
    if (args.length > 2) {
      System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27
          + "[0mUsage: java client.TCPClient [server_hostname [server_port]]");
      System.exit(1);
    }

    // Set the security policy
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new SecurityManager());
    }

    
    try (
        Socket socket = new Socket(s_serverHost, s_serverPort);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    ) {
      BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
      String fromServer, fromUser;
      
      
      System.out.println("Calling new TCPClient(host: " + s_serverHost + ", " + s_serverPort + ")");
      TCPClient client = new TCPClient(socket);
      while((fromServer = in.readLine()) != null) {
        System.out.println(fromServer);
      }
      

//      System.out.println("33333");
      // client.start();
    } catch (Exception e) {
      System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUncaught exception");
      e.printStackTrace();
      System.exit(1);
    }
  }

}
