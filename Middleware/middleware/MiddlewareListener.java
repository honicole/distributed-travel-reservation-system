package middleware;

import java.net.Socket;
import Server.LockManager.DeadlockException;

public interface MiddlewareListener {
  void onNewConnection(Socket socket) throws DeadlockException;

  boolean prepare(Socket clientSocket, int transactionId, String rm);

  boolean commit(Socket clientSocket, int transactionId, String rm);

  void abort(Socket clientSocket, int transactionId, String rm);
}
