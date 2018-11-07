package middleware;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public interface MiddlewareListener {
  void onNewConnection(Socket socket);

  boolean commit(int transactionId, String rm);

  void abort(int transactionId, String rm);
}
