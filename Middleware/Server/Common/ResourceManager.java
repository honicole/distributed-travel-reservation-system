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

public class ResourceManager implements IResourceManager {
  protected String m_name = "";
  protected RMHashMap m_data = new RMHashMap();
  protected LockManager lockManager;
  protected Map<Integer, Map<String, RMItem>> write_list = new HashMap<>();
  protected Map<Integer, Map<String, RMItem>> pre_image = new HashMap<>();

  protected ShadowPage<RMHashMap> file_A;
  protected ShadowPage<RMHashMap> file_B;
  protected ShadowPage<MasterRecord> master_record_file;
  protected MasterRecord master_record = new MasterRecord();

  public static Log log = new Log();
  private int crashMode = 0;

  public ResourceManager() {
    lockManager = new LockManager();
  }

  public ResourceManager(String rm) {
    m_name = rm;
    lockManager = new LockManager();

    file_A = new ShadowPage<>(rm, "A");
    file_B = new ShadowPage<>(rm, "B");
    master_record_file = new ShadowPage<>(rm, "master_record");
    initializeFiles();
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
    ReservableItem curObj = write_list.get(xid) != null && write_list.get(xid).containsKey(key)
        ? (ReservableItem) write_list.get(xid).get(key)
        : (ReservableItem) readData(xid, key);
    // Check if there is such an item in the storage
    if (curObj == null) {
      Trace.warn("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
      return false;
    } else {
      if (curObj.getReserved() == 0) {
        addToWriteList(xid, curObj.getKey(), null);
        addToPreImage(xid, curObj.getKey(), curObj);
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
    ReservableItem curObj = write_list.get(xid) != null && write_list.get(xid).containsKey(key)
        ? (ReservableItem) write_list.get(xid).get(key)
        : (ReservableItem) readData(xid, key);
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
    ReservableItem curObj = write_list.get(xid) != null && write_list.get(xid).containsKey(key)
        ? (ReservableItem) write_list.get(xid).get(key)
        : (ReservableItem) readData(xid, key);
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
    lockManager.Lock(xid, key, TransactionLockObject.LockType.LOCK_READ);
    Customer customer = write_list.get(xid) != null && write_list.get(xid).containsKey(Customer.getKey(customerID))
        ? (Customer) write_list.get(xid).get(Customer.getKey(customerID))
        : (Customer) readData(xid, Customer.getKey(customerID));
    if (customer == null) {
      Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location
          + ")  failed--customer doesn't exist");
      return false;
    }

    // Check if the item is available
    ReservableItem item = write_list.get(xid) != null && write_list.get(xid).containsKey(key)
        ? (ReservableItem) write_list.get(xid).get(key)
        : (ReservableItem) readData(xid, key);
    if (item == null) {
      Trace.warn(
          "RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
      return false;
    } else if (item.getCount() == 0) {
      Trace.warn(
          "RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
      return false;
    } else {
      addToPreImage(xid, customer.getKey(), customer);
      customer.reserve(key, location, item.getPrice());
      addToWriteList(xid, customer.getKey(), customer);

      addToPreImage(xid, item.getKey(), item);
      // Decrease the number of available items in the storage
      item.setCount(item.getCount() - 1);
      item.setReserved(item.getReserved() + 1);
      addToWriteList(xid, item.getKey(), item);

      Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
      return true;
    }
  }

  // Create a new flight, or add seats to existing flight
  // NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
  public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice)
      throws RemoteException, DeadlockException {
    Trace.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
    lockManager.Lock(xid, Flight.getKey(flightNum), TransactionLockObject.LockType.LOCK_WRITE);
    Flight curObj = write_list.get(xid) != null && write_list.get(xid).containsKey(Flight.getKey(flightNum))
        ? (Flight) write_list.get(xid).get(Flight.getKey(flightNum))
        : (Flight) readData(xid, Flight.getKey(flightNum));

    if (curObj == null) {
      Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
      addToWriteList(xid, newObj.getKey(), newObj);
      addToPreImage(xid, newObj.getKey(), null);
      Trace.info("RM::addFlight(" + xid + ") created new flight " + flightNum + ", seats=" + flightSeats + ", price=$"
          + flightPrice);
    } else {
      addToPreImage(xid, curObj.getKey(), curObj);
      curObj.setCount(curObj.getCount() + flightSeats);
      if (flightPrice > 0) {
        curObj.setPrice(flightPrice);
      }
      addToWriteList(xid, curObj.getKey(), curObj);
      Trace.info("RM::addFlight(" + xid + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount()
          + ", price=$" + flightPrice);
    }
    return true;
  }

  // Create a new car location or add cars to an existing location
  // NOTE: if price <= 0 and the location already exists, it maintains its current price
  public boolean addCars(int xid, String location, int count, int price) throws RemoteException, DeadlockException {
    Trace.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
    lockManager.Lock(xid, Car.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
    Car curObj = write_list.get(xid) != null && write_list.get(xid).containsKey(Car.getKey(location))
        ? (Car) write_list.get(xid).get(Car.getKey(location))
        : (Car) readData(xid, Car.getKey(location));
    if (curObj == null) {
      // Car location doesn't exist yet, add it
      Car newObj = new Car(location, count, price);
      addToWriteList(xid, newObj.getKey(), newObj);
      addToPreImage(xid, newObj.getKey(), null);
      Trace
          .info("RM::addCars(" + xid + ") created new location " + location + ", count=" + count + ", price=$" + price);
    } else {
      addToPreImage(xid, curObj.getKey(), curObj);
      // Add count to existing car location and update price if greater than zero
      curObj.setCount(curObj.getCount() + count);
      if (price > 0) {
        curObj.setPrice(price);
      }
      addToWriteList(xid, curObj.getKey(), curObj);
      Trace.info("RM::addCars(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount()
          + ", price=$" + price);
    }
    return true;
  }

  // Create a new room location or add rooms to an existing location
  // NOTE: if price <= 0 and the room location already exists, it maintains its current price
  public boolean addRooms(int xid, String location, int count, int price) throws RemoteException, DeadlockException {
    Trace.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
    lockManager.Lock(xid, Room.getKey(location), TransactionLockObject.LockType.LOCK_WRITE);
    Room curObj = write_list.get(xid) != null && write_list.get(xid).containsKey(Room.getKey(location))
        ? (Room) write_list.get(xid).get(Room.getKey(location))
        : (Room) readData(xid, Room.getKey(location));
    if (curObj == null) {
      // Room location doesn't exist yet, add it
      Room newObj = new Room(location, count, price);
      addToWriteList(xid, newObj.getKey(), newObj);
      addToPreImage(xid, newObj.getKey(), null);
      Trace.info(
          "RM::addRooms(" + xid + ") created new room location " + location + ", count=" + count + ", price=$" + price);
    } else {
      addToPreImage(xid, curObj.getKey(), curObj);
      curObj.setCount(curObj.getCount() + count);
      if (price > 0) {
        curObj.setPrice(price);
      }
      addToWriteList(xid, curObj.getKey(), curObj);
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
    Customer customer = write_list.get(xid) != null && write_list.get(xid).containsKey(Customer.getKey(customerID))
        ? (Customer) write_list.get(xid).get(Customer.getKey(customerID))
        : (Customer) readData(xid, Customer.getKey(customerID));
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
    addToPreImage(xid, customer.getKey(), null);
    addToWriteList(xid, customer.getKey(), customer);
    Trace.info("RM::newCustomer(" + cid + ") returns ID=" + cid);
    return cid;
  }

  public boolean newCustomer(int xid, int customerID) throws RemoteException, DeadlockException {
    Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") called");
    Customer customer = write_list.get(xid) != null && write_list.get(xid).containsKey(Customer.getKey(customerID))
        ? (Customer) write_list.get(xid).get(Customer.getKey(customerID))
        : (Customer) readData(xid, Customer.getKey(customerID));
    if (customer == null) {
      customer = new Customer(customerID);
      addToWriteList(xid, customer.getKey(), customer);
      addToPreImage(xid, customer.getKey(), null);
      Trace.info("RM::newCustomer(" + xid + ", " + customerID + ") created a new customer");
      return true;
    } else {
      Trace.info("INFO: RM::newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
      return false;
    }
  }

  public boolean deleteCustomer(int xid, int customerID) throws RemoteException, DeadlockException {
    Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
    Customer customer = write_list.get(xid) != null && write_list.get(xid).containsKey(Customer.getKey(customerID))
        ? (Customer) write_list.get(xid).get(Customer.getKey(customerID))
        : (Customer) readData(xid, Customer.getKey(customerID));
    if (customer == null) {
      Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
      return false;
    } else {
      // Increase the reserved numbers of all reservable items which the customer reserved.
      RMHashMap reservations = customer.getReservations();
      for (String reservedKey : reservations.keySet()) {
        ReservedItem reserveditem = customer.getReservedItem(reservedKey);
        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " "
            + reserveditem.getCount() + " times");

        ReservableItem item = (ReservableItem) readData(xid, reserveditem.getKey());
        addToPreImage(xid, item.getKey(), item);
        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey()
            + " which is reserved " + item.getReserved() + " times and is still available " + item.getCount()
            + " times");
        item.setReserved(item.getReserved() - reserveditem.getCount());
        item.setCount(item.getCount() + reserveditem.getCount());
        addToWriteList(xid, item.getKey(), item);
      }

      // Remove the customer from the storage
      addToPreImage(xid, customer.getKey(), customer);
      addToWriteList(xid, customer.getKey(), null);
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

  public boolean prepare(int xid)
      throws RemoteException, TransactionAbortedException, InvalidTransactionException, DeadlockException {
    crash(1);

    if (write_list.get(xid).isEmpty()) {
      Trace.info("RM::prepare(" + xid + ") voted NO");
      log.write("RM-" + getName() + "\t" + xid + "\tABORT");
      crash(2);
      abort(xid);
      new Timer().schedule(new TimerTask() {
        @Override
        public void run() {
          crash(3);
        }
      }, 500);
      return false;
    }

    Trace.info("RM::prepare(" + xid + ") voted YES");
    log.write("RM-" + getName() + "\t" + xid + "\tYES");
    crash(2);
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        crash(3);
      }
    }, 500);
    return true;
  }

  @Override
  public boolean commit(int transactionId)
      throws RemoteException, TransactionAbortedException, InvalidTransactionException {
    crash(4);

    Trace.info("RM::commit(" + transactionId + ") called");

    synchronized (m_data) {
      write_list.get(transactionId).forEach((key, item) -> {
        if (item != null) {
          writeData(transactionId, key, item);
          Trace.info("RM::commit(" + transactionId + ") writing " + key + " to database");
        } else {
          removeData(transactionId, key);
          Trace.info("RM::commit(" + transactionId + ") removing " + key + " from database");
        }
      });
      if (master_record.getPointer() == file_A) {
        file_B.save(m_data);
        master_record.setPointer(file_B);
        Trace.info("RM::commit(" + transactionId + ") saved database to file_B");
      } else {
        file_A.save(m_data);
        master_record.setPointer(file_A);
        Trace.info("RM::commit(" + transactionId + ") saved database to file_A");
      }
      master_record.setId(transactionId);
      master_record_file.save(master_record);
      Trace.info("RM::commit(" + transactionId + ") saved master record");
    }

//    if (write_list.get(transactionId) != null) {
//      write_list.get(transactionId).clear();
//    }
//    if (pre_image.get(transactionId) != null) {
//      pre_image.get(transactionId).clear();
//    }

    if (lockManager.UnlockAll(transactionId)) {
      log.write("RM-" + getName() + "\t" + transactionId + "\tCOMMIT");
      Trace.info("RM::commit(" + transactionId + ") succeeded");
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void abort(int transactionId) throws RemoteException, InvalidTransactionException, DeadlockException {
    crash(4);

    Trace.info("RM::abort(" + transactionId + ") called");

//    if (write_list.get(transactionId) != null) {
//      write_list.get(transactionId).clear();
//    }
//    if (pre_image.get(transactionId) != null) {
//      pre_image.get(transactionId).clear();
//    }

    lockManager.UnlockAll(transactionId);
    log.write("RM-" + getName() + "\t" + transactionId + "\tABORT");
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

  private void addToWriteList(int xid, String key, RMItem item) {
    Map<String, RMItem> map = write_list.get(xid);
    if (map == null)
      map = new HashMap<String, RMItem>();
    map.put(key, (RMItem) item == null ? null : (RMItem) item.clone());
    write_list.put(xid, map);
  }

  private void addToPreImage(int xid, String key, RMItem item) {
    Map<String, RMItem> map = pre_image.get(xid);
    if (map == null)
      map = new HashMap<String, RMItem>();
    if (map.containsKey(key))
      return;
    map.put(key, (RMItem) item == null ? null : (RMItem) item.clone());
    pre_image.put(xid, map);
  }

  private void initializeFiles() {
    MasterRecord load_master_from_file = master_record_file.load();

    if (load_master_from_file != null) {
      master_record = load_master_from_file;
      Trace.info("RM::initializeFiles() loaded master record from file");
      RMHashMap load_from_file = master_record.getPointer().load();

      if (load_from_file != null) {
        m_data = load_from_file;
        Trace.info("RM::initializeFiles() loaded database from file");
      } else {
        file_A.save(m_data);
        file_B.save(m_data);
        Trace.info("RM::initializeFiles() initialized shadow files");
      }
    } else {
      master_record.setPointer(file_A);
      master_record_file.save(master_record);
      Trace.info("RM::initializeFiles() initialized master record file");
      file_A.save(m_data);
      file_B.save(m_data);
      Trace.info("RM::initializeFiles() initialized shadow files");
    }
  }

  public boolean resetCrashes() throws RemoteException {
    Trace.info("Resetting crash mode of " + m_name);
    crashMode = 0;
    return true;
  }

  /**
   * Sets the crash mode of the specified resource manager.
   * 
   * @param name Name of the resource manager
   * @param mode
   * @throws RemoteException
   */
  public boolean crashResourceManager(String name, int mode) throws RemoteException {
    if (m_name.equals(name)) {
      Trace.info("Setting crash mode of " + name + " to " + mode);
      setCrashMode(mode);
    }
    return true;
  }

  /**
   * Crashes the transaction manager by calling {@code System.exit(1);} if the crash mode is set to the given mode
   * 
   * @param mode
   */
  private void crash(int mode) {
    if (crashMode == mode) {
      Trace.info(m_name + " crashed in mode " + mode + "!");
      System.exit(1);
    }
  }

  /**
   * @return the crashMode
   */
  public int getCrashMode() {
    return crashMode;
  }

  /**
   * @param crashMode the crashMode to set
   */
  public void setCrashMode(int crashMode) {
    this.crashMode = crashMode;
  }

}
