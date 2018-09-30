package middleware;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import Server.Interface.IResourceManager;

//import middleware.Interface.IResourceManager;
//import Server.RMI.RMIResourceManager;

public class RMIMiddleware extends Middleware {

  private static String s_serverName = "Middleware";
  private static String s_rmiPrefix = "group3_";
  
  private String name;
  
  public RMIMiddleware(String name) {
    super(new FlightResourceManager("Flights"), new CarResourceManager("Cars"), new RoomResourceManager("Rooms"));
    this.name = name;
  }

  public static void main(String[] args) {
    System.out.println("RMIMiddleware successfully called! :)");
    
    if (args.length > 0) {
      s_serverName = args[0];
    }

    // Create the RMI server entry
    try {
      // Create a new Server object
      IResourceManager server = new RMIMiddleware(s_serverName);

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
      registry.rebind(s_rmiPrefix + s_serverName, resourceManager);

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

  public String getName() {
    return name;
  }
  
}
