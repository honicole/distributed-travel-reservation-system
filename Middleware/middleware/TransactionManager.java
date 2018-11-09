package middleware;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import Server.Common.Trace;
import exceptions.InvalidTransactionException;
import exceptions.TransactionAbortedException;

public class TransactionManager {
  private static final long TIMEOUT = 30000;
  private int xid;
  private Map<Integer, Transaction> transactions;
  private Map<Integer, Timer> time_to_live;
  private MiddlewareListener middleware;

  public TransactionManager(MiddlewareListener middleware) {
    this.middleware = middleware;
    this.xid = 0;
    this.transactions = new HashMap<>();
    this.time_to_live = new HashMap<>();
  }

  public enum Status {
    ACTIVE, COMMITTING, COMMITTED, ABORTING, ABORTED, TIME_OUT, INVALID
  }

  private static class Transaction implements Serializable {
    private static final long serialVersionUID = 3089530352465349341L;
    private HashSet<String> resourceManagersList;
    private Status status;

    Transaction() {
      this.resourceManagersList = new HashSet<>();
      this.status = Status.ACTIVE;
    }
  }

  public int start() throws RemoteException {
    transactions.put(++xid, new Transaction());
    Trace.info("TM::start() Starting new transaction [xid=" + xid + "]");
    return xid;
  }

  public boolean commit(int transactionId)
      throws RemoteException, TransactionAbortedException, InvalidTransactionException {
    Transaction transaction = transactions.get(transactionId);

    if (transaction == null) {
      throw new InvalidTransactionException("The transaction does not exist");
    }

    if (transaction.status != Status.ACTIVE) {
      throw new InvalidTransactionException("Cannot commit the transaction.");
    }

    setStatus(transactionId, Status.COMMITTING);
    Trace.info("TM::commit(" + transactionId + ") Transaction committing");

    // should check first if everything can be committed?
    for (String rm : transaction.resourceManagersList) {
      this.middleware.commit(transactionId, rm);
    }

    setStatus(transactionId, Status.COMMITTED);
    Trace.info("TM::commit(" + transactionId + ") Transaction committed");
    return true;
  }

  public boolean abort(int transactionId) throws RemoteException, InvalidTransactionException {
    Transaction transaction = transactions.get(transactionId);

    if (transaction == null) {
      throw new InvalidTransactionException("The transaction does not exist");
    }

    if (transaction.status != Status.ACTIVE) {
      throw new InvalidTransactionException("Cannot abort the transaction.");
    }

    setStatus(transactionId, Status.ABORTING);
    Trace.info("TM::abort(" + transactionId + ") Transaction aborting");

    // should check first if everything can be aborted?
    for (String rm : transaction.resourceManagersList) {
      this.middleware.abort(transactionId, rm);
    }

    setStatus(transactionId, Status.ABORTED);
    Trace.info("TM::abort(" + transactionId + ") Transaction aborted");
    return true;
  }

  public boolean shutdown() throws RemoteException {
    // TODO
    return false;
  }

  public void setStatus(int id, Status status) {
    if (transactions.containsKey(id)) {
      transactions.get(id).status = status;
      Trace.info("TM::setStatus(" + id + ", " + status + ") Status set");
    } else {
      Trace.info("TM::setStatus(" + id + ", " + status + ") Status not set -- transaction id does not exist");
    }
  }

  public Status getStatus(int id) {
    if (transactions.containsKey(id)) {
      return transactions.get(id).status;
    } else {
      return Status.INVALID;
    }
  }

  public void addResourceManager(int id, String resourceManager) throws RemoteException {
    Transaction transaction = transactions.get(id);
    if (!transaction.resourceManagersList.contains(resourceManager)) {
      transaction.resourceManagersList.add(resourceManager);
      Trace.info("TM::addResourceManager(" + id + ", " + resourceManager + ") Resource manager added");
    }
  }

  public void resetTimeToLive(int id) {
    if (transactions.containsKey(id)) {
      if (time_to_live.containsKey(id)) {
        time_to_live.get(id).cancel();
      }

      Timer timer = new Timer();
      timer.schedule(new TimerTask() {
        @Override
        public void run() {
          if (transactions.containsKey(id) && transactions.get(id).status == Status.ACTIVE) {
            try {
              abort(id);
            } catch (InvalidTransactionException e) {
              e.printStackTrace();
            } catch (RemoteException e) {
              e.printStackTrace();
            }
            setStatus(id, Status.TIME_OUT);
            Trace.info("TM::resetTimeToLive(" + id + ") Transaction timed out.");
          } else {
            time_to_live.remove(id);
          }
        }
      }, TIMEOUT);
      time_to_live.put(id, timer);
    }
  }
}
