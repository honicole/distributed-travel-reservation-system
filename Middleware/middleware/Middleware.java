package middleware;

import java.rmi.RemoteException;
import java.util.Vector;

import Server.Interface.IResourceManager;

public class Middleware implements IResourceManager {

  IResourceManager[] resourceManagers;

  IResourceManager flightManager;
  IResourceManager carManager;
  IResourceManager roomManager;

  String name;
  
  public Middleware() {}

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
    int cid = flightManager.newCustomer(id);
    carManager.newCustomer(id, cid);
    roomManager.newCustomer(id, cid);
    return cid;
  }

  @Override
  public boolean newCustomer(int id, int cid) throws RemoteException {
    return flightManager.newCustomer(id, cid) && carManager.newCustomer(id, cid) && roomManager.newCustomer(id, cid);
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
    return flightManager.deleteCustomer(id, customerID) && carManager.deleteCustomer(id, customerID)
        && roomManager.deleteCustomer(id, customerID);
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
    return flightManager.queryCustomerInfo(id, customerID);
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
    boolean flightsReserved = true, carReserved = true, roomReserved = true;

    for (String flightNumber : flightNumbers) {
      flightsReserved = flightManager.reserveFlight(id, customerID, Integer.valueOf(flightNumber));
      if (!flightsReserved) {
        return false;
      }
    }
    if (car) {
      carReserved = carManager.reserveCar(id, customerID, location);
    }
    if (room) {
      roomReserved = roomManager.reserveRoom(id, customerID, location);
    }
    return flightsReserved && carReserved && roomReserved;
  }

  @Override
  public String getName() throws RemoteException {
    return name;
  }

}
