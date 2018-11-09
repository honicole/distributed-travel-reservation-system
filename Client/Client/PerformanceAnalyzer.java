package Client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static Client.Command.*;

public class PerformanceAnalyzer extends TCPClient {
  
  private static String s_serverHost = "localhost";
  private static int s_serverPort = 1099;
  private static Executor executor = Executors.newFixedThreadPool(8);
  private Socket socket;
  private static Socket serverSocket;
  private ObjectOutputStream oos;
  private ObjectInputStream ois;
  
  private UserCommand[] commands = {
    new UserCommand(AddFlight, new String[] {"1", "1", "1", "100"}),
  };

  public PerformanceAnalyzer(Socket socket) throws Exception {
    super(socket);
    this.socket = socket;
  }

  public static void main(String[] args) {

    PerformanceAnalyzer performanceAnalyzer = null;
    try {
      performanceAnalyzer = new PerformanceAnalyzer(new Socket());
    } catch (Exception e) {
      e.printStackTrace();
    }
    performanceAnalyzer.runCommand();

    // Set the security policy
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new SecurityManager());
    }

  }
  
  @Override
  public void runCommand() {
    // Prepare for reading commands
    System.out.println();
    System.out.println("Preparing to run performance analysis...");

    try (Socket serverSocket = new Socket(s_serverHost, s_serverPort);
        ObjectOutputStream oos = new ObjectOutputStream(serverSocket.getOutputStream());
        ObjectInputStream ois = new ObjectInputStream(serverSocket.getInputStream());) {

      this.oos = oos;
      this.ois = ois;

      for (UserCommand uc: commands) {
        execute(uc.getCommand(), new Vector<String>(Arrays.asList(uc.getArgs())));
      }
      
    } catch (Exception e) {
      System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUncaught exception");
      e.printStackTrace();
      System.exit(1);
    }
  }

}
