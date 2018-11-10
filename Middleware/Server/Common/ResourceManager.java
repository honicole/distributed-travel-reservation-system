// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import java.util.*;

import Server.Interface.IResourceManager;
import Server.LockManager.DeadlockException;
import Server.LockManager.LockManager;
import Server.LockManager.TransactionLockObject;
import exceptions.InvalidTransactionException;
import exceptions.TransactionAbortedException;
import Server.Common.RMHashMap;

import java.rmi.RemoteException;
import java.io.*;

public class ResourceManager implements IResourceManager {
  protected String m_name = "";
  protected RMHashMap m_data = new RMHashMap();
  protected LockManager lockManager;
  protected Map<Integer, List<String>> write_list = new HashMap<>();
  
  /**
   * Preserves the state of previously committed transactions.
   * 
   * Maps each transaction id to a map, which in turn maps keys to resource manager items
   */
  protected Map<Integer, Map<String, RMItem>> pre_image = new HashMap<>();

  public ResourceManager() {
    lockManager = new LockManager();
  }

  public ResourceManager(String p_name) {
    m_name = p_name;
    lockManager = new LockManager();
  }

  // Reads a data item
  protected RMItem readData(int xid, String key) {
    synchronized (m_data) {
      RMItem item = m_data.get(key);
      if (item != null) {
        return (RMItem) item.clone();
      }
      return null;
    }
  }

  // Writes a data item
  protected void writeData(int xid, String key, RMItem value) {
    synchronized (m_data) {
      m_data.put(key, value);
    }
  }

  // Remove the item out of storage
  protected void removeData(int xid, String key) {
    synchronized (m_data) {
      m_data.remove(key);
    }
  }

  // Deletes the item
  protected boolean deleteItem(int xid, String key) throws DeadlockException {
    Trace.info("RM::deleteItem(" + xid + ", " + key + ") called");
    lockManager.Lock(xid, key, TransactionLockObject.LockType.LOCK_WRITE);
    ReservableItem curObj = (ReservableItem) readData(xid, key);
    // Check if there is such an item in the storage
    if (curObj == null) {
      Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
      return false;
    } else {
      if (curObj.getReserved() == 0) {
        removeData(xid, curObj.getKey());
        Trace.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
        return true;
      } else {
        Trace.info(
            "RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
        return false;
      }
    }
  }

  // Query the number of available seats/rooms/cars
  protected int queryNum(int xid, String key) throws DeadlockException {
    Trace.info("RM::queryNum(" + xid + ", " + key + ") called");
    lockManager.Lock(xid, key, TransactionLockObject.LockType.LOCK_READ);
    ReservableItem curObj = (ReservableItem) readData(xid, key);
    int value = 0;
    if (curObj != null) {
      value = curObj.getCount();
    }
    Trace.info("RM::queryNum(" + xid + ", " + key + ") returns count=" + value);
    return value;
  }

  // Query the price of an item
  protected int queryPrice(int xid, String key) throws DeadlockException {
    Trace.info("RM::queryPrice(" + xid + ", " + key + ") called");
    lockManager.Lock(xid, key, TransactionLockObject.LockType.LOCK_READ);
    ReservableItem curObj = (ReservableItem) readData(xid, key);
    int value = 0;
    if (curObj != null) {
      value = curObj.getPrice();
    }
    Trace.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
    return value;
  }

  // Reserve an item
  protected boolean reserveItem(int xid, int customerID, String key, String location) throws DeadlockException {
    Trace.info("RM::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called");
    lockManager.Lock(xid, Integer.toString(customerID), TransactionLockObject.LockType.LOCK_READ);
    lockManager.Lock(xid, key, TransactionLockObject.LockType.LOCK_READ);
    Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
    if (customer == null) {
      Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location
          + ")  failed--customer doesn't exist");
      return false;
    }

    // Check if the item is available
    ReservableItem item = (ReservableItem) readData(xid, key);
    if (item == null) {
      Trace.warn(
          "RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
      return false;
    } else if (item.getCount() == 0) {
      Trace.warn(
          "RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
      return false;
    } else {
      customer.reserve(key, location, item.getPrice());
      writeData(xid, customer.getKey(), customer);

      // Decrease the number of available items in the storage
      item.setCount(item.getCount() - 1);
      item.setReserved(item.getReserved() + 1);
      writeData(xid, item.getKey(), item);

      Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
      return true;
    }
  }

  // Create a new flight, or add seats to existing flight
  // NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
  public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice)
      throws RemoteException, DeadlockException {
    Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
    lockManager.Lock(xid, Integer.toString(flightNum), TransactionLockObject.LockType.LOCK_WRITE);
    Flight curObj = (Flight) readData(xid, Flight.getKey(flightNum));

    if (curObj == null) { // not in database
      // Doesn't exist yet, add it
      Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
      
      // if flight not in pre_image
//      Map a = pre_image.get(xid);
//      if (!a.containsKey(Flight.getKey(flightNum))) { // NullPointerException
//        // set "previous" flight to null so that if the transaction aborts, the flight will still have a value
//        pre_image.get(xid).put(Flight.getKey(flightNum), null);
//      }
      
      addToWriteList(xid, newObj.getKey());
      writeData(xid, newObj.getKey(), newObj);
      Trace.info("RM::addFlight(" + xid + ") created new flight " + flightNum + ", seats=" + flightSeats + ", price=$"
          + flightPrice);
    } else { // in database
      addToWriteList(xid, curObj.getKey());
      
      // if flight not in pre_image
      //if (!pre_image.get(xid).containsKey(Flight.getKey(flightNum))) {
        addToPreImage(xid, curObj.getKey(), curObj);
      //}
      
      // Add seats to existing flight and update the price if greater than zero
      curObj.setCount(curObj.getCount() + flightSeats);
      if (flightPrice > 0) {
        curObj.setPrice(flightPrice);
      }
      writeData(xid, curObj.getKey(), curObj);
      Trace.info("RM::addFlight(" + xid + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount()
          + ", price=$" + flightPrice);
    }
    return true;
  }

  // Create a new car location or add cars to an existing location
  // NOTE: if price <= 0 and the location already exists, it maintains its current price
  public boolean addCars(int xid, String location, int count, int price) throws RemoteException, DeadlockException {
    Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
    lockManager.Lock(xid, location, TransactionLockObject.LockType.LOCK_WRITE);
    Car curObj = (Car) readData(xid, Car.getKey(location));
    if (curObj == null) {
      // Car location doesn't exist yet, add it
      Car newObj = new Car(location, count, price);
      
      // if flight not in pre_image
//      if (!pre_image.get(xid).containsKey(Car.getKey(location))) {
//        pre_image.get(xid).put(Car.getKey(location), null);
//      }
      
      addToWriteList(xid, newObj.getKey());
      Trace.info("RM::ADD TO WRITE LIST XID = " + xid + " KEY = " + newObj.getKey());
      writeData(xid, newObj.getKey(), newObj);
      Trace
          .info("RM::addCars(" + xid + ") created new location " + location + ", count=" + count + ", price=$" + price);
    } else {
      addToWriteList(xid, curObj.getKey());
      
      // if car not in pre_image
      //if (!pre_image.get(xid).containsKey(Car.getKey(location))) {
        addToPreImage(xid, curObj.getKey(), curObj);
      //}
      
      Trace.info("RM::ADD TO PRE IMAGE XID = " + xid + " KEY = " + curObj.getKey() + " OBJ = " + curObj);
      // Add count to existing car location and update price if greater than zero
      curObj.setCount(curObj.getCount() + count);
      if (price > 0) {
        curObj.setPrice(price);
      }
      Trace.info("RM::ADD TO WRITE LIST XID = " + xid + " KEY = " + curObj.getKey());
      writeData(xid, curObj.getKey(), curObj);
      Trace.info("RM::addCars(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount()
          + ", price=$" + price);
    }
    return true;
  }

  // Create a new room location or add rooms to an existing location
  // NOTE: if price <= 0 and the room location already exists, it maintains its current price
  public boolean addRooms(int xid, String location, int count, int price) throws RemoteException, DeadlockException {
    Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
    lockManager.Lock(xid, location, TransactionLockObject.LockType.LOCK_WRITE);
    Room curObj = (Room) readData(xid, Room.getKey(location));
    if (curObj == null) {
      // Room location doesn't exist yet, add it
      Room newObj = new Room(location, count, price);
      
      // if room not in pre_image
//      if (!pre_image.get(xid).containsKey(Room.getKey(location))) {
//        pre_image.get(xid).put(Room.getKey(location), null);
//      }
      
      addToWriteList(xid, newObj.getKey());
      writeData(xid, newObj.getKey(), newObj);
      Trace.info(
          "RM::addRooms(" + xid + ") created new room location " + location + ", count=" + count + ", price=$" + price);
    } else {
      addToWriteList(xid, curObj.getKey());
      
      // if room not in pre_image
      //if (!pre_image.get(xid).containsKey(Room.getKey(location))) {
        addToPreImage(xid, curObj.getKey(), curObj);
      //}
      
      // Add count to existing object and update price if greater than zero
      curObj.setCount(curObj.getCount() + count);
      if (price > 0) {
        curObj.setPrice(price);
      }
      writeData(xid, curObj.getKey(), curObj);
      Trace.info("RM::addRooms(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount()
          + ", price=$" + price);
    }
    return true;
  }

  // Deletes flight
  public boolean deleteFlight(int xid, int flightNum) throws RemoteException, DeadlockException {
    return deleteItem(xid, Flight.getKey(flightNum));
  }

  // Delete cars at a location
  public boolean deleteCars(int xid, String location) throws RemoteException, DeadlockException {
    return deleteItem(xid, Car.getKey(location));
  }

  // Delete rooms at a location
  public boolean deleteRooms(int xid, String location) throws RemoteException, DeadlockException {
    return deleteItem(xid, Room.getKey(location));
  }

  // Returns the number of empty seats in this flight
  public int queryFlight(int xid, int flightNum) throws RemoteException, DeadlockException {
    return queryNum(xid, Flight.getKey(flightNum));
  }

  // Returns the number of cars available at a location
  public int queryCars(int xid, String location) throws RemoteException, DeadlockException {
    return queryNum(xid, Car.getKey(location));
  }

  // Returns the amount of rooms available at a location
  public int queryRooms(int xid, String location) throws RemoteException, DeadlockException {
    return queryNum(xid, Room.getKey(location));
  }

  // Returns price of a seat in this flight
  public int queryFlightPrice(int xid, int flightNum) throws RemoteException, DeadlockException {
    return queryPrice(xid, Flight.getKey(flightNum));
  }

  // Returns price of cars at this location
  public int queryCarsPrice(int xid, String location) throws RemoteException, DeadlockException {
    return queryPrice(xid, Car.getKey(location));
  }

  // Returns room price at this location
  public int queryRoomsPrice(int xid, String location) throws RemoteException, DeadlockException {
    return queryPrice(xid, Room.getKey(location));
  }

  public String queryCustomerInfo(int xid, int customerID) throws RemoteException, DeadlockException {
    Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") called");
    lockManager.Lock(xid, Integer.toString(customerID), TransactionLockObject.LockType.LOCK_READ);
    Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
    if (customer == null) {
      Trace.warn("RM::queryCustomerInfo(" + xid + ", " + customerID + ") failed--customer doesn't exist");
      // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
      return "";
    } else {
      Trace.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ")");
      System.out.println(customer.getBill());
      return customer.getBill();
    }
  }

  public int newCustomer(int xid) throws RemoteException, DeadlockException {
    Trace.info("RM::newCustomer(" + xid + ") called");
    // Generate a globally unique ID for the new customer
    int cid = Integer.parseInt(String.valueOf(xid) + String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND))
        + String.valueOf(Math.round(Math.random() * 100 + 1)));
    Customer customer = new Customer(cid);
    lockManager.Lock(xid, Integer.toString(cid), TransactionLockObject.LockType.LOCK_WRITE);
    addToWriteList(xid, customer.getKey());
    writeData(xid, customer.getKey(), customer);
    Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
    return cid;
  }

  public boolean newCustomer(int xid, int customerID) throws RemoteException, DeadlockException {
    Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");
    Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
    if (customer == null) {
      customer = new Customer(customerID);
      lockManager.Lock(xid, Integer.toString(customerID), TransactionLockObject.LockType.LOCK_WRITE);
      addToWriteList(xid, customer.getKey());
      writeData(xid, customer.getKey(), customer);
      Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") created a new customer");
      return true;
    } else {
      Trace.info("INFO: RM::newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
      return false;
    }
  }

  public boolean deleteCustomer(int xid, int customerID) throws RemoteException, DeadlockException {
    Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
    lockManager.Lock(xid, Integer.toString(customerID), TransactionLockObject.LockType.LOCK_WRITE);
    Customer customer = (Customer) readData(xid, Customer.getKey(customerID));
    if (customer == null) {
      Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
      return false;
    } else {
      // Increase the reserved numbers of all reservable items which the customer reserved.
      RMHashMap reservations = customer.getReservations();
      for (String reservedKey : reservations.keySet()) {
        ReservedItem reserveditem = customer.getReservedItem(reservedKey);
        addToWriteList(xid, reservedKey);
        addToPreImage(xid, reservedKey, reserveditem);
        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " "
            + reserveditem.getCount() + " times");
        ReservableItem item = (ReservableItem) readData(xid, reserveditem.getKey());
        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey()
            + " which is reserved " + item.getReserved() + " times and is still available " + item.getCount()
            + " times");
        item.setReserved(item.getReserved() - reserveditem.getCount());
        item.setCount(item.getCount() + reserveditem.getCount());
        writeData(xid, item.getKey(), item);
      }

      // Remove the customer from the storage
      addToWriteList(xid, customer.getKey());
      addToPreImage(xid, customer.getKey(), customer);
      removeData(xid, customer.getKey());
      Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
      return true;
    }
  }

  // Adds flight reservation to this customer
  public boolean reserveFlight(int xid, int customerID, int flightNum) throws RemoteException, DeadlockException {
    return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
  }

  // Adds car reservation to this customer
  public boolean reserveCar(int xid, int customerID, String location) throws RemoteException, DeadlockException {
    return reserveItem(xid, customerID, Car.getKey(location), location);
  }

  // Adds room reservation to this customer
  public boolean reserveRoom(int xid, int customerID, String location) throws RemoteException, DeadlockException {
    return reserveItem(xid, customerID, Room.getKey(location), location);
  }

  // Reserve bundle
  public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car,
      boolean room) throws RemoteException {
    return false;
  }

  public String getName() throws RemoteException {
    return m_name;
  }

  @Override
  public boolean commit(int transactionId)
      throws RemoteException, TransactionAbortedException, InvalidTransactionException {
    Trace.info("RM::commit(" + transactionId + ") called");

    if (write_list.get(transactionId) != null) {
      write_list.get(transactionId).clear();
    }
    if (pre_image.get(transactionId) != null) {
      pre_image.get(transactionId).clear();
    }

    if (lockManager.UnlockAll(transactionId)) {
      Trace.info("RM::commit(" + transactionId + ") succeeded");
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void abort(int transactionId) throws RemoteException, InvalidTransactionException, DeadlockException {
    Trace.info("RM::abort(" + transactionId + ") called");

    if (write_list.get(transactionId) != null) {
      write_list.get(transactionId).forEach(key -> {
        removeData(transactionId, key);
      });
    }

    if (pre_image.get(transactionId) != null) {
      pre_image.get(transactionId).forEach((key, item) -> {
        writeData(transactionId, key, item);
      });

    }

    lockManager.UnlockAll(transactionId);
    Trace.info("RM::abort(" + transactionId + ") succeeded");
    return;

  }

  @Override
  public boolean shutdown() throws RemoteException {
    Trace.info("RM::shutdown() Shutting down server...");

    Timer shutdown = new Timer();
    shutdown.schedule(new TimerTask() {
      @Override
      public void run() {
        System.exit(0);
      }
    }, 3000);
    return true;
  }

  private void addToWriteList(int xid, String key) {
    List<String> list = write_list.get(xid);
    if (list == null) {
      list = new ArrayList<String>();
    }
    list.add(key);
    write_list.put(xid, list);
  }

  private void addToPreImage(int xid, String key, RMItem item) {
    Map<String, RMItem> map = pre_image.get(xid);
    if (map == null) {
      map = new HashMap<String, RMItem>();
    }
    map.put(key, (RMItem) item.clone());
    pre_image.put(xid, map);
  }
}
