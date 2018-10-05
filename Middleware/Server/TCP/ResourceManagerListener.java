package Server.TCP;

import java.net.Socket;

public interface ResourceManagerListener {
  void onNewConnection(Socket socket);
}
