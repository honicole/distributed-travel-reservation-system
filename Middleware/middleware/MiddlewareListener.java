package middleware;

import java.net.Socket;

public interface MiddlewareListener {
  void onNewConnection(Socket socket);
}
