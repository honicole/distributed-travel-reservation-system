package middleware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import Server.TCP.TCPResourceManager;

public class TCPMiddleware extends Middleware {

  private static String[] s_serverHosts = new String[] { "localhost", "localhost", "localhost" };
  private static int s_serverPort = 1099;
  private String[] serverNames;
  private ServerSocket server;

  private static Executor executor = Executors.newFixedThreadPool(8);

  public TCPMiddleware(String[] args) {
    super(new TCPResourceManager(args[2]), new TCPResourceManager(args[4]), new TCPResourceManager(args[6]));
    try {
      this.server = new ServerSocket(Integer.valueOf(args[0]), 1, InetAddress.getLocalHost());
    } catch (NumberFormatException | IOException e) {
      e.printStackTrace();
    }
    s_serverHosts = new String[] { args[1], args[3], args[5] };
    serverNames = new String[] { args[2], args[4], args[6] };
  }

  private void listen() throws Exception {
    String data = null;
    Socket clientSocket = server.accept();
    String clientAddress = clientSocket.getInetAddress().getHostAddress();
    System.out.println("\r\nNew connection from " + clientAddress);

    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    while ((data = in.readLine()) != null) {
      System.out.println("\r\nMessage from " + clientAddress + ": " + data);
    }
  }

  public static void main(String[] args) {
    System.out.println("TCPMiddleware successfully called! :)");

    if (args.length > 0) {
      s_serverPort = Integer.valueOf(args[0]);
    }

    // Create and install a security manager
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new SecurityManager());
    }

    try (ServerSocket serverSocket = new ServerSocket(s_serverPort);
        Socket clientSocket = serverSocket.accept();
        ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
        ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());)

    {

      String inputLine, outputLine, fromClient;

//        // Initiate conversation with client
//        KnockKnockProtocol kkp = new KnockKnockProtocol();
//        outputLine = kkp.processInput(null);
//        out.println(outputLine);
//
//        while ((inputLine = in.readLine()) != null) {
//            outputLine = kkp.processInput(inputLine);
//            out.println(outputLine);
//            if (outputLine.equals("Bye."))
//                break;
//        }

      while (true) {
        CompletableFuture future = CompletableFuture.supplyAsync(() -> {
          try {
            oos.writeObject("Message recieved");
            return (String) ois.readObject();
          } catch (Exception e) {
            e.printStackTrace();
          }
          return false;
        }, executor);
        System.out.println(future.get());
      }
    } catch (Exception e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

//    try {
//      TCPMiddleware middleware = new TCPMiddleware(args);
//      System.out.println(middleware);
//      middleware.listen();
//    } catch (Exception e) {
//      System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mUncaught exception");
//      e.printStackTrace();
//      System.exit(1);
//    }
  }

}
