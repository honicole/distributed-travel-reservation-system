package Client;

import java.io.PrintWriter;
import java.net.Socket;

public class TCPClient extends Client {
  private static String s_serverHost = "localhost";
  private static int s_serverPort = 1099;

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

    try {
      System.out.println("Calling new TCPClient(host: " + s_serverHost
          + ", " + s_serverPort);
      TCPClient client = new TCPClient(s_serverHost, s_serverPort);
      
      //client.start();
    } catch (Exception e) {
      System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUncaught exception");
      e.printStackTrace();
      System.exit(1);
    }
  }

  private Socket socket;

  public TCPClient(String host, int port) throws Exception {
    super();
    this.socket = new Socket(host, port);
  }
}
