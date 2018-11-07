package middleware;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import Server.Common.ResourceManager;
import Server.Common.Trace;
import exceptions.InvalidTransactionException;
import exceptions.TransactionAbortedException;

public class TransactionManager {
  private int xid;
  private Map<Integer, Transaction> transactions;

  public TransactionManager() {
    this.xid = 0;
    this.transactions = new HashMap<>();
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

    for (String rm : transaction.resourceManagersList) {
    }

    setStatus(transactionId, Status.COMMITTED);
    Trace.info("TM::commit(" + transactionId + ") Transaction committed");
    return true;
  }

  public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
    Transaction transaction = transactions.get(transactionId);

    if (transaction == null) {
      throw new InvalidTransactionException("The transaction does not exist");
    }

    // if transaction not committed/committing/invalid

    // set transaction status to ABORTING
    setStatus(transactionId, Status.ABORTING);
    Trace.info("TM::abort(" + transactionId + ") Transaction aborting");

    for (String rm : transaction.resourceManagersList) {
      // send abort request
    }

    // set transaction status to ABORTED
    setStatus(transactionId, Status.ABORTING);

    Trace.info("TM::abort(" + transactionId + ") Transaction aborted");
    return;
  }

  public boolean shutdown() throws RemoteException {
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
    transactions.get(id).resourceManagersList.add(resourceManager);
    Trace.info("TM::addResourceManager(" + id + ", " + resourceManager + ") Resource manager added");
  }

  public void resetTimeToLive(int id) {
    Trace.info("TM::resetTimeToLive(" + id + ") Time-to-live reset to 0");
  }
}
