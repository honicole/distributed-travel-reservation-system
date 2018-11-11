package Client;

import java.io.Serializable;
import java.util.Arrays;

public class UserCommand implements Serializable {

  private static final long serialVersionUID = -2648982057052896146L;
  private Command command;
  private String[] args;
  private long id = 0;

  public UserCommand(Command command, String[] args) {
    id = System.currentTimeMillis() + (long) (1000 * Math.random());
    this.command = command;
    this.args = args;
  }

  public Command getCommand() {
    return command;
  }

  public void setCommand(Command command) {
    this.command = command;
  }

  public String[] getArgs() {
    return args;
  }

  public void setArgs(String[] args) {
    this.args = args;
  }
  
  public String get(int index) {
    return args[index];
  }

  public long getId() {
    return id;
  }
  
  public String toString() {
    String result = Arrays.toString(args).replaceAll("\\s","");
    return result.substring(1, result.length()-1);
  }

}
