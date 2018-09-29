package middleware;

import java.rmi.RemoteException;
import java.util.Vector;

import middleware.Interface.IResourceManager;

public class FlightResourceManager implements IResourceManager {

  // TODO Implement these methods
  @Override
  public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException {
    return false;
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
    return false;
  }

  @Override
  public boolean deleteCustomer(int id, int customerID) throws RemoteException {
    return false;
  }

  @Override
  public int queryFlight(int id, int flightNumber) throws RemoteException {
    return 0;
  }

  @Override
  public String queryCustomerInfo(int id, int customerID) throws RemoteException {
    return null;
  }

  @Override
  public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
    return 0;
  }

  @Override
  public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException {
    return false;
  }

  @Override
  public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car,
      boolean room) throws RemoteException {
    return false;
  }

  @Override
  public String getName() throws RemoteException {
    return null;
  }
  
  
  
  
  // Irrelevant methods

  @Override
  public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
    return false;
  }

  @Override
  public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
    return false;
  }

  @Override
  public boolean deleteCars(int id, String location) throws RemoteException {
    return false;
  }

  @Override
  public boolean deleteRooms(int id, String location) throws RemoteException {
    return false;
  }

  @Override
  public int queryCars(int id, String location) throws RemoteException {
    return 0;
  }

  @Override
  public int queryRooms(int id, String location) throws RemoteException {
    return 0;
  }

  @Override
  public int queryCarsPrice(int id, String location) throws RemoteException {
    return 0;
  }

  @Override
  public int queryRoomsPrice(int id, String location) throws RemoteException {
    return 0;
  }

  @Override
  public boolean reserveCar(int id, int customerID, String location) throws RemoteException {
    return false;
  }

  @Override
  public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {
    return false;
  }

}
