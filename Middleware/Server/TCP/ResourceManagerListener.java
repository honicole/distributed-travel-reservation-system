package Server.TCP;

import java.net.Socket;

import Server.LockManager.DeadlockException;

public interface ResourceManagerListener {
  void onNewConnection(Socket socket) throws DeadlockException;
}
