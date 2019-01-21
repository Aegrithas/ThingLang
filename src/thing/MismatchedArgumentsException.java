package thing;

class MismatchedArgumentsException extends RuntimeException {
  
  public MismatchedArgumentsException() {}
  
  public MismatchedArgumentsException(String message) {
    super(message);
  }
  
  public MismatchedArgumentsException(String message, Throwable cause) {
    super(message, cause);
  }
  
  public MismatchedArgumentsException(Throwable cause) {
    super(cause);
  }
  
}