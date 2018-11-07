package middleware;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import Server.LockManager.DeadlockException;

public interface MiddlewareListener {
  void onNewConnection(Socket socket) throws DeadlockException;

  boolean commit(int transactionId, String rm);

  void abort(int transactionId, String rm);
}
