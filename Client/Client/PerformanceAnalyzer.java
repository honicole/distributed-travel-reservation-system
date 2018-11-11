package Client;

import static Client.Command.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;

public class PerformanceAnalyzer extends TCPClient {
  
  private static String s_serverHost = "localhost";
  private static int s_serverPort = 1099;
  
  private static final String[] CITIES = {"Montreal", "Vancouver", "Chicago", "Miami", "Tokyo", "Dubai"};
  
  private static final String[] OPTIONS = 
      {"Single Client-Single RM", "Single Client-Multiple RMs", "Multiple Clients & RMs"};
  private static int option = 0;
  
  private static final String FILENAME = "./log.txt";
  private static File logFile = new File(FILENAME);
  private static StringBuilder log = new StringBuilder();
  private static int counter = 0;
  
  Random random = new Random();
  
  // could have multiple lists, like itinery, etc
  private UserCommand[] commands;
  
  private static final UserCommand START = new UserCommand(start, new String[] {"start"});
  

  public PerformanceAnalyzer(Socket socket) throws Exception {
    super(socket);
  }

  public static void main(String[] args) {
    if (args.length > 0) {
      s_serverHost = args[0];
    }
    if (args.length > 1) {
      s_serverPort = Integer.valueOf(args[1]);
    }
    
    if (args.length > 3) {
      System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27
          + "[0mUsage: java client.PerformanceAnalyzer [server_hostname [server_port]] option(0-2)(SCSR-SCMR-MCMR)");
      System.exit(1);
    }

    if (!logFile.exists()) {
      try {
        logFile.createNewFile();
      } catch (IOException e) {}
    }

    // Write log to disk on Ctrl-C
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        BufferedWriter writer = new BufferedWriter(new FileWriter(FILENAME));
        writer.write(log.toString());
        writer.close();
      } catch (Exception e) {}
    }));

    PerformanceAnalyzer performanceAnalyzer = null;
    try {
      performanceAnalyzer = new PerformanceAnalyzer(new Socket());
      option = Integer.parseInt(args[2]);
    } catch (Exception e) {
      e.printStackTrace();
    }
    performanceAnalyzer.runCommand();

    // Set the security policy
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new SecurityManager());
    }

  }
  
  @Override
  public void runCommand() {
    // Prepare for reading commands
    System.out.println();
    System.out.println("Preparing to run performance analysis...");

    try (Socket serverSocket = new Socket(s_serverHost, s_serverPort);
        ObjectOutputStream oos = new ObjectOutputStream(serverSocket.getOutputStream());
        ObjectInputStream ois = new ObjectInputStream(serverSocket.getInputStream());) {

      this.oos = oos;
      this.ois = ois;

      System.out.println(OPTIONS[option] + "\n");

      while (true) {
        long start = System.currentTimeMillis();
        System.out.println(">] start");
        execute(START);
        int xid = this.xid;
        
        commands = createUserCommands(xid);
        
        for (UserCommand uc: commands) {
          System.out.println(">] " + uc);
          execute(uc);
          System.out.println();
          
          log.append(counter + "," + xid + "," + System.currentTimeMillis() + "\n");
          
          if (option > 0) { // Multiple RMs
            int x = randomNumberBetween(0, 50) - 25; // [-25, 25]
            long transactionDuration = System.currentTimeMillis() - start;
            if(transactionDuration < 500 + x) {
              sleep((500 + x) - transactionDuration);
            }
          }
        }
        counter++;
      }
      
    } catch (Exception e) {
      System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUncaught exception");
      e.printStackTrace();
      System.exit(1);
    }
  }
  
  private UserCommand[] createUserCommands(int xid) {
    if (option == 0) { // Single Client-Single RM
      // Simply add and query flights
      String xid_ = Integer.toString(xid);
      String flightNumber1 = Integer.toString(randomNumberBetween(1000, 1999));
      String numberOfSeats1 = Integer.toString(50*randomNumberBetween(1, 6));
      String flightPrice1 = Integer.toString(100*randomNumberBetween(1, 9) + 99);
      
      String flightNumber2 = Integer.toString(randomNumberBetween(2000, 2999));
      String numberOfSeats2 = Integer.toString(50*randomNumberBetween(1, 6));
      String flightPrice2 = Integer.toString(100*randomNumberBetween(1, 9) + 99);
      
      String flightNumber3 = Integer.toString(randomNumberBetween(3000, 3999));
      String numberOfSeats3 = Integer.toString(50*randomNumberBetween(1, 6));
      String flightPrice3 = Integer.toString(100*randomNumberBetween(1, 9) + 99);
      
      String flightNumber4 = Integer.toString(randomNumberBetween(4000, 4999));
      String numberOfSeats4 = Integer.toString(50*randomNumberBetween(1, 6));
      String flightPrice4 = Integer.toString(100*randomNumberBetween(1, 9) + 99);
      
      UserCommand[] commands = {
        new UserCommand(AddFlight, new String[] {"addFlight", xid_, flightNumber1, numberOfSeats1, flightPrice1}),
        new UserCommand(AddFlight, new String[] {"addFlight", xid_, flightNumber2, numberOfSeats2, flightPrice2}),
        new UserCommand(AddFlight, new String[] {"addFlight", xid_, flightNumber3, numberOfSeats3, flightPrice3}),
        new UserCommand(AddFlight, new String[] {"addFlight", xid_, flightNumber4, numberOfSeats4, flightPrice4}),
        
        new UserCommand(QueryFlight, new String[] {"queryFlight", xid_, flightNumber1}),
        new UserCommand(QueryFlight, new String[] {"queryFlight", xid_, flightNumber2}),
        new UserCommand(QueryFlight, new String[] {"queryFlight", xid_, flightNumber3}),
        new UserCommand(QueryFlight, new String[] {"queryFlight", xid_, flightNumber4}),
        
        new UserCommand(QueryFlightPrice, new String[] {"queryFlightPrice", xid_, flightNumber1}),
        new UserCommand(QueryFlightPrice, new String[] {"queryFlightPrice", xid_, flightNumber2}),
        new UserCommand(QueryFlightPrice, new String[] {"queryFlightPrice", xid_, flightNumber3}),
        new UserCommand(QueryFlightPrice, new String[] {"queryFlightPrice", xid_, flightNumber4}),

        new UserCommand(commit, new String[] {"commit", xid_}),
      };
    
      return commands;
    } else { // Single/Multiple Client(s)-Multiple RMs
      // Customer reserves a flight, a car, and a hotel.
      String xid_ = Integer.toString(xid);
      String customerId = Integer.toString(randomNumberBetween(1, 5000));
      String flightNumber = Integer.toString(randomNumberBetween(1000, 4999));
      String numberOfSeats = Integer.toString(50*randomNumberBetween(1, 6));
      String numberOfCars = Integer.toString(randomNumberBetween(10, 70));
      String numberOfRooms = Integer.toString(randomNumberBetween(30, 7351));
      String flightPrice = Integer.toString(100*randomNumberBetween(1, 9) + 99);
      String carPrice = Integer.toString(5*randomNumberBetween(25, 150));
      String roomPrice = Integer.toString(5*randomNumberBetween(85, 450));
      String location = (String) oneOf(CITIES);
      
      UserCommand[] commands = {
        new UserCommand(AddFlight, new String[] {"addFlight", xid_, flightNumber, numberOfSeats, flightPrice}),
        new UserCommand(AddCars, new String[] {"addCars", xid_, location, numberOfCars, carPrice}),
        new UserCommand(AddRooms, new String[] {"addRooms", xid_, location, numberOfRooms, roomPrice}),
        new UserCommand(AddCustomerID, new String[] {"addCustomerID", xid_, customerId}),
        
        new UserCommand(QueryFlight, new String[] {"queryFlight", xid_, flightNumber}),
        new UserCommand(QueryCars, new String[] {"queryCars", xid_, location}),
        new UserCommand(QueryRooms, new String[] {"queryRooms", xid_, location}),
        
        new UserCommand(QueryFlightPrice, new String[] {"queryFlightPrice", xid_, flightNumber}),
        new UserCommand(QueryCarsPrice, new String[] {"queryCarsPrice", xid_, location}),
        new UserCommand(QueryRoomsPrice, new String[] {"queryRoomsPrice", xid_, location}),
        
        new UserCommand(ReserveFlight, new String[] {"reserveFlight", xid_, customerId, flightNumber}),
        new UserCommand(ReserveCar, new String[] {"reserveCar", xid_, customerId, location}),
        new UserCommand(ReserveRoom, new String[] {"reserveRoom", xid_, customerId, location}),
        
        new UserCommand(QueryCustomer, new String[] {"queryCustomer", xid_, customerId}),
        new UserCommand(commit, new String[] {"commit", xid_}),
      };
      
      return commands;
    }
  }
  
  private void sleep(long millis) {
    try { Thread.sleep(millis); } catch (Exception e) {}
  }
  
  /**
   * @param min
   * @param max
   * @return a random integer between min and max, inclusive
   */
  private int randomNumberBetween(int min, int max) {
    return random.nextInt(max + 1 - min) + min;
  }
  
  /**
   * @param objects
   * @return one random element from an array of objects
   */
  private Object oneOf(Object[] objects) {
    int l = objects.length;
    return objects[randomNumberBetween(0, l - 1)];
  }
  
}
