package middleware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.rmi.UnmarshalException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;

import Client.Command;
import Server.Interface.IResourceManager;
import Server.RMI.RMIResourceManager;

public class RMIMiddleware extends Middleware {

  private static String s_serverHost = "localhost";
  private static int s_serverPort = 1099;
  private static String s_serverName = "Middleware";
  private static String s_rmiPrefix = "group3_";
  
  private String[] serverNames;
  
  
  public RMIMiddleware(String name, String[] resourceManagerNames) {
    super(new RMIResourceManager(resourceManagerNames[1]),
          new RMIResourceManager(resourceManagerNames[2]),
          new RMIResourceManager(resourceManagerNames[3]));
    this.name = name;
    serverNames = new String[] {resourceManagerNames[1], resourceManagerNames[2], resourceManagerNames[3]};
  }

  public static void main(String[] args) {
    System.out.println("RMIMiddleware successfully called! :)");
    
    if (args.length > 0) {
      s_serverName = args[0];
    }

    // Create the RMI server entry
    try {
      // Create a new Server object
      IResourceManager server = new RMIMiddleware(s_serverName, args);

      // Dynamically generate the stub (client proxy)
      IResourceManager resourceManager = (IResourceManager) UnicastRemoteObject.exportObject(server, 0);

      // Bind the remote object's stub in the registry
      Registry l_registry;
      try {
        l_registry = LocateRegistry.createRegistry(1099);
      } catch (RemoteException e) {
        l_registry = LocateRegistry.getRegistry(1099);
      }
      final Registry registry = l_registry;
      String name = s_rmiPrefix + s_serverName; System.out.println(name);
      registry.rebind(name, resourceManager);

      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          try {
            registry.unbind(s_rmiPrefix + s_serverName);
            System.out.println("'" + s_serverName + "' resource manager unbound");
          } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
          }
        }
      });
      System.out.println(
          "'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
    } catch (Exception e) {
      System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
      e.printStackTrace();
      System.exit(1);
    }

    // Create and install a security manager
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new SecurityManager());
    }
  }
  
  public void start() {
    // Prepare for reading commands
    System.out.println();
    System.out.println("Location \"help\" for list of supported commands");

    BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

    while (true) {
      // Read the next command
      String command = "";
      Vector<String> arguments = new Vector<String>();
      try {
        System.out.print((char) 27 + "[32;1m\n>] " + (char) 27 + "[0m");
        command = stdin.readLine().trim();
      } catch (IOException io) {
        System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0m" + io.getLocalizedMessage());
        io.printStackTrace();
        System.exit(1);
      }

      try {
        arguments = parse(command);
        Command cmd = Command.fromString((String) arguments.elementAt(0));
        try {
          execute(cmd, arguments);
        } catch (ConnectException e) {
          connectServer();
          execute(cmd, arguments);
        }
      } catch (IllegalArgumentException | ServerException e) {
        System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0m" + e.getLocalizedMessage());
      } catch (ConnectException | UnmarshalException e) {
        System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0mConnection to server lost");
      } catch (Exception e) {
        System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0mUncaught exception");
        e.printStackTrace();
      }
    }
  }
  
  public void connectServer() {
    connectServer(s_serverHost, s_serverPort, serverNames);
  }

  public void connectServer(String server, int port, String[] names) {
    try {
      boolean firstConnAttemptFlight = true;
      boolean firstConnAttemptCar = true;
      boolean firstConnAttemptRoom = true;
      while (true) {
        try {
          Registry registry = LocateRegistry.getRegistry(server, port);
          flightManager = (IResourceManager) registry.lookup(s_rmiPrefix + names[0]);
          
          System.out.println("Connected to '" + names[0] + "' servers [" + server + ":" 
              + port + "/" + s_rmiPrefix + names[0] + "]");
          break;
        } catch (NotBoundException | RemoteException e) {
          if (firstConnAttemptFlight) {
            System.out.println("Waiting for '" + names[0] + "' server [" + server + ":" 
                + port + "/" + s_rmiPrefix + names[0] + "]");
            firstConnAttemptFlight = false;
          }
        }
        try {
          Registry registry = LocateRegistry.getRegistry(server, port);
          carManager = (IResourceManager) registry.lookup(s_rmiPrefix + names[1]);
          
          System.out.println("Connected to '" + names[1] + "' servers [" + server + ":" 
              + port + "/" + s_rmiPrefix + names[1] + "]");
          break;
        } catch (NotBoundException | RemoteException e) {
          if (firstConnAttemptCar) {
            System.out.println("Waiting for '" + names[1] + "' server [" + server + ":" 
                + port + "/" + s_rmiPrefix + names[1] + "]");
            firstConnAttemptCar = false;
          }
        }
        try {
          Registry registry = LocateRegistry.getRegistry(server, port);
          roomManager = (IResourceManager) registry.lookup(s_rmiPrefix + names[2]);
          
          System.out.println("Connected to '" + names[2] + "' servers [" + server + ":" 
              + port + "/" + s_rmiPrefix + names[2] + "]");
          break;
        } catch (NotBoundException | RemoteException e) {
          if (firstConnAttemptRoom) {
            System.out.println("Waiting for '" + names[2] + "' server [" + server + ":" 
                + port + "/" + s_rmiPrefix + names[2] + "]");
            firstConnAttemptRoom = false;
          }
        }

        Thread.sleep(500);
      }
    } catch (Exception e) {
      System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mUncaught exception");
      e.printStackTrace();
      System.exit(1);
    }
  }
  
}
