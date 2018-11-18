package thing;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.ArrayList;

class Scope {
  
  public final Scope parent;
  private final Value.Object values;
  
  private Scope(Scope parent, Value.Object values) {
    this.parent = parent;
    this.values = values;
  }
  
  private Scope(Scope parent) {
    this(parent, Value.Object.of());
  }
  
  public static Scope empty() {
    var scope = new Scope(null);
    {
      scope.define("print", Value.Function.of(new Value[1], true, args -> {
        var i = 0;
        while (i < args.length) {
          if (i > 0) System.out.print(' ');
          System.out.print(args[i++]);
        }
        return args[i - 1];
      }), false);
      scope.define("println", Value.Function.of(null, true, args -> {
        var i = 0;
        while (i < args.length) {
          if (i > 0) System.out.print(' ');
          System.out.print(args[i++]);
        }
        System.out.println();
        return args[i - 1];
      }), false);
      scope.define("Symbol", Value.Object.of(
        Value.String.of("type"), Value.Symbol.type,
        Value.String.of("invoke"), Value.Symbol.invoke,
        Value.String.of("function"), Value.Symbol.function,
        Value.String.of("symbol"), Value.Symbol.symbol,
        Value.String.of("object"), Value.Symbol.object,
        Value.String.of("string"), Value.Symbol.string,
        Value.String.of("number"), Value.Symbol.number,
        Value.String.of("bool"), Value.Symbol.bool,
        Value.String.of("nil"), Value.Symbol.nil,
        Value.Symbol.invoke, Value.Function.of(new Value[] {Value.Nil.VALUE}, false, args -> new Value.Symbol(args[0] instanceof Value.Nil ? null : args[0].toString()))
      ), false);
      scope.define("getMetaobj", Value.Function.of(new Value[1], false, args -> args[0].getMetaobject()), false);
    }
    return scope;
  }
  
  public final Scope with(Value.Object object) {
    return new Scope(this, object) {
      @Override
      public Scope define(String name, Value value, boolean mutable) {
        return parent.define(name, value, mutable);
      }
    };
  }
  
  public final Scope child() {
    return new Scope(this);
  }
  
  public final Scope brother() {
    return parent.child();
  }
  
  public Value get(String name) {
    var value = values.get(Value.String.of(name));
    if (value == null) {
      if (parent != null) return parent.get(name);
      else throw new DataException("missing variable '" + name + '\'');
    }
    return value;
  }
  
  public Scope set(String name, Value value) {
    var key = Value.String.of(name);
    var present = false;
    try {
      present = values.set(key, value) == null;
    } catch (DataException e) {
      throw new DataException("can't set immutable variable '" + name + '\'');
    }
    if (present) {
      if (parent != null) parent.set(name, value);
      else throw new DataException("missing variable '" + name + '\'');
    }
    return this;
  }
  
  public Scope define(String name, Value value, boolean mutable) {
    var key = Value.String.of(name);
    if (values.define(key, value, mutable) == null) throw new DataException("duplicate variable '" + name + '\'');
    return this;
  }
  
}