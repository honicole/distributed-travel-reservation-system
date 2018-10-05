package Client;

import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Vector;

import Server.Interface.IResourceManager;

public abstract class Client {
  IResourceManager m_resourceManager = null;
  private Socket socket;

  public Client() {
  }
  

  public static Vector<String> parse(String command) {
    Vector<String> arguments = new Vector<String>();
    StringTokenizer tokenizer = new StringTokenizer(command, ",");
    String argument = "";
    while (tokenizer.hasMoreTokens()) {
      argument = tokenizer.nextToken();
      argument = argument.trim();
      arguments.add(argument);
    }
    return arguments;
  }

  public static void checkArgumentsCount(Integer expected, Integer actual) throws IllegalArgumentException {
    if (expected != actual) {
      throw new IllegalArgumentException("Invalid number of arguments. Expected " + (expected - 1) + ", received "
          + (actual - 1) + ". Location \"help,<CommandName>\" to check usage of this command");
    }
  }

  public static int toInt(String string) throws NumberFormatException {
    return Integer.valueOf(string);
  }

  public static boolean toBoolean(String string) {
    return Boolean.valueOf(string);
  }
}
