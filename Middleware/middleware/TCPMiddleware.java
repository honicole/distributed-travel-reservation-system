package middleware;

import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import Client.Command;
import static Client.Command.*;
import Client.UserCommand;
import Server.LockManager.LockManager;
import Server.LockManager.TransactionLockObject;
import Server.Common.Trace;
import Server.LockManager.DeadlockException;
import exceptions.InvalidTransactionException;

public class TCPMiddleware extends Middleware {
  private ServerSocket server;
  private static int s_serverPort = 1099;
  private static String[] s_serverHosts;
  private static int[] s_serverPorts;
  private Socket s_socket;
  private Executor executor = Executors.newFixedThreadPool(8);
  private static MiddlewareListener listener;
  private static TransactionManager TM;

  /**
   * Set this to {@code true} only when performing performance analysis
   */
  private static final boolean LOG_PERFORMANCE = false;
  private static final String FILENAME = "./log.txt";
  private static File logFile = new File(FILENAME);
  private static StringBuilder log = new StringBuilder();
  private static int counter = 0;
  private static LockManager lockManager;

  private TCPMiddleware() {
  }

  public TCPMiddleware(String[] args) throws Exception {
    try {
      this.server = new ServerSocket(Integer.valueOf(args[0]), 1, InetAddress.getLocalHost());
    } catch (NumberFormatException | IOException e) {
      // e.printStackTrace();
    }
    s_serverHosts = new String[] { args[1], args[3], args[5] };
    s_serverPorts = new int[] { Integer.valueOf(args[2]), Integer.valueOf(args[4]), Integer.valueOf(args[6]) };
    lockManager = new LockManager();
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

    if (LOG_PERFORMANCE) {
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

    private Map<Socket, Map<Socket, ObjectOutputStream>> sockets_out = new HashMap<>();
    private Map<Socket, Map<Socket, ObjectInputStream>> sockets_in = new HashMap<>();

    public boolean prepare(Socket clientSocket, int transactionId, String rm) {
      boolean prepare_to_commit = true;

      for (int i = 0; i < 5; i++) {
        CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
          Object result = null;
          String[] cmd_args = new String[] { "prepare", Integer.toString(transactionId) };
          UserCommand req = new UserCommand(Command.fromString(cmd_args[0]), cmd_args);
          try {
            for (Socket socket : sockets_out.get(clientSocket).keySet()) {
              if (socket.getInetAddress().toString().equals(rm)) {
                s_socket = socket;
              }
            }
            sockets_out.get(clientSocket).get(s_socket).writeObject(req);
            result = sockets_in.get(clientSocket).get(s_socket).readObject();
          } catch (SocketException | EOFException e) {
            reconnect(clientSocket);
            return false;
          } catch (Exception e) {
            e.printStackTrace();
          }
          return result;
        }, executor);

        try {
          prepare_to_commit |= (Boolean) future.get(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        } catch (TimeoutException e) {
          prepare_to_commit |= false;
        }

        if (prepare_to_commit)
          break;
      }

      return prepare_to_commit;
    }

    public boolean commit(Socket clientSocket, int transactionId, String rm) {
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        Object result = null;
        String[] cmd_args = new String[] { "commit", Integer.toString(transactionId) };
        UserCommand req = new UserCommand(Command.fromString(cmd_args[0]), cmd_args);
        try {
          for (Socket socket : sockets_out.get(clientSocket).keySet()) {
            if (socket.getInetAddress().toString().equals(rm)) {
              s_socket = socket;
            }
          }
          sockets_out.get(clientSocket).get(s_socket).writeObject(req);
          result = sockets_in.get(clientSocket).get(s_socket).readObject();
        } catch (SocketException | EOFException e) {
          reconnect(clientSocket);
          return false;
        } catch (Exception e) {
          e.printStackTrace();
        }
        return result;
      }, executor);

      try {
        boolean isCommitted = (Boolean) future.get();
        if (isCommitted) {
          lockManager.UnlockAll(transactionId);
          return isCommitted;
        }
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
      return false;
    }

    public void abort(Socket clientSocket, int transactionId, String rm) {
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        String[] cmd_args = new String[] { "abort", Integer.toString(transactionId) };
        UserCommand req = new UserCommand(Command.fromString(cmd_args[0]), cmd_args);
        try {
          for (Socket socket : sockets_out.get(clientSocket).keySet()) {
            if (socket.getInetAddress().toString().equals(rm)) {
              s_socket = socket;
            }
          }
          sockets_out.get(clientSocket).get(s_socket).writeObject(req);
        } catch (SocketException | EOFException e) {
          reconnect(clientSocket);
          return false;
        } catch (Exception e) {
          e.printStackTrace();
        }
        lockManager.UnlockAll(transactionId);
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
            Map<Socket, ObjectOutputStream> socket_1 = new HashMap<>();
            Map<Socket, ObjectInputStream> socket_2 = new HashMap<>();
            for (int i = 0; i < s_serverHosts.length; i++) {
              Socket socket = new Socket(InetAddress.getByName(s_serverHosts[i]), s_serverPorts[i]);
              socket_1.put(socket, new ObjectOutputStream(socket.getOutputStream()));
              socket_2.put(socket, new ObjectInputStream(socket.getInputStream()));
            }
            sockets_out.put(clientSocket, socket_1);
            sockets_in.put(clientSocket, socket_2);
          } catch (Exception e) {
            e.printStackTrace();
          }

          final UserCommand[] client_command = new UserCommand[1];
          while ((client_command[0] = (UserCommand) client_in.readObject()) != null) {
            long start = System.currentTimeMillis();
            CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
              Object result = null;
              int transactionId = -1;
              try {
                final UserCommand req = client_command[0];
                final Command cmd = req.getCommand();
                final String[] args = req.getArgs();

                if (args.length > 1 && !isCrashCommand(cmd)) {
                  transactionId = Integer.valueOf(args[1]);
                  Exception e;
                  switch (TM.getStatus(transactionId)) {
                  case ACTIVE:
                    TM.resetTimeToLive(clientSocket, transactionId);
                    break;
                  case COMMITTED:
                    e = new InvalidTransactionException("The transaction was already committed");
                    result = e;
                    throw e;
                  case ABORTED:
                    e = new InvalidTransactionException("The transaction was already aborted");
                    result = e;
                    throw e;
                  case TIME_OUT:
                    e = new InvalidTransactionException("The transaction timed out");
                    result = e;
                    throw e;
                  case INVALID:
                    e = new InvalidTransactionException("The transaction does not exist");
                    result = e;
                    throw e;
                  default:
                    e = new InvalidTransactionException("Invalid transaction");
                    result = e;
                    throw e;
                  }
                }

                boolean success = true;
                boolean first = true;
                switch (cmd.name()) {
                case "ReserveFlight":
                  lockManager.Lock(transactionId, args[2], TransactionLockObject.LockType.LOCK_WRITE);
                case "AddFlight":
                case "DeleteFlight":
                case "QueryFlight":
                case "QueryFlightPrice":
                  for (Socket socket : sockets_out.get(clientSocket).keySet()) {
                    if (socket.getInetAddress() == InetAddress.getByName(s_serverHosts[0]))
                      s_socket = socket;
                  }
                  TM.addResourceManager(transactionId, s_socket.getInetAddress().toString());
                  sockets_out.get(clientSocket).get(s_socket).writeObject(req);
                  result = sockets_in.get(clientSocket).get(s_socket).readObject();
                  break;
                case "ReserveCar":
                  lockManager.Lock(transactionId, args[2], TransactionLockObject.LockType.LOCK_WRITE);
                case "AddCars":
                case "DeleteCars":
                case "QueryCars":
                case "QueryCarsPrice":
                  for (Socket socket : sockets_out.get(clientSocket).keySet()) {
                    if (socket.getInetAddress() == InetAddress.getByName(s_serverHosts[1]))
                      s_socket = socket;
                  }
                  TM.addResourceManager(transactionId, s_socket.getInetAddress().toString());
                  sockets_out.get(clientSocket).get(s_socket).writeObject(req);
                  result = sockets_in.get(clientSocket).get(s_socket).readObject();
                  break;
                case "ReserveRoom":
                  lockManager.Lock(transactionId, args[2], TransactionLockObject.LockType.LOCK_WRITE);
                case "AddRooms":
                case "DeleteRooms":
                case "QueryRooms":
                case "QueryRoomsPrice":
                  for (Socket socket : sockets_out.get(clientSocket).keySet()) {
                    if (socket.getInetAddress() == InetAddress.getByName(s_serverHosts[2]))
                      s_socket = socket;
                  }
                  TM.addResourceManager(transactionId, s_socket.getInetAddress().toString());
                  sockets_out.get(clientSocket).get(s_socket).writeObject(req);
                  result = sockets_in.get(clientSocket).get(s_socket).readObject();
                  break;
                case "AddCustomer":
                  int id = -1;
                  String[] args_with_id;
                  UserCommand req_with_id = null;
                  first = true;
                  for (Socket socket : sockets_out.get(clientSocket).keySet()) {
                    s_socket = socket;
                    TM.addResourceManager(transactionId, s_socket.getInetAddress().toString());
                    if (first) {
                      // If first server, generate customer ID and repackage for subsequent servers
                      sockets_out.get(clientSocket).get(s_socket).writeObject(req);
                      result = sockets_in.get(clientSocket).get(s_socket).readObject();
                      id = (int) result;
                      args_with_id = Arrays.copyOf(args, args.length + 1);
                      args_with_id[args_with_id.length - 1] = Integer.toString(id);
                      req_with_id = new UserCommand(Command.fromString("AddCustomerID"), args_with_id);
                      first = false;
                    } else {
                      sockets_out.get(clientSocket).get(s_socket).writeObject(req_with_id);
                      success &= (Boolean) sockets_in.get(clientSocket).get(s_socket).readObject();
                    }
                  }
                  result = success ? id : -1;
                  break;
                case "AddCustomerID":
                case "DeleteCustomer":
                  lockManager.Lock(transactionId, args[2], TransactionLockObject.LockType.LOCK_WRITE);
                  for (Socket socket : sockets_out.get(clientSocket).keySet()) {
                    s_socket = socket;
                    TM.addResourceManager(transactionId, s_socket.getInetAddress().toString());
                    sockets_out.get(clientSocket).get(s_socket).writeObject(req);
                    success &= (Boolean) sockets_in.get(clientSocket).get(s_socket).readObject();
                  }
                  result = success;
                  break;
                case "QueryCustomer":
                  lockManager.Lock(transactionId, args[2], TransactionLockObject.LockType.LOCK_READ);
                  String customer_bill = "";
                  String regex = "^Bill for customer [0-9]*\n";
                  first = true;
                  for (Socket socket : sockets_out.get(clientSocket).keySet()) {
                    s_socket = socket;
                    TM.addResourceManager(transactionId, s_socket.getInetAddress().toString());
                    sockets_out.get(clientSocket).get(s_socket).writeObject(req);
                    String bill = (String) sockets_in.get(clientSocket).get(s_socket).readObject();
                    if (first) {
                      customer_bill = bill;
                      first = false;
                    } else {
                      customer_bill += String.join("", bill.replaceFirst(regex, ""));
                    }
                  }
                  result = customer_bill;
                  break;
//                case "Bundle":
//                  // TODO: check that all are available before reserving
//
//                  /**
//                   * try to get all the locks on RM level, eg flight id
//                   * 
//                   * if false: abort, then release
//                   */
//                  lockManager.Lock(transactionId, args[2], TransactionLockObject.LockType.LOCK_WRITE);
//                  String xid = req.get(1);
//                  String cid = req.get(2);
//                  String location = req.get(args.length - 3);
//                  boolean reserved = true;
//                  String[] bundle_args;
//                  UserCommand command;
//                  server = s_serverHosts[0];
//                  TM.addResourceManager(transactionId, server);
//                  for (int i = 3; i < args.length - 3; i++) {
//                    bundle_args = new String[] { "ReserveFlight", xid, cid, req.get(i) };
//                    command = new UserCommand(Command.fromString(bundle_args[0]), bundle_args);
//                    sockets_out.get(clientSocket).get(server).writeObject(command);
//                    reserved &= (Boolean) sockets_in.get(clientSocket).get(server).readObject();
//                  }
//
//                  if (Boolean.valueOf(req.get(args.length - 2))) {
//                    server = s_serverHosts[1];
//                    TM.addResourceManager(transactionId, server);
//                    bundle_args = new String[] { "ReserveCar", xid, cid, location };
//                    command = new UserCommand(Command.fromString(bundle_args[0]), bundle_args);
//                    sockets_out.get(clientSocket).get(server).writeObject(command);
//                    reserved &= (Boolean) sockets_in.get(clientSocket).get(server).readObject();
//                  }
//
//                  if (Boolean.valueOf(req.get(args.length - 1))) {
//                    server = s_serverHosts[2];
//                    TM.addResourceManager(transactionId, server);
//                    bundle_args = new String[] { "ReserveRoom", xid, cid, location };
//                    command = new UserCommand(Command.fromString(bundle_args[0]), bundle_args);
//                    sockets_out.get(clientSocket).get(server).writeObject(command);
//                    reserved &= (Boolean) sockets_in.get(clientSocket).get(server).readObject();
//                  }
//                  result = reserved;
//                  break;
                case "start":
                  result = (int) TM.start();
                  break;
                case "commit":
                  result = TM.prepare(clientSocket, transactionId);
                  break;
                case "abort":
                  result = TM.abort(clientSocket, transactionId);
                  break;
                case "crashMiddleware":
                  result = TM.setCrashMode(Integer.valueOf(args[1]));
                  break;
                case "crashResourceManager":
                  for (Socket s : sockets_out.get(clientSocket).keySet()) {
                    sockets_out.get(clientSocket).get(s).writeObject(req);
                    success &= (Boolean) sockets_in.get(clientSocket).get(s).readObject();
                  }
                  result = success;
                  break;
                case "resetCrashes":
                  success &= TM.resetCrashes();
                  for (Socket s : sockets_out.get(clientSocket).keySet()) {
                    Trace.info("Resetting " + s.getPort());
                    sockets_out.get(clientSocket).get(s).writeObject(req);
                    success &= (Boolean) sockets_in.get(clientSocket).get(s).readObject();
                  }
                  result = success;
                  break;
                }
                if (LOG_PERFORMANCE) {
                  log.append(counter + "," + req.get(0) + "," + (System.currentTimeMillis() - start) + "\n");
                }
              } catch (DeadlockException e) {
                result = e;
              } catch (InvalidTransactionException e) {
              } catch (SocketException | EOFException e) {
                reconnect(clientSocket);
                return false;
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

    public void reconnect(Socket clientSocket) {
      System.out.println("Connection lost. Reconnecting...");
      
      CompletableFuture<?> reconnect = CompletableFuture.supplyAsync(() -> {
        Socket s = null;
        do {
          try {
            s = new Socket(s_socket.getInetAddress(), s_socket.getPort());
            sockets_out.get(clientSocket).put(s, new ObjectOutputStream(s.getOutputStream()));
            sockets_in.get(clientSocket).put(s, new ObjectInputStream(s.getInputStream()));
            sockets_out.get(clientSocket).remove(s_socket);
            sockets_in.get(clientSocket).remove(s_socket);
          } catch (IOException e1) {
            continue; // go again
          }
        } while (s == null || !s.isConnected());
        return true;
      }, executor).thenRun(() -> System.out.println("Reconnected."));
    }
  }

}
