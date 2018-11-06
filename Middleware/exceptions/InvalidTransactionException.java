package exceptions;

public class InvalidTransactionException extends Exception {
  private static final long serialVersionUID = 4479680066461651890L;

  @Override
  public String getMessage() {
    return "Invalid transaction.";
  }
}
