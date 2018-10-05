package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TCPClient extends Client {
  private static String s_serverHost = "localhost";
  private static int s_serverPort = 1099;

  private static Executor executor = Executors.newFixedThreadPool(8);
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

    try (Socket socket = new Socket(s_serverHost, s_serverPort);
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());)
    {
      BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
      TCPClient client = new TCPClient(socket);
      while (true) {
        Vector<String> arguments = new Vector<String>();
        try {
          System.out.print((char) 27 + "[32;1m\n>] " + (char) 27 + "[0m");
          final String command = stdIn.readLine().trim();
          CompletableFuture future = CompletableFuture.supplyAsync(() -> {
            try {
              oos.writeObject(command);
              return (String) ois.readObject();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return false;
          }, executor);
          System.out.println(future.get());

        } catch (IOException io) {
          System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m" + io.getLocalizedMessage());
          io.printStackTrace();
          System.exit(1);
        }
      }
    } catch (Exception e) {
      System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUncaught exception");
      e.printStackTrace();
      System.exit(1);
    }
  }

}
