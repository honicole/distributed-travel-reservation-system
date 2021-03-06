package middleware;

import java.io.Serializable;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import Server.Common.Log;
import Server.Common.ShadowPage;
import Server.Common.Trace;
import exceptions.InvalidTransactionException;
import exceptions.TransactionAbortedException;

public class TransactionManager {
  private static final long TIMEOUT = 60000;
  private static final long RESPONSE_TIMEOUT = 10000;
  private int xid;
  private Map<Integer, Transaction> transactions;
  private Map<Integer, Timer> time_to_live;
  private MiddlewareListener middleware;
  private Executor executor = Executors.newFixedThreadPool(8);
  private static Log log = new Log();
  protected ShadowPage<Map<Integer, Transaction>> transaction_file;

  //@formatter:off
  /** <pre>{@code Crash mode based on an int as follows:
    1. Crash before sending vote request
    2. Crash after sending vote request and before receiving any replies
    3. Crash after receiving some replies but not all
    4. Crash after receiving all replies but before deciding
    5. Crash after deciding but before sending decision
    6. Crash after sending some but not all decisions
    7. Crash after having sent all decisions
    8. Recovery of the coordinator (if you have decided to implement coordinator recovery)}</pre>
   */
  private int crashMode = 0;
  //@formatter:on

  public TransactionManager(MiddlewareListener middleware) {
    this.middleware = middleware;
    this.transactions = new HashMap<>();
    this.time_to_live = new HashMap<>();
    this.transaction_file = new ShadowPage<>("transactions");
    initializeFiles();
    this.xid = transactions.isEmpty() ? 0 : Collections.max(transactions.keySet());
  }

  public enum Status {
    ACTIVE, PREPARING_COMMIT, COMMITTING, COMMITTED, ABORTING, ABORTED, TIME_OUT, INVALID
  }

  private static class Transaction implements Serializable {
    private static final long serialVersionUID = 3089530352465349341L;
    private HashSet<String> resourceManagersList;
    private HashSet<String> prepareToCommitList;
    private Status status;

    Transaction() {
      this.resourceManagersList = new HashSet<>();
      this.prepareToCommitList = new HashSet<>();
      this.status = Status.ACTIVE;
    }
  }

  public int start() throws RemoteException {
    transactions.put(++xid, new Transaction());
    Trace.info("TM::start() Starting new transaction [xid=" + xid + "]");
    return xid;
  }

  public boolean prepare(Socket socket, int transactionId)
      throws RemoteException, TransactionAbortedException, InvalidTransactionException {
    Transaction transaction = transactions.get(transactionId);

    setStatus(transactionId, Status.PREPARING_COMMIT);
    Trace.info("TM::prepare() Starting 2-phase commit protocol");
    log.write("TM\t" + transactionId + "\tSTART_2PC");
    transaction_file.save(transactions);

    crash(1);

    boolean prepare_to_commit = true;
    int yesVotes = 0;

    for (String rm : transaction.resourceManagersList) {
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        new Timer().schedule(new TimerTask() {
          @Override
          public void run() {
            crash(2);
          }
        }, 50);
        return middleware.prepare(socket, transactionId, rm);
      }, executor);

      try {
        Thread.sleep(100);
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      }

      try {
        boolean vote = (Boolean) future.get(RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS);
        crash(3);
        // Consensus required. One No vote is enough to veto
        if (vote)
          yesVotes++;
        else
          yesVotes = 0;
        prepare_to_commit &= vote;
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      } catch (TimeoutException e) {
        prepare_to_commit = false;
        yesVotes = 0;
      }
    }
    crash(4);

    if (prepare_to_commit && yesVotes > 0) {
      Trace.info("TM::prepare() decided to COMMIT");
      log.write("TM\t" + transactionId + "\tCOMMIT");
      transaction_file.save(transactions);
      crash(5);
      return commit(socket, transactionId);
    }

    Trace.info("TM::prepare() decided to ABORT");
    log.write("TM\t" + transactionId + "\tABORT");
    transaction_file.save(transactions);
    crash(5);
    abort(socket, transactionId);
    return false;
  }

  public boolean commit(Socket socket, int transactionId)
      throws RemoteException, TransactionAbortedException, InvalidTransactionException {
    Transaction transaction = transactions.get(transactionId);

    if (transaction == null) {
      throw new InvalidTransactionException("The transaction does not exist");
    }

    if (transaction.status != Status.ACTIVE && transaction.status != Status.PREPARING_COMMIT) {
      throw new InvalidTransactionException("Cannot commit the transaction.");
    }

    boolean committed = true;
    setStatus(transactionId, Status.COMMITTING);
    for (String rm : transaction.resourceManagersList) {
      CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> {
        new Timer().schedule(new TimerTask() {
          @Override
          public void run() {
            crash(6);
          }
        }, 50);
        return this.middleware.commit(socket, transactionId, rm);
      }, executor);

      try {
        Thread.sleep(100);
      } catch (InterruptedException e1) {
        e1.printStackTrace();
      }

      try {
        committed &= (Boolean) future.get();
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      }
    }
    crash(7);

    setStatus(transactionId, Status.COMMITTED);
    transaction_file.save(transactions);
    Trace.info("TM::commit(" + transactionId + ") Transaction committed");
    return true;
  }

  public boolean abort(Socket socket, int transactionId) throws RemoteException, InvalidTransactionException {
    Transaction transaction = transactions.get(transactionId);

    if (transaction == null) {
      throw new InvalidTransactionException("The transaction does not exist");
    }

    if (transaction.status != Status.ACTIVE && transaction.status != Status.PREPARING_COMMIT) {
      throw new InvalidTransactionException("Cannot abort the transaction.");
    }

    setStatus(transactionId, Status.ABORTING);

    HashSet<String> abortList = transaction.resourceManagersList;
    if (!transaction.prepareToCommitList.isEmpty()) {
      abortList = transaction.prepareToCommitList;
    }
    for (String rm : abortList) {
      this.middleware.abort(socket, transactionId, rm);
    }

    setStatus(transactionId, Status.ABORTED);
    transaction_file.save(transactions);
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

  public void addPrepareToCommit(int id, String resourceManager) throws RemoteException {
    Transaction transaction = transactions.get(id);
    if (!transaction.prepareToCommitList.contains(resourceManager)) {
      transaction.prepareToCommitList.add(resourceManager);
      Trace.info("TM::addPrepareToCommit(" + id + ", " + resourceManager + ") Resource manager voted YES");
    }
  }

  public void resetTimeToLive(Socket socket, int id) {
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
              abort(socket, id);
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
            // try {
            // //abort(socket, id);
            // // abort(id);
            // } catch (InvalidTransactionException e) {
            // e.printStackTrace();
            // } catch (RemoteException e) {
            // e.printStackTrace();
            // }
            setStatus(id, Status.TIME_OUT);
            Trace.info("TM::resetTimeToLive(" + id + ") Transaction timed out.");
          } else {
            time_to_live.remove(id);
          }
        }
      }, TIMEOUT);
      time_to_live.put(id, timer);
      Trace.info("TM::resetTimeToLive(" + id + ") resetting time to live.");
    }
  }

  public boolean resetCrashes() throws RemoteException {
    Trace.info("Resetting middleware crash mode");
    crashMode = 0;
    return true;
  }

  public void crashMiddleware(int mode) throws RemoteException {
    setCrashMode(mode);
  }

  /**
   * Crashes the transaction manager by calling {@code System.exit(1);} if the
   * crash mode is set to the given mode
   * 
   * @param mode
   */
  private void crash(int mode) {
    if (crashMode == mode) {
      Trace.info("Middleware crashed in mode " + mode + "!");
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
  public boolean setCrashMode(int crashMode) throws RemoteException {
    Trace.info("Setting crash mode to " + crashMode);
    this.crashMode = crashMode;
    return true;
  }

  private void initializeFiles() {
    Map<Integer, Transaction> load_transactions_from_file = transaction_file.load();

    if (load_transactions_from_file != null) {
      Trace.info("RM::initializeFiles() loaded transactions from file");
      transactions = load_transactions_from_file;
      continueTransactions();
    } else {
      transaction_file.save(transactions);
      Trace.info("RM::initializeFiles() initialized transactions file");
    }
  }

  private void continueTransactions() {
    try {
      transactions.forEach((xid, transaction) -> {
        switch (transaction.status) {
        case ACTIVE:
          resetTimeToLive(xid);
          // case PREPARING_COMMIT:
          // prepare(xid);
          // case ABORTED:
          // abort(xid);
        case ABORTING:
          // call abort on all servers
        case COMMITTING:
          // call commit on all
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
