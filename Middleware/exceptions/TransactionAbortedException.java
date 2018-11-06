package exceptions;

public class TransactionAbortedException extends Exception {
  private static final long serialVersionUID = -8589680263887354552L;
  
  @Override
  public String getMessage() {
    return "Transaction aborted.";
  }

}
