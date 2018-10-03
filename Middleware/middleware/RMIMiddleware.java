package middleware;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import Server.Interface.IResourceManager;
import Server.RMI.RMIResourceManager;

public class RMIMiddleware extends Middleware {

  private static String[] s_serverHosts = new String[] {"localhost", "localhost", "localhost"};
  private static int s_serverPort = 1099;
  private static String s_serverName = "Middleware";
  private static String s_rmiPrefix = "group3_";
  
  private String[] serverNames;
  
  
  public RMIMiddleware(String name, String[] args) {
    super(new RMIResourceManager(args[2]),
          new RMIResourceManager(args[4]),
          new RMIResourceManager(args[6]));
    this.name = name;
    s_serverHosts = new String[] {args[1], args[3], args[5]};
    serverNames = new String[] {args[2], args[4], args[6]};
  }

  public static void main(String[] args) {
    System.out.println("RMIMiddleware successfully called! :)");
    
    if (args.length > 0) {
      s_serverName = args[0];
    }

    // Create the RMI server entry
    try {
      RMIMiddleware middleware = new RMIMiddleware(s_serverName, args);
      // Get a reference to the RMIRegister
      try {
        middleware.connectServers();
      } catch (Exception e) {
        System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mUncaught exception");
        e.printStackTrace();
      }
      
      // Dynamically generate the stub (client proxy)
      IResourceManager resourceManager = (IResourceManager) UnicastRemoteObject.exportObject(middleware, 0);

      // Bind the remote object's stub in the registry
      Registry l_registry;
      try {
        l_registry = LocateRegistry.createRegistry(1099);
      } catch (RemoteException e) {
        l_registry = LocateRegistry.getRegistry(1099);
      }
      final Registry registry = l_registry;
      String name = s_rmiPrefix + s_serverName;
      registry.rebind(name, resourceManager);

      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          try {
            registry.unbind(s_rmiPrefix + s_serverName);
            System.out.println("'" + s_serverName + "' resource manager unbound");
          } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
          }
        }
      });
      System.out.println(
          "'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix + s_serverName + "'");
    } catch (Exception e) {
      System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mUncaught exception");
      e.printStackTrace();
      System.exit(1);
    }

    // Create and install a security manager
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new SecurityManager());
    }
  }
  
  public void connectServers() {
    connectServer(s_serverHosts, s_serverPort, serverNames);
  }

  public void connectServer(String[] servers, int port, String[] names) {
    try {
      boolean firstConnAttemptFlight = true;
      boolean firstConnAttemptCar = true;
      boolean firstConnAttemptRoom = true;
      while (true) {
        try {
          Registry registry = LocateRegistry.getRegistry(servers[0], port);
          flightManager = (IResourceManager) registry.lookup(s_rmiPrefix + names[0]);
          
          System.out.println("Connected to '" + names[0] + "' server [" + servers[0] + ":" 
              + port + "/" + s_rmiPrefix + names[0] + "]");
        } catch (NotBoundException | RemoteException e) {
          if (firstConnAttemptFlight) {
            System.out.println("Waiting for '" + names[0] + "' server [" + servers[0] + ":" 
                + port + "/" + s_rmiPrefix + names[0] + "]");
            firstConnAttemptFlight = false;
          }
        }
        try {
          Registry registry = LocateRegistry.getRegistry(servers[1], port);
          carManager = (IResourceManager) registry.lookup(s_rmiPrefix + names[1]);
          
          System.out.println("Connected to '" + names[1] + "' server [" + servers[1] + ":" 
              + port + "/" + s_rmiPrefix + names[1] + "]");
        } catch (NotBoundException | RemoteException e) {
          if (firstConnAttemptCar) {
            System.out.println("Waiting for '" + names[1] + "' server [" + servers[1] + ":" 
                + port + "/" + s_rmiPrefix + names[1] + "]");
            firstConnAttemptCar = false;
          }
        }
        try {
          Registry registry = LocateRegistry.getRegistry(servers[2], port);
          roomManager = (IResourceManager) registry.lookup(s_rmiPrefix + names[2]);
          
          System.out.println("Connected to '" + names[2] + "' server [" + servers[2] + ":" 
              + port + "/" + s_rmiPrefix + names[2] + "]");
          break;
        } catch (NotBoundException | RemoteException e) {
          if (firstConnAttemptRoom) {
            System.out.println("Waiting for '" + names[2] + "' server [" + servers[2] + ":" 
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
