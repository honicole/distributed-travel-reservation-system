package Client;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

/**
 * Temp class to be removed
 */
public class TestConn {

  public static void main(String[] args) {
    try {
      Remote r =LocateRegistry.getRegistry("localhost", 1099).lookup("group3_Middleware");
      System.out.println(r.toString());
    } catch (RemoteException | NotBoundException e) {
      e.printStackTrace();
    }
  }

}
