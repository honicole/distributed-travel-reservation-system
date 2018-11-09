package exceptions;

public class InvalidTransactionException extends Exception {
  private static final long serialVersionUID = 4479680066461651890L;
  private String message;

  public InvalidTransactionException(String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
