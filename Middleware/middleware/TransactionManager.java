package middleware;

import java.io.Serializable;
import java.rmi.RemoteException;

import exceptions.InvalidTransactionException;
import exceptions.TransactionAbortedException;

public class TransactionManager {
  
  public TransactionManager() {
    
  }

  private static class Transaction implements Serializable {
  }


  public int start() throws RemoteException {
    return 0;
  }

  public boolean commit(int transactionId)
      throws RemoteException, TransactionAbortedException, InvalidTransactionException {
    return false;
  }

  public void abort(int transactionId) throws RemoteException, InvalidTransactionException {
    return;
  }

  public boolean shutdown() throws RemoteException {
    return false;
  }
}
