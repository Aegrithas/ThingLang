package thing;

class TypeException extends Value.Exception {
  
  public TypeException(Value value) {
    super(value);
  }
  
  public TypeException(Value value, String message) {
    super(value, message);
  }
  
  public TypeException(String message) {
    super(message);
  }
  
}