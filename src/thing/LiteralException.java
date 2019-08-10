package thing;

class LiteralException extends RuntimeException {
  
  public LiteralException() {}
  
  public LiteralException(String message) {
    super(message);
  }
  
  public LiteralException(String message, Throwable cause) {
    super(message, cause);
  }
  
  public LiteralException(Throwable cause) {
    super(cause);
  }
  
}