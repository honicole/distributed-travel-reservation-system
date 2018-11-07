package middleware;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import Client.Command;
import Client.UserCommand;
import Server.TCP.TCPResourceManager;
import exceptions.InvalidTransactionException;

public class TCPMiddleware extends Middleware {

  private static String[] s_serverHosts = new String[] { "localhost", "localhost", "localhost" };
  private static int s_serverPort = 1099;
  private static int[] s_serverPorts;
  private ServerSocket server;

  private static Executor executor = Executors.newFixedThreadPool(8);
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
    TM = new TransactionManager();
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

  class MiddlewareListenerImpl implements MiddlewareListener {

    private ObjectOutputStream f_oos;
    private ObjectInputStream f_ois;
    private ObjectOutputStream c_oos;
    private ObjectInputStream c_ois;
    private ObjectOutputStream r_oos;
    private ObjectInputStream r_ois;

    @Override
    public void onNewConnection(Socket clientSocket) {
      Runnable r = () -> {

        try (ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());) {
          System.out.println("Connected to client.");
          try {
            Socket flightSocket = new Socket(InetAddress.getByName(s_serverHosts[0]), s_serverPorts[0]);
            this.f_oos = new ObjectOutputStream(flightSocket.getOutputStream());
            this.f_ois = new ObjectInputStream(flightSocket.getInputStream());
            Socket carsSocket = new Socket(InetAddress.getByName(s_serverHosts[1]), s_serverPorts[1]);
            this.c_oos = new ObjectOutputStream(carsSocket.getOutputStream());
            this.c_ois = new ObjectInputStream(carsSocket.getInputStream());
            Socket roomsSocket = new Socket(InetAddress.getByName(s_serverHosts[2]), s_serverPorts[2]);
            this.r_oos = new ObjectOutputStream(roomsSocket.getOutputStream());
            this.r_ois = new ObjectInputStream(roomsSocket.getInputStream());

          } catch (Exception e) {
            e.printStackTrace();
          }

          final UserCommand[] fromClient = new UserCommand[1];
          while ((fromClient[0] = (UserCommand) ois.readObject()) != null) {
            CompletableFuture future = CompletableFuture.supplyAsync(() -> {
              Object result = null;
              try {
                final UserCommand req = fromClient[0];
                final Command cmd = req.getCommand();
                final String[] args = req.getArgs();
                int transactionId = -1;
                if (args.length > 1) {
                  transactionId = Integer.valueOf(args[1]);
                  switch (TM.getStatus(transactionId)) {
                  case ACTIVE:
                    TM.resetTimeToLive(transactionId);
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

                switch (cmd.name()) {
                case "AddFlight":
                case "DeleteFlight":
                case "QueryFlight":
                case "QueryFlightPrice":
                case "ReserveFlight":
                  TM.addResourceManager(transactionId, s_serverHosts[0]);
                  this.f_oos.writeObject(req);
                  result = this.f_ois.readObject();
                  break;
                case "AddCars":
                case "DeleteCars":
                case "QueryCars":
                case "QueryCarsPrice":
                case "ReserveCar":
                  TM.addResourceManager(transactionId, s_serverHosts[1]);
                  this.c_oos.writeObject(req);
                  result = this.c_ois.readObject();
                  break;
                case "AddRooms":
                case "DeleteRooms":
                case "QueryRooms":
                case "QueryRoomsPrice":
                case "ReserveRoom":
                  TM.addResourceManager(transactionId, s_serverHosts[2]);
                  this.r_oos.writeObject(req);
                  result = this.r_ois.readObject();
                  break;
                case "AddCustomer":
                  TM.addResourceManager(transactionId, s_serverHosts[0]);
                  TM.addResourceManager(transactionId, s_serverHosts[1]);
                  TM.addResourceManager(transactionId, s_serverHosts[2]);
                  this.f_oos.writeObject(req);
                  int id = (int) f_ois.readObject();
                  String[] args_with_id = Arrays.copyOf(args, args.length + 1);
                  args_with_id[args_with_id.length - 1] = Integer.toString(id);
                  UserCommand req_with_id = new UserCommand(Command.fromString("AddCustomerID"), args_with_id);
                  this.c_oos.writeObject(req_with_id);
                  this.r_oos.writeObject(req_with_id);
                  if ((Boolean) this.c_ois.readObject() && (Boolean) this.r_ois.readObject()) {
                    result = id;
                  } else {
                    result = -1;
                  }
                  break;
                case "AddCustomerID":
                case "DeleteCustomerID":
                  TM.addResourceManager(transactionId, s_serverHosts[0]);
                  TM.addResourceManager(transactionId, s_serverHosts[1]);
                  TM.addResourceManager(transactionId, s_serverHosts[2]);
                  this.f_oos.writeObject(req);
                  this.c_oos.writeObject(req);
                  this.r_oos.writeObject(req);
                  result = (Boolean) this.f_ois.readObject() && (Boolean) this.c_ois.readObject()
                      && (Boolean) this.r_ois.readObject();
                  break;
                case "QueryCustomer":
                  TM.addResourceManager(transactionId, s_serverHosts[0]);
                  TM.addResourceManager(transactionId, s_serverHosts[1]);
                  TM.addResourceManager(transactionId, s_serverHosts[2]);
                  this.f_oos.writeObject(req);
                  this.c_oos.writeObject(req);
                  this.r_oos.writeObject(req);
                  String flights_bill = (String) this.f_ois.readObject();
                  String cars_bill = (String) this.c_ois.readObject();
                  String rooms_bill = (String) this.r_ois.readObject();
                  String regex = "^Bill for customer [0-9]*\n";
                  result = String.join("", flights_bill, cars_bill.replaceFirst(regex, ""),
                      rooms_bill.replaceFirst(regex, ""));
                  break;
                case "Bundle":
                  TM.addResourceManager(transactionId, s_serverHosts[0]);
                  TM.addResourceManager(transactionId, s_serverHosts[1]);
                  TM.addResourceManager(transactionId, s_serverHosts[2]);
                  String xid = req.get(1);
                  String cid = req.get(2);
                  String location = req.get(args.length - 3);
                  boolean reserved = true;
                  for (int i = 3; i < args.length - 3; i++) {
                    UserCommand flights_bundle = new UserCommand(Command.fromString("ReserveFlight"),
                        new String[] { "ReserveFlight", xid, cid, req.get(i) });
                    this.f_oos.writeObject(flights_bundle);
                    reserved = reserved && (Boolean) this.f_ois.readObject();
                  }
                  if (Boolean.valueOf(req.get(args.length - 2))) {
                    UserCommand cars_bundle = new UserCommand(Command.fromString("ReserveCar"),
                        new String[] { "ReserveCar", xid, cid, location });
                    this.c_oos.writeObject(cars_bundle);
                    ;
                    reserved = reserved && (Boolean) this.c_ois.readObject();
                  }
                  if (Boolean.valueOf(req.get(args.length - 1))) {
                    UserCommand rooms_bundle = new UserCommand(Command.fromString("ReserveRoom"),
                        new String[] { "ReserveRoom", xid, cid, location });
                    this.r_oos.writeObject(rooms_bundle);
                    ;
                    reserved = reserved && (Boolean) this.r_ois.readObject();
                  }
                  result = reserved;
                  break;
                case "start":
                  result = (int) TM.start();
                  break;
                case "commit":
                  result = TM.commit(transactionId);
                  break;
                case "abort":
                  break;
                }
              } catch (Exception e) {
                e.printStackTrace();
              }
              return result;
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
