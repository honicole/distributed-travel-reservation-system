package Server.TCP;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import Client.Command;
import Client.UserCommand;
import Server.Common.ResourceManager;
import Server.LockManager.DeadlockException;

public class TCPResourceManager extends ResourceManager {
  private static int s_serverPort = 1099;
  private ServerSocket server;

  private Executor executor = Executors.newFixedThreadPool(8);
  private ResourceManagerListener listener;

  /**
   * Set this to {@code true} only when performing performance analysis
   */
  private static final boolean LOG_PERFORMANCE = false;
  private static String FILENAME;
  private static File logFile;
  private static StringBuilder log = new StringBuilder();
  private static int counter = 0;

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

    if (LOG_PERFORMANCE) {
      String timestamp = Long.toString(System.currentTimeMillis());
      FILENAME = "log-" + timestamp + ".txt";
      logFile = new File(FILENAME);
      if (!logFile.exists()) {
        try {
          logFile.createNewFile();
        } catch (IOException e) {
        }
      }

      // Write log to disk on Ctrl-C
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          BufferedWriter writer = new BufferedWriter(new FileWriter(FILENAME));
          writer.write(log.toString());
          writer.close();
        } catch (Exception e) {
        }
      }));
    }

    TCPResourceManager rm = new TCPResourceManager(args[0]);
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
    public void onNewConnection(Socket socket) throws DeadlockException {
      Runnable r = () -> {
        try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());) {
          System.out.println("Connected");

          final UserCommand[] fromClient = new UserCommand[1];
          while ((fromClient[0] = (UserCommand) ois.readObject()) != null) {
            long start = System.currentTimeMillis();
            CompletableFuture future = CompletableFuture.supplyAsync(() -> {
              try {
                final UserCommand req = fromClient[0];
                final Command cmd = req.getCommand();
                final String[] args = req.getArgs();

                switch (cmd.name()) {
                case "AddFlight":
                  oos.writeObject(new Boolean(addFlight(Integer.valueOf(args[1]), Integer.valueOf(args[2]),
                      Integer.valueOf(args[3]), Integer.valueOf(args[4]))));
                  break;
                case "DeleteFlight":
                  oos.writeObject(new Boolean(deleteFlight(Integer.valueOf(args[1]), Integer.valueOf(args[2]))));
                  break;
                case "QueryFlight":
                  oos.writeObject(new Integer(queryFlight(Integer.valueOf(args[1]), Integer.valueOf(args[2]))));
                  break;
                case "QueryFlightPrice":
                  oos.writeObject(new Integer(queryFlightPrice(Integer.valueOf(args[1]), Integer.valueOf(args[2]))));
                  break;
                case "ReserveFlight":
                  oos.writeObject(new Boolean(
                      reserveFlight(Integer.valueOf(args[1]), Integer.valueOf(args[2]), Integer.valueOf(args[3]))));
                  break;
                case "AddCars":
                  oos.writeObject(new Boolean(
                      addCars(Integer.valueOf(args[1]), args[2], Integer.valueOf(args[3]), Integer.valueOf(args[4]))));
                  break;
                case "DeleteCars":
                  oos.writeObject(new Boolean(deleteCars(Integer.valueOf(args[1]), args[2])));
                  break;
                case "QueryCars":
                  oos.writeObject(new Integer(queryCars(Integer.valueOf(args[1]), args[2])));
                  break;
                case "QueryCarsPrice":
                  oos.writeObject(new Integer(queryCarsPrice(Integer.valueOf(args[1]), args[2])));
                  break;
                case "ReserveCar":
                  oos.writeObject(new Boolean(reserveCar(Integer.valueOf(args[1]), Integer.valueOf(args[2]), args[3])));
                  break;
                case "AddRooms":
                  oos.writeObject(new Boolean(
                      addRooms(Integer.valueOf(args[1]), args[2], Integer.valueOf(args[3]), Integer.valueOf(args[4]))));
                  break;
                case "DeleteRooms":
                  oos.writeObject(new Boolean(deleteRooms(Integer.valueOf(args[1]), args[2])));
                  break;
                case "QueryRooms":
                  oos.writeObject(new Integer(queryRooms(Integer.valueOf(args[1]), args[2])));
                  break;
                case "QueryRoomsPrice":
                  oos.writeObject(new Integer(queryRoomsPrice(Integer.valueOf(args[1]), args[2])));
                  break;
                case "ReserveRoom":
                  oos.writeObject(
                      new Boolean(reserveRoom(Integer.valueOf(args[1]), Integer.valueOf(args[2]), args[3])));
                  break;
                case "AddCustomer":
                  oos.writeObject(new Integer(newCustomer(Integer.valueOf(args[1]))));
                  break;
                case "AddCustomerID":
                  oos.writeObject(new Boolean(newCustomer(Integer.valueOf(args[1]), Integer.valueOf(args[2]))));
                  break;
                case "DeleteCustomer":
                  oos.writeObject(new Boolean(deleteCustomer(Integer.valueOf(args[1]), Integer.valueOf(args[2]))));
                  break;
                case "QueryCustomer":
                  oos.writeObject(new String(queryCustomerInfo(Integer.valueOf(args[1]), Integer.valueOf(args[2]))));
                  break;
                case "prepare":
                  oos.writeObject(new Boolean(prepare(Integer.valueOf(args[1]))));
                  break;
                case "commit":
                  oos.writeObject(new Boolean(commit(Integer.valueOf(args[1]))));
                  break;
                case "abort":
                  abort(Integer.valueOf(args[1]));
                  break;
                case "shutdown":
                  shutdown();
                  break;
                }

                if (LOG_PERFORMANCE) {
                  log.append(counter + "," + args[1] + "," + (System.currentTimeMillis() - start) + "\n");
                }

                return true;
              } catch (DeadlockException e) {
                try {
                  oos.writeObject(e);
                } catch (IOException e1) {
                  e1.printStackTrace();
                }
                e.printStackTrace();

                if (LOG_PERFORMANCE) {
                  log.append(counter + ",L," + (System.currentTimeMillis() - start) + "\n");
                }
              } catch (Exception e) {
                e.printStackTrace();

                if (LOG_PERFORMANCE) {
                  log.append(counter + ",F," + (System.currentTimeMillis() - start) + "\n");
                }
              } finally {
                counter++;
              }
              return false;
            }, executor);
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
