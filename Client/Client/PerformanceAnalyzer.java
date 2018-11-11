package Client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static Client.Command.*;

public class PerformanceAnalyzer extends TCPClient {
  
  private static String s_serverHost = "localhost";
  private static int s_serverPort = 1099;
  private static Executor executor = Executors.newFixedThreadPool(8);
  private Socket socket;
  private static Socket serverSocket;
  
  private static final int NUM_TRANSACTIONS = 30;
  
  private static boolean multipleClients = false;
  
  Random random = new Random();
  
  
  // could have multiple lists, like itinery, etc
  private UserCommand[] commands;
  
  private static final UserCommand START = new UserCommand(start, new String[] {"start"});

  public PerformanceAnalyzer(Socket socket) throws Exception {
    super(socket);
    this.socket = socket;
  }

  public static void main(String[] args) {
    if (args.length > 0) {
      s_serverHost = args[0];
    }
    if (args.length > 1) {
      s_serverPort = Integer.valueOf(args[1]);
    }
    if (args.length > 2) {
      multipleClients = Boolean.parseBoolean(args[2]);
    }
    if (args.length > 3) {
      System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27
          + "[0mUsage: java client.PerformanceAnalyzer [server_hostname [server_port]] multipleClients(true/false)");
      System.exit(1);
    }

    PerformanceAnalyzer performanceAnalyzer = null;
    try {
      performanceAnalyzer = new PerformanceAnalyzer(new Socket());
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("PA: " + performanceAnalyzer);
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

      for(int i = 0; i < NUM_TRANSACTIONS; i++) {
        execute(START);
        //waitFor(100);
        int xid = this.xid;
        
        commands = createUserCommands(xid);
        
        for (UserCommand uc: commands) {
          long start = System.currentTimeMillis();
          System.out.println(">] " + uc);
          execute(uc);
          System.out.println();
          long now = System.currentTimeMillis();
          if(multipleClients && now - start < 500) {
            // wait
            while(System.currentTimeMillis() - start < 500)
              ;
          }
        }
      }
      
    } catch (Exception e) {
      System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUncaught exception");
      e.printStackTrace();
      System.exit(1);
    }
  }
  
  private UserCommand[] createUserCommands(int xid) {
    if (!multipleClients) {
      // Customer reserves a flight
      String xid_ = Integer.toString(xid);
      String customerId = Integer.toString(randomNumberBetween(1, 20));
      String flightNumber = Integer.toString(randomNumberBetween(100, 199));
      String numberOfSeats = Integer.toString(50*randomNumberBetween(1, 6));
      String price = Integer.toString(100*randomNumberBetween(1, 9) + 99);
      
      UserCommand[] commands = {
        new UserCommand(AddFlight, new String[] {"addFlight", xid_, flightNumber, numberOfSeats, price}),
        new UserCommand(AddCustomerID, new String[] {"addCustomerID", xid_, customerId}),
        new UserCommand(QueryFlight, new String[] {"queryFlight", xid_, flightNumber}),
        new UserCommand(QueryFlightPrice, new String[] {"queryFlightPrice", xid_, flightNumber}),
        new UserCommand(ReserveFlight, new String[] {"reserveFlight", xid_, customerId, flightNumber}),
        new UserCommand(QueryCustomer, new String[] {"queryCustomer", xid_, customerId}),
        new UserCommand(commit, new String[] {"commit", xid_}),
      };
      
      return commands;
    } else {
      return null;
    }
  }

  private void waitFor(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
    }
  }
  
  /**
   * @param min
   * @param max
   * @return a random integer between min and max, inclusive
   */
  private int randomNumberBetween(int min, int max) {
    return random.nextInt(max + 1 - min) + min;
  }
  
}
