package middleware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;
import java.util.StringTokenizer;
import java.util.Vector;

import Client.Command;
import Server.Interface.IResourceManager;

public class Middleware implements IResourceManager {
  
  IResourceManager[] resourceManagers;
  
  IResourceManager flightManager;
  IResourceManager carManager;
  IResourceManager roomManager;
  
  String name;
  
  public Middleware(IResourceManager... resourceManagers) {
    this.resourceManagers = resourceManagers;
    
    flightManager = resourceManagers[0];
    carManager = resourceManagers[1];
    roomManager = resourceManagers[2];
  }
  
  
  @Override
  public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException {
    return flightManager.addFlight(id, flightNum, flightSeats, flightPrice);
  }

  @Override
  public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
    return carManager.addCars(id, location, numCars, price);
  }

  @Override
  public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
    return roomManager.addRooms(id, location, numRooms, price);
  }

  @Override
  public int newCustomer(int id) throws RemoteException {
    return 0;
  }

  @Override
  public boolean newCustomer(int id, int cid) throws RemoteException {
    return false;
  }

  @Override
  public boolean deleteFlight(int id, int flightNum) throws RemoteException {
    return flightManager.deleteFlight(id, flightNum);
  }

  @Override
  public boolean deleteCars(int id, String location) throws RemoteException {
    return carManager.deleteCars(id, location);
  }

  @Override
  public boolean deleteRooms(int id, String location) throws RemoteException {
    return roomManager.deleteRooms(id, location);
  }

  @Override
  public boolean deleteCustomer(int id, int customerID) throws RemoteException {
    return false;
  }

  @Override
  public int queryFlight(int id, int flightNumber) throws RemoteException {
    return flightManager.queryFlight(id, flightNumber);
  }

  @Override
  public int queryCars(int id, String location) throws RemoteException {
    return carManager.queryCars(id, location);
  }

  @Override
  public int queryRooms(int id, String location) throws RemoteException {
    return roomManager.queryRooms(id, location);
  }

  @Override
  public String queryCustomerInfo(int id, int customerID) throws RemoteException {
    return null;
  }

  @Override
  public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
    return flightManager.queryFlightPrice(id, flightNumber);
  }

  @Override
  public int queryCarsPrice(int id, String location) throws RemoteException {
    return flightManager.queryCarsPrice(id, location);
  }

  @Override
  public int queryRoomsPrice(int id, String location) throws RemoteException {
    return roomManager.queryRoomsPrice(id, location);
  }

  @Override
  public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException {
    return flightManager.reserveFlight(id, customerID, flightNumber);
  }

  @Override
  public boolean reserveCar(int id, int customerID, String location) throws RemoteException {
    return carManager.reserveCar(id, customerID, location);
  }

  @Override
  public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {
    return roomManager.reserveRoom(id, customerID, location);
  }

  @Override
  public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car,
      boolean room) throws RemoteException {
    return false;
  }

  @Override
  public String getName() throws RemoteException {
    return name;
  }
  
  
  //////////////////////////
  
  public void execute(Command cmd, Vector<String> arguments) throws RemoteException, NumberFormatException {
    switch (cmd) {
    case Help: {
      if (arguments.size() == 1) {
        System.out.println(Command.description());
      } else if (arguments.size() == 2) {
        Command l_cmd = Command.fromString((String) arguments.elementAt(1));
        System.out.println(l_cmd.toString());
      } else {
        System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27
            + "[0mImproper use of help command. Location \"help\" or \"help,<CommandName>\"");
      }
      break;
    }
    case AddFlight: {
      System.out.println("Adding a new flight [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Flight Number: " + arguments.elementAt(2));
      System.out.println("-Flight Seats: " + arguments.elementAt(3));
      System.out.println("-Flight Price: " + arguments.elementAt(4));

      int id = toInt(arguments.elementAt(1));
      int flightNum = toInt(arguments.elementAt(2));
      int flightSeats = toInt(arguments.elementAt(3));
      int flightPrice = toInt(arguments.elementAt(4));

      if (addFlight(id, flightNum, flightSeats, flightPrice)) {
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

      if (addCars(id, location, numCars, price)) {
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

      if (addRooms(id, location, numRooms, price)) {
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
      int customer = newCustomer(id);

      System.out.println("Add customer ID: " + customer);
      break;
    }
    case AddCustomerID: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Customer ID: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      int customerID = toInt(arguments.elementAt(2));

      if (newCustomer(id, customerID)) {
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

      if (deleteFlight(id, flightNum)) {
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

      if (deleteCars(id, location)) {
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

      if (deleteRooms(id, location)) {
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

      if (deleteCustomer(id, customerID)) {
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

      int seats = queryFlight(id, flightNum);
      System.out.println("Number of seats available: " + seats);
      break;
    }
    case QueryCars: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying cars location [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Car Location: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      String location = arguments.elementAt(2);

      int numCars = queryCars(id, location);
      System.out.println("Number of cars at this location: " + numCars);
      break;
    }
    case QueryRooms: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying rooms location [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Room Location: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      String location = arguments.elementAt(2);

      int numRoom = queryRooms(id, location);
      System.out.println("Number of rooms at this location: " + numRoom);
      break;
    }
    case QueryCustomer: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying customer information [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Customer ID: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      int customerID = toInt(arguments.elementAt(2));

      String bill = queryCustomerInfo(id, customerID);
      System.out.print(bill);
      break;
    }
    case QueryFlightPrice: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying a flight price [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Flight Number: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      int flightNum = toInt(arguments.elementAt(2));

      int price = queryFlightPrice(id, flightNum);
      System.out.println("Price of a seat: " + price);
      break;
    }
    case QueryCarsPrice: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying cars price [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Car Location: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      String location = arguments.elementAt(2);

      int price = queryCarsPrice(id, location);
      System.out.println("Price of cars at this location: " + price);
      break;
    }
    case QueryRoomsPrice: {
      checkArgumentsCount(3, arguments.size());

      System.out.println("Querying rooms price [xid=" + arguments.elementAt(1) + "]");
      System.out.println("-Room Location: " + arguments.elementAt(2));

      int id = toInt(arguments.elementAt(1));
      String location = arguments.elementAt(2);

      int price = queryRoomsPrice(id, location);
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

      if (reserveFlight(id, customerID, flightNum)) {
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

      if (reserveCar(id, customerID, location)) {
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

      if (reserveRoom(id, customerID, location)) {
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

      if (bundle(id, customerID, flightNumbers, location, car, room)) {
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
  
  public static Vector<String> parse(String command) {
    Vector<String> arguments = new Vector<String>();
    StringTokenizer tokenizer = new StringTokenizer(command, ",");
    String argument = "";
    while (tokenizer.hasMoreTokens()) {
      argument = tokenizer.nextToken();
      argument = argument.trim();
      arguments.add(argument);
    }
    return arguments;
  }
  
  public static void checkArgumentsCount(Integer expected, Integer actual) throws IllegalArgumentException {
    if (expected != actual) {
      throw new IllegalArgumentException("Invalid number of arguments. Expected " + (expected - 1) + ", received "
          + (actual - 1) + ". Location \"help,<CommandName>\" to check usage of this command");
    }
  }
  
  public static int toInt(String string) throws NumberFormatException {
    return Integer.valueOf(string);
  }

  public static boolean toBoolean(String string) {
    return Boolean.valueOf(string);
  }
  
}
