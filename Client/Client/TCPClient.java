package Client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import Server.LockManager.DeadlockException;

public class TCPClient extends Client {
  private static String s_serverHost = "localhost";
  private static int s_serverPort = 1099;
  private static Executor executor = Executors.newFixedThreadPool(8);
  private Socket socket;
  private Socket serverSocket;
  protected ObjectOutputStream oos;
  protected ObjectInputStream ois;

  protected int xid; // for use by performance analysis

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

      this.oos = oos;
      this.ois = ois;

      BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
      TCPClient client = new TCPClient(serverSocket);

      while (true) {
        Vector<String> arguments = new Vector<String>();
        try {
          System.out.print((char) 27 + "[32;1m\n>] " + (char) 27 + "[0m");
          String command = stdIn.readLine().trim();
          arguments = parse(command);
          Command cmd = Command.fromString(arguments.elementAt(0));
          try {
            execute(cmd, arguments);
          } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
          }
        } catch (IllegalArgumentException e) {
          System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0m" + e.getLocalizedMessage());
        } catch (Exception e) {
          System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0mUncaught exception");
          e.printStackTrace();
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

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Flight could not be added");
          System.out.println("Deadlock detected -- transaction aborted");
        } else if ((boolean) result) {
          System.out.println("Flight added");
        } else {
          System.out.println("Flight could not be added");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case AddCars: {
      checkArgumentsCount(5, arguments.size());

      System.out.println("Adding new cars [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Car Location: " + arguments.elementAt(2));
      System.out.println("-Number of Cars: " + arguments.elementAt(3));
      System.out.println("-Car Price: " + arguments.elementAt(4));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Cars could not be added");
          System.out.println("Deadlock detected -- transaction aborted");
        } else if ((boolean) result) {
          System.out.println("Cars added");
        } else {
          System.out.println("Cars could not be added");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case AddRooms: {
      checkArgumentsCount(5, arguments.size());

      System.out.println("Adding new rooms [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Room Location: " + arguments.elementAt(2));
      System.out.println("-Number of Rooms: " + arguments.elementAt(3));
      System.out.println("-Room Price: " + arguments.elementAt(4));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Rooms could not be added");
          System.out.println("Deadlock detected -- transaction aborted");
        } else if ((boolean) result) {
          System.out.println("Rooms added");
        } else {
          System.out.println("Rooms could not be added");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case AddCustomer: {
      checkArgumentsCount(2, arguments.size());

      System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Customer could not be added");
          System.out.println("Deadlock detected -- transaction aborted");
        } else {
          int customer = (int) result;
          System.out.println("Add customer ID: " + customer);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case AddCustomerID: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Customer ID: " + arguments.elementAt(2));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Customer could not be added");
          System.out.println("Deadlock detected -- transaction aborted");
        } else if ((boolean) result) {
          System.out.println("Add customer ID: " + arguments.elementAt(2));
        } else {
          System.out.println("Customer could not be added");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case DeleteFlight: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Deleting a flight [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Flight Number: " + arguments.elementAt(2));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Flight could not be deleted");
          System.out.println("Deadlock detected -- transaction aborted");
        } else if ((boolean) result) {
          System.out.println("Flight Deleted");
        } else {
          System.out.println("Flight could not be deleted");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case DeleteCars: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Deleting all cars at a particular location [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Car Location: " + arguments.elementAt(2));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Cars could not be deleted");
          System.out.println("Deadlock detected -- transaction aborted");
        } else if ((boolean) result) {
          System.out.println("Cars Deleted");
        } else {
          System.out.println("Cars could not be deleted");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case DeleteRooms: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Deleting all rooms at a particular location [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Car Location: " + arguments.elementAt(2));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Rooms could not be deleted");
          System.out.println("Deadlock detected -- transaction aborted");
        } else if ((boolean) result) {
          System.out.println("Rooms Deleted");
        } else {
          System.out.println("Rooms could not be deleted");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case DeleteCustomer: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Deleting a customer from the database [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Customer ID: " + arguments.elementAt(2));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Customer could not be deleted");
          System.out.println("Deadlock detected -- transaction aborted");
          break;
        } else if ((boolean) result) {
          System.out.println("Customer Deleted");
        } else {
          System.out.println("Customer could not be deleted");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case QueryFlight: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying a flight [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Flight Number: " + arguments.elementAt(2));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Deadlock detected -- transaction aborted");
          break;
        } else {
          int seats = (int) result;
          System.out.println("Number of seats available: " + seats);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case QueryCars: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying cars location [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Car Location: " + arguments.elementAt(2));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return true;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Deadlock detected -- transaction aborted");
          break;
        } else {
          int numCars = (int) result;
          System.out.println("Number of cars at this location: " + numCars);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case QueryRooms: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying rooms location [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Room Location: " + arguments.elementAt(2));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Deadlock detected -- transaction aborted");
          break;
        } else {
          int numRoom = (int) result;
          System.out.println("Number of rooms at this location: " + numRoom);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case QueryCustomer: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying customer information [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Customer ID: " + arguments.elementAt(2));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Deadlock detected -- transaction aborted");
          break;
        } else {
          String bill = (String) result;
          System.out.print(bill);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case QueryFlightPrice: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying a flight price [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Flight Number: " + arguments.elementAt(2));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Deadlock detected -- transaction aborted");
          break;
        } else {
          int price = (int) result;
          System.out.println("Price of a seat: " + price);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case QueryCarsPrice: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying cars price [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Car Location: " + arguments.elementAt(2));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Deadlock detected -- transaction aborted");
          break;
        } else {
          int price = (int) result;
          System.out.println("Price of cars at this location: " + price);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case QueryRoomsPrice: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying rooms price [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Room Location: " + arguments.elementAt(2));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Deadlock detected -- transaction aborted");
          break;
        } else {
          int price = (int) result;
          System.out.println("Price of rooms at this location: " + price);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case ReserveFlight: {
      checkArgumentsCount(4, arguments.size());

      System.out.println("Reserving seat in a flight [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Customer ID: " + arguments.elementAt(2));
      System.out.println("-Flight Number: " + arguments.elementAt(3));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Flight could not be reserved");
          System.out.println("Deadlock detected -- transaction aborted");
          break;
        } else if ((boolean) result) {
          System.out.println("Flight Reserved");
        } else {
          System.out.println("Flight could not be reserved");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case ReserveCar: {
      checkArgumentsCount(4, arguments.size());

      System.out.println("Reserving a car at a location [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Customer ID: " + arguments.elementAt(2));
      System.out.println("-Car Location: " + arguments.elementAt(3));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Car could not be reserved");
          System.out.println("Deadlock detected -- transaction aborted");
          break;
        } else if ((boolean) result) {
          System.out.println("Car Reserved");
        } else {
          System.out.println("Car could not be reserved");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case ReserveRoom: {
      checkArgumentsCount(4, arguments.size());

      System.out.println("Reserving a room at a location [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Customer ID: " + arguments.elementAt(2));
      System.out.println("-Room Location: " + arguments.elementAt(3));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Room could not be reserved");
          System.out.println("Deadlock detected -- transaction aborted");
          break;
        } else if ((boolean) result) {
          System.out.println("Room Reserved");
        } else {
          System.out.println("Room could not be reserved");
        }
      } catch (Exception e) {
        e.printStackTrace();
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

      Vector<String> flightNumbers = new Vector<String>();
      for (int i = 0; i < arguments.size() - 6; ++i) {
        flightNumbers.addElement(arguments.elementAt(3 + i));
      }
      String location = arguments.elementAt(arguments.size() - 3);
      boolean car = toBoolean(arguments.elementAt(arguments.size() - 2));
      boolean room = toBoolean(arguments.elementAt(arguments.size() - 1));

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Bundle could not be reserved");
          System.out.println("Deadlock detected -- transaction aborted");
          break;
        } else if ((boolean) result) {
          System.out.println("Bundle Reserved");
        } else {
          System.out.println("Bundle could not be reserved");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case start: {
      checkArgumentsCount(1, arguments.size());

      System.out.println("Starting a transaction");

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        int xid = (Integer) future.get();
        this.xid = xid;
        System.out.println("Transaction id: " + xid);
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }

    case commit: {
      checkArgumentsCount(2, arguments.size());

      System.out.println("Committing a transaction [xid=" + arguments.elementAt(1) + "]");

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if (result instanceof DeadlockException) {
          System.out.println("Deadlock detected -- transaction aborted");
          break;
        } else if ((boolean) result) {
          System.out.println("Transaction was committed");
        } else {
          System.out.println("Transaction could not be committed");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case abort: {
      checkArgumentsCount(2, arguments.size());

      System.out.println("Aborting a transaction [xid=" + arguments.elementAt(1) + "]");

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        if ((Boolean) future.get()) {
          System.out.println("Transaction was aborted");
        } else {
          System.out.println("Transaction could not be aborted");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case crashMiddleware: {
      checkArgumentsCount(2, arguments.size());

      System.out.println("Enabling middleware crash mode [mode=" + arguments.elementAt(1) + "]");

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if ((boolean) result) {
          System.out.println("Middleware crash mode enabled");
        } else {
          System.out.println("Middleware crash mode could not be enabled");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case crashResourceManager: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Enabling crash mode [mode=" + arguments.elementAt(2) + "] at resource manager [rm=" 
          + arguments.elementAt(1) + "]");

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if ((boolean) result) {
          System.out.println("Crash mode enabled");
        } else {
          System.out.println("Crash mode could not be enabled");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case resetCrashes: {
      checkArgumentsCount(1, arguments.size());

      System.out.println("Resetting all crash modes");

      final String[] args = arguments.toArray(new String[arguments.size()]);
      final UserCommand packagedCommand = new UserCommand(cmd, args);
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        try {
          oos.writeObject(packagedCommand);
          return ois.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return false;
      }, executor);

      try {
        Object result = future.get();
        if ((boolean) result) {
          System.out.println("Crash modes disabled");
        } else {
          System.out.println("Crash mode could not be disabled");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      break;
    }
    case Quit:
      checkArgumentsCount(1, arguments.size());

      System.out.println("Quitting client");
      System.exit(0);
    }
  }

  public void execute(UserCommand uc) throws NumberFormatException, RemoteException {
    execute(uc.getCommand(), new Vector<String>(Arrays.asList(uc.getArgs())));
  }

}
