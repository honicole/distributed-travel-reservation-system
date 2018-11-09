package middleware;

import java.net.Socket;
import Server.LockManager.DeadlockException;

public interface MiddlewareListener {
  void onNewConnection(Socket socket) throws DeadlockException;

  boolean commit(int transactionId, String rm);

  void abort(int transactionId, String rm);
}
