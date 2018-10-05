package Client;

import java.io.Serializable;

public class UserCommand implements Serializable {


  private static final long serialVersionUID = -2648982057052896146L;
  private Command command;
  private String[] args;
  private int id = 0;
  

  public UserCommand(Command command, String[] args) {
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
  
  public int getId() {
    return id;
  }
  
}
