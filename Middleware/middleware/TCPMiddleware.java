package middleware;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import Client.Command;
import Client.UserCommand;
import Server.LockManager.DeadlockException;
import Server.TCP.TCPResourceManager;
import exceptions.InvalidTransactionException;

public class TCPMiddleware extends Middleware {

  private ServerSocket server;
  private static int s_serverPort = 1099;
  private static String[] s_serverHosts;
  private static int[] s_serverPorts;
  private Executor executor = Executors.newFixedThreadPool(8);
  private static MiddlewareListener listener;
  private static TransactionManager TM;

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
    TM = new TransactionManager(getListener());

    try (ServerSocket serverSocket = new ServerSocket(s_serverPort);) {
      TCPMiddleware middleware = new TCPMiddleware(args);
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

  public static MiddlewareListener getListener() {
    return TCPMiddleware.listener;
  }

  class MiddlewareListenerImpl implements MiddlewareListener {

    private Map<Socket, Map<String, ObjectOutputStream>> sockets_out = new HashMap<>();
    private Map<Socket, Map<String, ObjectInputStream>> sockets_in = new HashMap<>();

    public boolean commit(Socket clientSocket, int transactionId, String rm) {
      CompletableFuture future = CompletableFuture.supplyAsync(() -> {
        Object result = null;
        String[] cmd_args = new String[] { "commit", Integer.toString(transactionId) };
        UserCommand req = new UserCommand(Command.fromString(cmd_args[0]), cmd_args);
        try {
          sockets_out.get(clientSocket).get(rm).writeObject(req);
          result = sockets_in.get(clientSocket).get(rm).readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return result;
      }, executor);

      try {
        return (Boolean) future.get();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
      return false;
    }

    public void abort(Socket clientSocket, int transactionId, String rm) {
      CompletableFuture future = CompletableFuture.supplyAsync(() -> {
        String[] cmd_args = new String[] { "abort", Integer.toString(transactionId) };
        UserCommand req = new UserCommand(Command.fromString(cmd_args[0]), cmd_args);
        try {
          sockets_out.get(clientSocket).get(rm).writeObject(req);
        } catch (Exception e) {
          e.printStackTrace();
        }
        return true;
      }, executor);
    }

    @Override
    public void onNewConnection(Socket clientSocket) throws DeadlockException {
      Runnable r = () -> {
        try (ObjectOutputStream client_out = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream client_in = new ObjectInputStream(clientSocket.getInputStream());) {
          System.out.println("Connected to client.");
          try {
            Map<String, ObjectOutputStream> socket_1 = new HashMap<>();
            Map<String, ObjectInputStream> socket_2 = new HashMap<>();
            for (int i = 0; i < s_serverHosts.length; i++) {
              Socket socket = new Socket(InetAddress.getByName(s_serverHosts[i]), s_serverPorts[i]);
              socket_1.put(s_serverHosts[i], new ObjectOutputStream(socket.getOutputStream()));
              socket_2.put(s_serverHosts[i], new ObjectInputStream(socket.getInputStream()));
            }
            sockets_out.put(clientSocket, socket_1);
            sockets_in.put(clientSocket, socket_2);
          } catch (Exception e) {
            e.printStackTrace();
          }

          final UserCommand[] client_command = new UserCommand[1];
          while ((client_command[0] = (UserCommand) client_in.readObject()) != null) {
            CompletableFuture future = CompletableFuture.supplyAsync(() -> {
              Object result = null;
              int transactionId = -1;
              try {
                final UserCommand req = client_command[0];
                final Command cmd = req.getCommand();
                final String[] args = req.getArgs();

                if (args.length > 1) {
                  transactionId = Integer.valueOf(args[1]);
                  switch (TM.getStatus(transactionId)) {
                  case ACTIVE:
                    TM.resetTimeToLive(clientSocket, transactionId);
                    break;
                  case COMMITTED:
                    throw new InvalidTransactionException("The transaction was committed.");
                  case ABORTED:
                    throw new InvalidTransactionException("The transaction was aborted");
                  case TIME_OUT:
                    throw new InvalidTransactionException("The transaction timed out.");
                  case INVALID:
                    throw new InvalidTransactionException("The transaction does not exist");
                  default:
                    throw new InvalidTransactionException("Invalid transaction.");
                  }
                }

                String server;
                boolean success = true;
                switch (cmd.name()) {
                case "AddFlight":
                case "DeleteFlight":
                case "QueryFlight":
                case "QueryFlightPrice":
                case "ReserveFlight":
                  server = s_serverHosts[0];
                  TM.addResourceManager(transactionId, server);
                  sockets_out.get(clientSocket).get(server).writeObject(req);
                  result = sockets_in.get(clientSocket).get(server).readObject();
                  break;
                case "AddCars":
                case "DeleteCars":
                case "QueryCars":
                case "QueryCarsPrice":
                case "ReserveCar":
                  server = s_serverHosts[1];
                  TM.addResourceManager(transactionId, server);
                  sockets_out.get(clientSocket).get(server).writeObject(req);
                  result = sockets_in.get(clientSocket).get(server).readObject();
                  break;
                case "AddRooms":
                case "DeleteRooms":
                case "QueryRooms":
                case "QueryRoomsPrice":
                case "ReserveRoom":
                  server = s_serverHosts[2];
                  TM.addResourceManager(transactionId, server);
                  sockets_out.get(clientSocket).get(server).writeObject(req);
                  result = sockets_in.get(clientSocket).get(server).readObject();
                  break;
                case "AddCustomer":
                  int id = -1;
                  String[] args_with_id;
                  UserCommand req_with_id = null;
                  for (int i = 0; i < s_serverHosts.length; i++) {
                    server = s_serverHosts[i];
                    TM.addResourceManager(transactionId, server);
                    if (i == 0) {
                      // If first server, generate customer ID and repackage for subsequent servers
                      sockets_out.get(clientSocket).get(server).writeObject(req);
                      id = (int) sockets_in.get(clientSocket).get(server).readObject();
                      args_with_id = Arrays.copyOf(args, args.length + 1);
                      args_with_id[args_with_id.length - 1] = Integer.toString(id);
                      req_with_id = new UserCommand(Command.fromString("AddCustomerID"), args_with_id);
                    } else {
                      sockets_out.get(clientSocket).get(server).writeObject(req_with_id);
                      success &= (Boolean) sockets_in.get(clientSocket).get(server).readObject();
                    }
                  }
                  result = success ? id : -1;
                  break;
                case "AddCustomerID":
                case "DeleteCustomerID":
                  for (String s : s_serverHosts) {
                    TM.addResourceManager(transactionId, s);
                    sockets_out.get(clientSocket).get(s).writeObject(req);
                    success &= (Boolean) sockets_in.get(clientSocket).get(s).readObject();
                  }
                  result = success;
                  break;
                case "QueryCustomer":
                  String customer_bill = "";
                  String regex = "^Bill for customer [0-9]*\n";
                  for (int i = 0; i < s_serverHosts.length; i++) {
                    server = s_serverHosts[i];
                    TM.addResourceManager(transactionId, server);
                    sockets_out.get(clientSocket).get(server).writeObject(req);
                    String bill = (String) sockets_in.get(clientSocket).get(server).readObject();
                    if (i == 0) {
                      customer_bill = bill;
                    } else {
                      customer_bill += String.join("", bill.replaceFirst(regex, ""));
                    }
                  }
                  result = customer_bill;
                  break;
                case "Bundle":
                  // TODO: check that all are available before reserving
                  String xid = req.get(1);
                  String cid = req.get(2);
                  String location = req.get(args.length - 3);
                  boolean reserved = true;
                  String[] bundle_args;
                  UserCommand command;
                  server = s_serverHosts[0];
                  TM.addResourceManager(transactionId, server);
                  for (int i = 3; i < args.length - 3; i++) {
                    bundle_args = new String[] { "ReserveFlight", xid, cid, req.get(i) };
                    command = new UserCommand(Command.fromString(bundle_args[0]), bundle_args);
                    sockets_out.get(clientSocket).get(server).writeObject(command);
                    reserved &= (Boolean) sockets_in.get(clientSocket).get(server).readObject();
                  }

                  if (Boolean.valueOf(req.get(args.length - 2))) {
                    server = s_serverHosts[1];
                    TM.addResourceManager(transactionId, server);
                    bundle_args = new String[] { "ReserveCar", xid, cid, location };
                    command = new UserCommand(Command.fromString(bundle_args[0]), bundle_args);
                    sockets_out.get(clientSocket).get(server).writeObject(command);
                    reserved &= (Boolean) sockets_in.get(clientSocket).get(server).readObject();
                  }

                  if (Boolean.valueOf(req.get(args.length - 1))) {
                    server = s_serverHosts[2];
                    TM.addResourceManager(transactionId, server);
                    bundle_args = new String[] { "ReserveRoom", xid, cid, location };
                    command = new UserCommand(Command.fromString(bundle_args[0]), bundle_args);
                    sockets_out.get(clientSocket).get(server).writeObject(command);
                    reserved &= (Boolean) sockets_in.get(clientSocket).get(server).readObject();
                  }
                  result = reserved;
                  break;
                case "start":
                  result = (int) TM.start();
                  break;
                case "commit":
                  result = TM.commit(clientSocket, transactionId);
                  break;
                case "abort":
                  result = TM.abort(clientSocket, transactionId);
                  break;
                }
              } catch (Exception e) {
                e.printStackTrace();
              }
              return result;
            }, executor);
            Object result = future.get();
            if (result instanceof DeadlockException) {
              TM.abort(clientSocket, ((DeadlockException) result).getXid());
            }
            client_out.writeObject(result);
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
