package middleware;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import Server.TCP.TCPResourceManager;

public class TCPMiddleware extends Middleware {

  private static String[] s_serverHosts = new String[] { "localhost", "localhost", "localhost" };
  private static int s_serverPort = 1099;
  private String[] serverNames;
  private ServerSocket server;

  private static Executor executor = Executors.newFixedThreadPool(8);
  private static MiddlewareListener listener;

  private TCPMiddleware() {}
  
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

    @Override
    public void onNewConnection(Socket socket) {
      Runnable r = () -> {
        // package command

        // write object to server socket

        // get a response

        // write to console

        try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());) {

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
        } catch (Exception e) {
          e.printStackTrace();
        }
      };
      executor.execute(r);
    }
  }

}
