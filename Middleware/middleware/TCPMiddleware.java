package middleware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import Server.TCP.TCPResourceManager;

public class TCPMiddleware extends Middleware {

  private static String[] s_serverHosts = new String[] { "localhost", "localhost", "localhost" };
  private static int s_serverPort = 1099;
  private String[] serverNames;
  private static ServerSocket server;

  public TCPMiddleware(String[] args) {
    super(new TCPResourceManager(args[2]), new TCPResourceManager(args[4]), new TCPResourceManager(args[6]));
    try {
      server = new ServerSocket(Integer.valueOf(args[0]),1,InetAddress.getLocalHost());
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    s_serverHosts = new String[] { args[1], args[3], args[5] };
    serverNames = new String[] { args[2], args[4], args[6] };
  }
  
  private void listen() throws Exception {
    String data = null;
    Socket clientSocket = server.accept();
    System.out.println("1");
    String clientAddress = clientSocket.getInetAddress().getHostAddress();

    System.out.println("2");
    System.out.println("\r\nNew connection from " + clientAddress);
    
    BufferedReader in = new BufferedReader(
            new InputStreamReader(clientSocket.getInputStream()));        
    while ( (data = in.readLine()) != null ) {
        System.out.println("\r\nMessage from " + clientAddress + ": " + data);
    }
  }

  public static void main(String[] args) {
    System.out.println("TCPMiddleware successfully called! :)");

    if (args.length > 0) {
      s_serverPort = Integer.valueOf(args[0]);
    }

    // Create the RMI server entry
    try {
      TCPMiddleware middleware = new TCPMiddleware(args);
      System.out.println(server.getLocalPort());
      middleware.listen();
    } catch (Exception e) {
      System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mUncaught exception");
      e.printStackTrace();
      System.exit(1);
    }

    // Create and install a security manager
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new SecurityManager());
    }
  }

}
