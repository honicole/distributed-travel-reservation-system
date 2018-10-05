package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TCPClient extends Client {
  private static String s_serverHost = "localhost";
  private static int s_serverPort = 1099;

  private static Executor executor = Executors.newFixedThreadPool(8);
  private Socket socket;
  private UserCommandListener listener;

  // These are set in the try
  private static Socket serverSocket;
  private ObjectOutputStream oos;
  private ObjectInputStream ois;

  private TCPClient() {
  }
  
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

    TCPClient tcpClient = new TCPClient();
    tcpClient.runCommand();
    
    // Set the security policy
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new SecurityManager());
    }

  }

  
  public void runCommand() {
    // Prepare for reading commands
    System.out.println();
    System.out.println("Location \"help\" for list of supported commands");
    
    try (Socket serverSocket = new Socket(s_serverHost, s_serverPort);
        ObjectOutputStream oos = new ObjectOutputStream(serverSocket.getOutputStream());
        ObjectInputStream ois = new ObjectInputStream(serverSocket.getInputStream());) {
      
      BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
      TCPClient client = new TCPClient(serverSocket);
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
  
 public void execute(Command cmd, Vector<String> arguments) throws RemoteException, NumberFormatException {
    switch (cmd) {
    case Help: {
      if (arguments.size() == 1) {
        System.out.println(Command.description());
      } else if (arguments.size() == 2) {
        Command l_cmd = Command.fromString(arguments.elementAt(1));
        System.out.println(l_cmd.toString());
      } else {
        System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27
            + "[0mImproper use of help command. Location \"help\" or \"help,<CommandName>\"");
      }
      break;
    }
    case AddFlight: {
      checkArgumentsCount(5, arguments.size());

      System.out.println("Adding a new flight [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Flight Number: " + arguments.elementAt(2));
      System.out.println("-Flight Seats: " + arguments.elementAt(3));
      System.out.println("-Flight Price: " + arguments.elementAt(4));

      int id = toInt(arguments.elementAt(1));
      int flightNum = toInt(arguments.elementAt(2));
      int flightSeats = toInt(arguments.elementAt(3));
      int flightPrice = toInt(arguments.elementAt(4));

      if (m_resourceManager.addFlight(id, flightNum, flightSeats, flightPrice)) {
        System.out.println("Flight added");
      } else {
        System.out.println("Flight could not be added");
      }
      break;
    }
    case AddCars: {
      checkArgumentsCount(5, arguments.size());

      System.out.println("Adding new cars [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Car Location: " + arguments.elementAt(2));
      System.out.println("-Number of Cars: " + arguments.elementAt(3));
      System.out.println("-Car Price: " + arguments.elementAt(4));

      int id = toInt(arguments.elementAt(1));
      String location = arguments.elementAt(2);
      int numCars = toInt(arguments.elementAt(3));
      int price = toInt(arguments.elementAt(4));

      if (m_resourceManager.addCars(id, location, numCars, price)) {
        System.out.println("Cars added");
      } else {
        System.out.println("Cars could not be added");
      }
      break;
    }
    case AddRooms: {
      checkArgumentsCount(5, arguments.size());

      System.out.println("Adding new rooms [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Room Location: " + arguments.elementAt(2));
      System.out.println("-Number of Rooms: " + arguments.elementAt(3));
      System.out.println("-Room Price: " + arguments.elementAt(4));

      int id = toInt(arguments.elementAt(1));
      String location = arguments.elementAt(2);
      int numRooms = toInt(arguments.elementAt(3));
      int price = toInt(arguments.elementAt(4));

      if (m_resourceManager.addRooms(id, location, numRooms, price)) {
        System.out.println("Rooms added");
      } else {
        System.out.println("Rooms could not be added");
      }
      break;
    }
    case AddCustomer: {
      checkArgumentsCount(2, arguments.size());

      System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");

      int id = toInt(arguments.elementAt(1));
      int customer = m_resourceManager.newCustomer(id);

      System.out.println("Add customer ID: " + customer);
      break;
    }
    case AddCustomerID: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Customer ID: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      int customerID = toInt(arguments.elementAt(2));

      if (m_resourceManager.newCustomer(id, customerID)) {
        System.out.println("Add customer ID: " + customerID);
      } else {
        System.out.println("Customer could not be added");
      }
      break;
    }
    case DeleteFlight: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Deleting a flight [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Flight Number: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      int flightNum = toInt(arguments.elementAt(2));

      if (m_resourceManager.deleteFlight(id, flightNum)) {
        System.out.println("Flight Deleted");
      } else {
        System.out.println("Flight could not be deleted");
      }
      break;
    }
    case DeleteCars: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Deleting all cars at a particular location [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Car Location: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      String location = arguments.elementAt(2);

      if (m_resourceManager.deleteCars(id, location)) {
        System.out.println("Cars Deleted");
      } else {
        System.out.println("Cars could not be deleted");
      }
      break;
    }
    case DeleteRooms: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Deleting all rooms at a particular location [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Car Location: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      String location = arguments.elementAt(2);

      if (m_resourceManager.deleteRooms(id, location)) {
        System.out.println("Rooms Deleted");
      } else {
        System.out.println("Rooms could not be deleted");
      }
      break;
    }
    case DeleteCustomer: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Deleting a customer from the database [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Customer ID: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      int customerID = toInt(arguments.elementAt(2));

      if (m_resourceManager.deleteCustomer(id, customerID)) {
        System.out.println("Customer Deleted");
      } else {
        System.out.println("Customer could not be deleted");
      }
      break;
    }
    case QueryFlight: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying a flight [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Flight Number: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      int flightNum = toInt(arguments.elementAt(2));

      int seats = m_resourceManager.queryFlight(id, flightNum);
      System.out.println("Number of seats available: " + seats);
      break;
    }
    case QueryCars: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying cars location [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Car Location: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      String location = arguments.elementAt(2);

      int numCars = m_resourceManager.queryCars(id, location);
      System.out.println("Number of cars at this location: " + numCars);
      break;
    }
    case QueryRooms: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying rooms location [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Room Location: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      String location = arguments.elementAt(2);

      int numRoom = m_resourceManager.queryRooms(id, location);
      System.out.println("Number of rooms at this location: " + numRoom);
      break;
    }
    case QueryCustomer: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying customer information [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Customer ID: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      int customerID = toInt(arguments.elementAt(2));

      String bill = m_resourceManager.queryCustomerInfo(id, customerID);
      System.out.print(bill);
      break;
    }
    case QueryFlightPrice: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying a flight price [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Flight Number: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      int flightNum = toInt(arguments.elementAt(2));

      int price = m_resourceManager.queryFlightPrice(id, flightNum);
      System.out.println("Price of a seat: " + price);
      break;
    }
    case QueryCarsPrice: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying cars price [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Car Location: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      String location = arguments.elementAt(2);

      int price = m_resourceManager.queryCarsPrice(id, location);
      System.out.println("Price of cars at this location: " + price);
      break;
    }
    case QueryRoomsPrice: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying rooms price [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Room Location: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      String location = arguments.elementAt(2);

      int price = m_resourceManager.queryRoomsPrice(id, location);
      System.out.println("Price of rooms at this location: " + price);
      break;
    }
    case ReserveFlight: {
      checkArgumentsCount(4, arguments.size());

      System.out.println("Reserving seat in a flight [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Customer ID: " + arguments.elementAt(2));
      System.out.println("-Flight Number: " + arguments.elementAt(3));

      int id = toInt(arguments.elementAt(1));
      int customerID = toInt(arguments.elementAt(2));
      int flightNum = toInt(arguments.elementAt(3));

      if (m_resourceManager.reserveFlight(id, customerID, flightNum)) {
        System.out.println("Flight Reserved");
      } else {
        System.out.println("Flight could not be reserved");
      }
      break;
    }
    case ReserveCar: {
      checkArgumentsCount(4, arguments.size());

      System.out.println("Reserving a car at a location [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Customer ID: " + arguments.elementAt(2));
      System.out.println("-Car Location: " + arguments.elementAt(3));

      int id = toInt(arguments.elementAt(1));
      int customerID = toInt(arguments.elementAt(2));
      String location = arguments.elementAt(3);

      if (m_resourceManager.reserveCar(id, customerID, location)) {
        System.out.println("Car Reserved");
      } else {
        System.out.println("Car could not be reserved");
      }
      break;
    }
    case ReserveRoom: {
      checkArgumentsCount(4, arguments.size());

      System.out.println("Reserving a room at a location [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Customer ID: " + arguments.elementAt(2));
      System.out.println("-Room Location: " + arguments.elementAt(3));

      int id = toInt(arguments.elementAt(1));
      int customerID = toInt(arguments.elementAt(2));
      String location = arguments.elementAt(3);

      if (m_resourceManager.reserveRoom(id, customerID, location)) {
        System.out.println("Room Reserved");
      } else {
        System.out.println("Room could not be reserved");
      }
      break;
    }
    case Bundle: {
      if (arguments.size() < 7) {
        System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27
            + "[0mBundle command expects at least 7 arguments. Location \"help\" or \"help,<CommandName>\"");
        break;
      }

      System.out.println("Reserving an bundle [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Customer ID: " + arguments.elementAt(2));
      for (int i = 0; i < arguments.size() - 6; ++i) {
        System.out.println("-Flight Number: " + arguments.elementAt(3 + i));
      }
      System.out.println("-Car Location: " + arguments.elementAt(arguments.size() - 2));
      System.out.println("-Room Location: " + arguments.elementAt(arguments.size() - 1));

      int id = toInt(arguments.elementAt(1));
      int customerID = toInt(arguments.elementAt(2));
      Vector<String> flightNumbers = new Vector<String>();
      for (int i = 0; i < arguments.size() - 6; ++i) {
        flightNumbers.addElement(arguments.elementAt(3 + i));
      }
      String location = arguments.elementAt(arguments.size() - 3);
      boolean car = toBoolean(arguments.elementAt(arguments.size() - 2));
      boolean room = toBoolean(arguments.elementAt(arguments.size() - 1));
      if (m_resourceManager.bundle(id, customerID, flightNumbers, location, car, room)) {
        System.out.println("Bundle Reserved");
      } else {
        System.out.println("Bundle could not be reserved");
      }
      break;
    }
    case Quit:
      checkArgumentsCount(1, arguments.size());

      System.out.println("Quitting client");
      System.exit(0);
    }
  }
  
  public void setListener(UserCommandListener listener) {
    this.listener = listener;
  }
  
  class UserCommandListernerImpl implements UserCommandListener {

    @Override
    public void userEnteredCommand() {
      Runnable r = () -> {
        // package command
        
        // write object to server socket 
        
        // get a response
        
        // write to console
      };
      executor.execute(r);
    }
  }

}
