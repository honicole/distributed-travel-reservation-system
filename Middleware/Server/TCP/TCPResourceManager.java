// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.TCP;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import Server.Common.ResourceManager;

public class TCPResourceManager extends ResourceManager {
  private static int s_serverPort = 1099;
  private ServerSocket server;

  private Executor executor = Executors.newFixedThreadPool(8);
  private ResourceManagerListener listener;

  private TCPResourceManager() {
  }

  public TCPResourceManager(String name) {
    super(name);
  }

  public static void main(String[] args) {
    System.out.println("TCPResourceManager successfully called! :)");

    if (args.length > 0) {
      s_serverPort = Integer.valueOf(args[0]);
    }

    // Create and install a security manager
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new SecurityManager());
    }

    TCPResourceManager rm = new TCPResourceManager();
    rm.setListener(rm.new ResourceManagerListenerImpl());

    try (ServerSocket serverSocket = new ServerSocket(s_serverPort);) {
      while (true) {
        Socket clientSocket = serverSocket.accept();
        rm.listener.onNewConnection(clientSocket);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void setListener(ResourceManagerListener listener) {
    this.listener = listener;
  }

  class ResourceManagerListenerImpl implements ResourceManagerListener {

    @Override
    public void onNewConnection(Socket socket) {
      Runnable r = () -> {
        try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());) {
          System.out.println("Connected");
          Object fromClient;
          while ((fromClient = (String) ois.readObject()) != null) {
            CompletableFuture future = CompletableFuture.supplyAsync(() -> {
              try {
                // unpackage object

                // send to respective RM

                // oos.writeObject(new Boolean(true));

                // should be response from RM
                return true;
              } catch (Exception e) {
                e.printStackTrace();
              }
              return false;
            }, executor);
            // oos.writeObject(future.get());
            System.out.println(future.get());
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
