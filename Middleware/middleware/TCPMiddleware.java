package middleware;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import Client.UserCommand;
import Server.TCP.TCPResourceManager;

public class TCPMiddleware extends Middleware {

  private static String[] s_serverHosts = new String[] { "localhost", "localhost", "localhost" };
  private static int s_serverPort = 1099;
  private static int[] s_serverPorts;
  private ServerSocket server;

  private static Executor executor = Executors.newFixedThreadPool(8);
  private static MiddlewareListener listener;

  private TCPMiddleware() {
  }

  public TCPMiddleware(String[] args) throws Exception {
    super(new TCPResourceManager(args[2]), new TCPResourceManager(args[4]), new TCPResourceManager(args[6]));
    try {
      this.server = new ServerSocket(Integer.valueOf(args[0]), 1, InetAddress.getLocalHost());
    } catch (NumberFormatException | IOException e) {
      e.printStackTrace();
    }
    s_serverHosts = new String[] { args[1], args[3], args[5] };
    s_serverPorts = new int[] { Integer.valueOf(args[2]), Integer.valueOf(args[4]), Integer.valueOf(args[6]) };
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

    TCPMiddleware mw = new TCPMiddleware();
    setListener(mw.new MiddlewareListenerImpl());

    try (ServerSocket serverSocket = new ServerSocket(s_serverPort);) {
      while (true) {
        Socket clientSocket = serverSocket.accept();
        listener.onNewConnection(clientSocket);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public static void setListener(MiddlewareListener listener) {
    TCPMiddleware.listener = listener;
  }

  class MiddlewareListenerImpl implements MiddlewareListener {

    private Socket flightSocket;
    private ObjectOutputStream f_oos;
    private ObjectInputStream f_ois;

    @Override
    public void onNewConnection(Socket clientSocket) {
      Runnable r = () -> {

        try (ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
            ) {
          System.out.println("Connected to client.");
          try {
            Socket flightSocket = new Socket(s_serverHosts[0], s_serverPorts[0]);
            ObjectOutputStream f_oos = new ObjectOutputStream(this.flightSocket.getOutputStream());
            ObjectInputStream f_ois = new ObjectInputStream(this.flightSocket.getInputStream());
            
          } catch (Exception e) {
            e.printStackTrace();
          }
          this.f_oos = f_oos;
          this.f_ois = f_ois;
          
          Object fromClient;
          while ((fromClient = (UserCommand) ois.readObject()) != null) {
            CompletableFuture future = CompletableFuture.supplyAsync(() -> {
              try {

                // unpackage object
                // send to respective RM
                this.f_oos.writeObject(new String("Hello"));
                // oos.writeObject(new Boolean(true));

                // should be response from RM
                return true;
              } catch (Exception e) {
                e.printStackTrace();
              }
              return false;
            }, executor);
            oos.writeObject(future.get());
          }
        } catch (EOFException e) {
          System.out.println("Connection closed.");
        } catch (Exception e) {
          e.printStackTrace();
        }
      };
      executor.execute(r);
    }
  }

}
