package thing;

import java.io.PrintStream;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.function.Supplier;

abstract class Value {
  
  private Value() {}
  
  public abstract <R> R match(Function.Matcher<R> function, Symbol.Matcher<R> symbol, Object.Matcher<R> object, String.Matcher<R> string, Number.Matcher<R> number, Bool.Matcher<R> bool, Nil.Matcher<R> nil);
  
  public abstract Symbol getType();
  
  public abstract Object getMetaobject();
  
  @Override
  public abstract java.lang.String toString();
  
  @Override
  public abstract boolean equals(java.lang.Object o);
  
  @Override
  public abstract int hashCode();
  
  public static abstract class Function extends Value {
    
    public static final java.lang.String THIS = "this";
    public static final Object meta = Object.of(
      String.of("apply"), Function.of(new Value[1], true, (thisArg, args) -> {
        var rest = new ArrayList<Value>();
        for (var i = 1; i < args.length; i++) rest.add(args[i]);
        return functor(thisArg).apply(args[0], rest);
      })
    );
    
    private Function() {}
    
    public static Function of(ASTEvaluator evaluator, java.lang.String[] params, Value[] defaults, java.lang.String varargs, AST body, Scope scope) {
      return new Defined(evaluator, params, defaults, varargs, body, scope);
    }
    
    public static Function of(Value[] params, boolean varargs, NativeThisFunction body) {
      return new Native(params == null ? new Value[0] : params, varargs, body);
    }
    
    public static Function of(Value[] params, boolean varargs, NativeFunction body) {
      return of(params, varargs, (NativeThisFunction) body);
    }
    
    public static Function functor(Value value) {
      if (value instanceof Function) return (Function) value;
      if (value instanceof Object) return ((Object) value).getInvoke();
      return value.getMetaobject().getInvoke();
    }
    
    public Value call(Iterable<Value> args) {
      return apply(null, args);
    }
    
    public abstract Value apply(Value thisArg, Iterable<Value> args);
    
    @Override
    public abstract java.lang.String toString();
    
    @Override
    public <R> R match(Function.Matcher<R> function, Symbol.Matcher<R> symbol, Object.Matcher<R> object, String.Matcher<R> string, Number.Matcher<R> number, Bool.Matcher<R> bool, Nil.Matcher<R> nil) {
      return function == null ? null : function.match(this);
    }
    
    @Override
    public Symbol getType() {
      return Symbol.function;
    }
    
    @Override
    public Object getMetaobject() {
      return meta;
    }
    
    @Override
    public boolean equals(java.lang.Object o) {
      return this == o;
    }
    
    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }
    
    private static final class Defined extends Function {
      
      private final ASTEvaluator evaluator;
      private final java.lang.String[] params;
      private final Value[] defaults;
      private final java.lang.String varargs;
      private final AST body;
      private final Scope scope;
      
      public Defined(ASTEvaluator evaluator, java.lang.String[] params, Value[] defaults, java.lang.String varargs, AST body, Scope scope) {
        this.evaluator = evaluator;
        this.params = params;
        this.defaults = defaults;
        this.varargs = varargs;
        this.body = body;
        this.scope = scope;
        if (params.length < defaults.length) throw new MismatchedArgumentsException("too many default args");
        var hasDefault = false;
        for (var param : defaults) {
          if (param == null) {
            if (hasDefault) throw new MismatchedArgumentsException("non-trailing default argument(s)");
          } else {
            hasDefault = true;
          }
        }
      }
      
      public Value apply(Value thisArg, Iterable<Value> args) {
        Scope scope = this.scope.child(), oldScope = evaluator.scope;
        var varargs = this.varargs == null ? null : Object.of();
        evaluator.scope = scope;
        scope.define(THIS, thisArg == null ? Value.Nil.VALUE : thisArg, false);
        var i = 0;
        for (var arg : args) {
          if (i >= params.length) {
            if (varargs == null) throw new MismatchedArgumentsException("too many");
            else varargs.define(Number.of(i++ - params.length), arg, true);
            continue;
          }
          scope.define(params[i++], arg, true);
        }
        if (varargs != null) scope.define(this.varargs, varargs, true);
        final var diff = params.length - defaults.length;
        while (i < params.length) {
          if (defaults[i - diff] == null) throw new MismatchedArgumentsException("too few");
          scope.define(params[i], defaults[i++ - diff], true);
        }
        Value value = null;
        try {
          value = body.accept(evaluator);
        } catch (AST.JumpNode.Exception e) {
          if (e.type == AST.JumpNode.Type.RETURN) value = e.value;
          else if (e.type == AST.JumpNode.Type.THROW) throw e;
          else throw new Value.Exception('\'' + e.type.name().toLowerCase() + "' outside of valid context");
        } catch (SpreadException e) {
          throw new Value.Exception("'spread' outside of valid context");
        } finally {
          evaluator.scope = oldScope;
        }
        return value;
      }
      
      @Override
      public java.lang.String toString() {
        var builder = new StringBuilder().append("Function(");
        var first = true;
        for (var param : params) {
          if (first) first = false;
          else builder.append(", ");
          builder.append(param);
        }
        if (varargs != null) {
          if (!first) builder.append(", ");
          builder.append(varargs).append("...");
        }
        return builder.append(')').toString();
      }
      
    }
    
    private static final class Native extends Function {
      
      private final Value[] params;
      private final boolean varargs;
      private final NativeThisFunction body;
      
      public Native(Value[] params, boolean varargs, NativeThisFunction body) {
        this.params = params;
        this.varargs = varargs;
        this.body = body;
        var hasDefault = false;
        for (var param : params) {
          if (param == null) {
            if (hasDefault) throw new MismatchedArgumentsException("non-trailing default argument(s)");
          } else {
            hasDefault = true;
          }
        }
      }
      
      public Value apply(Value thisArg, Iterable<Value> args) {
        var argv = new ArrayList<Value>();
        args.forEach(argv::add);
        var size = argv.size();
        while (size < params.length) {
          if (params[size] == null) throw new MismatchedArgumentsException("too few");
          argv.add(params[size++]);
        }
        if (size > params.length && !varargs) throw new MismatchedArgumentsException("too many");
        return body.call(thisArg == null ? Value.Nil.VALUE : thisArg, argv.toArray(new Value[size]));
      }
      
      @Override
      public java.lang.String toString() {
        var builder = new StringBuilder().append("Function([native]");
        var argc = 0;
        for (var param : params) if (param == null) argc++;
        else break;
        if (argc > 0) builder.append(' ').append(argc);
        var optc = params.length - argc;
        if (optc > 0) builder.append(' ').append(optc).append('?');
        if (varargs) builder.append(" ...");
        return builder.append(')').toString();
      }
      
    }
    
    public interface NativeThisFunction {
      
      Value call(Value thisArg, Value... values);
      
    }
    
    public interface NativeFunction extends NativeThisFunction {
      
      Value call(Value... values);
      
      default Value call(Value thisArg, Value... values) {
        return call(values);
      }
      
    }
    
    public interface Matcher<R> {
      
      R match(Function function);
      
    }
    
  }
  
  public static final class Symbol extends Value {
    
    public static final Symbol
      type = new Symbol("type"),
      invoke = new Symbol("invoke"),
      function = new Symbol("function"),
      symbol = new Symbol("symbol"),
      object = new Symbol("object"),
      string = new Symbol("string"),
      number = new Symbol("number"),
      bool = new Symbol("bool"),
      nil = new Symbol("nil");
    public static final Object meta = Object.of(
      String.of("getName"), Function.of(null, false, (thisArg, args) -> {
        if (thisArg instanceof Symbol) return String.of(((Symbol) thisArg).name);
        throw new TypeException("expected a symbol");
      })
    );
    public final java.lang.String name;
    
    public Symbol(java.lang.String name) {
      this.name = name;
    }
    
    @Override
    public <R> R match(Function.Matcher<R> function, Symbol.Matcher<R> symbol, Object.Matcher<R> object, String.Matcher<R> string, Number.Matcher<R>  number, Bool.Matcher<R> bool, Nil.Matcher<R> nil) {
      return symbol == null ? null : symbol.match(this);
    }
    
    @Override
    public Symbol getType() {
      return Symbol.symbol;
    }
    
    @Override
    public Object getMetaobject() {
      return meta;
    }
    
    @Override
    public java.lang.String toString() {
      return "Symbol(" + (name == null ? hashCode() : name) + ')';
    }
    
    @Override
    public boolean equals(java.lang.Object o) {
      return this == o;
    }
    
    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }
    
    public interface Matcher<R> {
      
      R match(Symbol symbol);
      
    }
    
  }
  
  public static final class Object extends Value {
    
    public final Object meta;
    private final Map<Value, MutableValue> entries;
    
    private Object(Object meta, Map<Value, MutableValue> entries) {
      this.meta = meta;
      this.entries = entries;
    }
    
    public static Object of(Value... values) {
      Object meta = null;
      var i = 0;
      if (values.length % 2 != 0) {
        var value = values[i++];
        if (value == null || value instanceof Object) meta = (Object) value;
        else throw new TypeException("Metaobjects must be objects");
      }
      var entries = new HashMap<Value, MutableValue>();
      while (i < values.length) entries.put(values[i++], new MutableValue(values[i++], false));
      return new Object(meta, entries);
    }
    
    public static Iterable<Value> iterable(Value value) {
      if (!(value instanceof Object)) throw new TypeException("only arrays can be iterated upon");
      var object = (Object) value;
      return () -> new Iterator<>() {
        private int i = 0;
        
        public boolean hasNext() {
          return object.hasSelf(Number.of(i));
        }
        
        public Value next() {
          return object.getSelf(Number.of(i++));
        }
      };
    }
    
    public boolean has(Value key) {
      return entries.containsKey(key) || (meta != null && meta.has(key));
    }
    
    public boolean hasSelf(Value key) {
      return entries.containsKey(key);
    }
    
    public Value get(Value key) {
      var value = entries.get(key);
      if (value == null) return meta == null ? null : meta.get(key);
      return value.value;
    }
    
    public Value getSelf(Value key) {
      var value = entries.get(key);
      return value == null ? null : value.value;
    }
    
    public Value set(Value key, Value value) {
      var entry = entries.get(key);
      if (entry != null) {
        if (entry.mutable) entry.value = value;
        else throw new DataException("can't set immutable field");
      } else if (meta != null) {
        meta.set(key, value);
      } else {
        return null;
      }
      return value;
    }
    
    public Value setSelf(Value key, Value value) {
      var entry = entries.get(key);
      if (entry == null) return null;
      if (entry.mutable) return entry.value = value;
      throw new DataException("can't set immutable field");
    }
    
    public Value define(Value key, Value value, boolean mutable) {
      if (entries.containsKey(key)) return null;
      entries.put(key, new MutableValue(value, mutable));
      return value;
    }
    
    public boolean isEmpty() {
      return entries.isEmpty();
    }
    
    public Function getInvoke() {
      var invoke = get(Symbol.invoke);
      if (invoke instanceof Function) return (Function) invoke;
      if (invoke instanceof Object) return ((Object) invoke).getInvoke();
      return null;
    }
    
    @Override
    public <R> R match(Function.Matcher<R> function, Symbol.Matcher<R> symbol, Object.Matcher<R> object, String.Matcher<R> string, Number.Matcher<R> number, Bool.Matcher<R> bool, Nil.Matcher<R> nil) {
      return object == null ? null : object.match(this);
    }
    
    @Override
    public Symbol getType() {
      var value = get(Symbol.type);
      if (value == null) value = Symbol.object;
      if (!(value instanceof Symbol)) throw new TypeException("an object's type must be a symbol");
      return (Symbol) value;
    }
    
    @Override
    public Object getMetaobject() {
      return meta;
    }
    
    @Override
    public java.lang.String toString() {
      var builder = new StringBuilder().append('[');
      var first = true;
      for (var entry : entries.entrySet()) {
        if (first) first = false;
        else builder.append(", ");
        builder.append(toString(entry.getKey())).append(": ").append(toString(entry.getValue()));
      }
      return builder.append(']').toString();
    }
    
    private java.lang.String toString(Value value) {
      return value instanceof String ? '"' + value.toString() + '"' : value.toString();
    }
    
    private java.lang.String toString(MutableValue value) {
      return toString(value.value);
    }
    
    @Override
    public boolean equals(java.lang.Object o) {
      return o instanceof Object && entries.equals(((Object) o).entries);
    }
    
    @Override
    public int hashCode() {
      return entries.hashCode();
    }
    
    private final static class MutableValue {
      
      public Value value;
      public final boolean mutable;
      
      public MutableValue(Value value, boolean mutable) {
        this.value = value;
        this.mutable = mutable;
      }
      
      @Override
      public boolean equals(java.lang.Object o) {
        return o instanceof MutableValue && value.equals(((MutableValue) o).value);
      }
      
      @Override
      public int hashCode() {
        return value.hashCode();
      }
      
    }
    
    public interface Matcher<R> {
      
      R match(Object object);
      
    }
    
  }
  
  public static final class String extends Value {
    
    public static final Object META = Object.of(
      String.of("indexOf"), Function.of(new Value[1], false, (thisArg, args) -> {
        if (thisArg instanceof String && args[0] instanceof String) return Number.of(((String) thisArg).value.indexOf(args[0].toString()));
        throw new TypeException("expected a string");
      })
    );
    public static final String EMPTY = new String("");
    public final java.lang.String value;
    
    private String(java.lang.String value) {
      this.value = value;
    }
    
    public static String of(java.lang.String value) {
      return value.length() == 0 ? EMPTY : new String(value);
    }
    
    @Override
    public <R> R match(Function.Matcher<R> function, Symbol.Matcher<R> symbol, Object.Matcher<R> object, String.Matcher<R> string, Number.Matcher<R> number, Bool.Matcher<R> bool, Nil.Matcher<R> nil) {
      return string == null ? null : string.match(this);
    }
    
    @Override
    public Symbol getType() {
      return Symbol.string;
    }
    
    @Override
    public Object getMetaobject() {
      return META;
    }
    
    @Override
    public java.lang.String toString() {
      return value;
    }
    
    @Override
    public boolean equals(java.lang.Object o) {
      return o instanceof String && value.equals(((String) o).value);
    }
    
    @Override
    public int hashCode() {
      return value.hashCode();
    }
    
    public interface Matcher<R> {
      
      R match(String string);
      
    }
    
  }
  
  public static final class Number extends Value {
    
    public static final Object META = Object.of(
      Symbol.invoke, Function.of(new Value[1], false, (thisArg, args) -> BinOpFunc.MUL.operate(thisArg, args[0]))
    );
    public static final Number ZERO = new Number(0), ONE = new Number(1), NaN = new Number(Double.NaN), INFINITY = new Number(Double.POSITIVE_INFINITY), NEGATIVE_INFINITY = new Number(Double.NEGATIVE_INFINITY);
    public final double value;
    
    private Number(double value) {
      this.value = value;
    }
    
    public static Number of(double value) {
      if (value == 0) return ZERO;
      if (value == 1) return ONE;
      if (value == Double.NaN) return NaN;
      if (value == Double.POSITIVE_INFINITY) return INFINITY;
      if (value == Double.NEGATIVE_INFINITY) return NEGATIVE_INFINITY;
      return new Number(value);
    }
    
    public static Number of(Value value) {
      return value.match(
        function -> new TypeException("can't convert a function to a number").throwValue(),
        symbol -> new TypeException("can't convert a symbol to a number").throwValue(),
        object -> new TypeException("can't convert an object to a number").throwValue(),
        string -> new TypeException("can't convert a string to a number").throwValue(),
        number -> number,
        bool -> bool.value ? ONE : ZERO,
        nil -> ZERO
      );
    }
    
    public Number negative() {
      return new Number(-value);
    }
    
    @Override
    public <R> R match(Function.Matcher<R> function, Symbol.Matcher<R> symbol, Object.Matcher<R> object, String.Matcher<R> string, Number.Matcher<R> number, Bool.Matcher<R> bool, Nil.Matcher<R> nil) {
      return number == null ? null : number.match(this);
    }
    
    @Override
    public Symbol getType() {
      return Symbol.number;
    }
    
    @Override
    public Object getMetaobject() {
      return META;
    }
    
    @Override
    public java.lang.String toString() {
      return (value % 1) == 0 ? Long.toString((long) value) : Double.toString(value);
    }
    
    @Override
    public boolean equals(java.lang.Object o) {
      return o instanceof Number && value == ((Number) o).value;
    }
    
    @Override
    public int hashCode() {
      return Double.hashCode(value);
    }
    
    public interface Matcher<R> {
      
      R match(Number number);
      
    }
    
  }
  
  public static final class Bool extends Value {
    
    public static final Object META = Object.of(
      Symbol.invoke, Function.of(new Value[1], false, (thisArg, args) -> BinOpFunc.MUL.operate(thisArg, args[0]))
    );
    public static final Bool TRUE = new Bool(true), FALSE = new Bool(false);
    public final boolean value;
    
    private Bool(boolean value) {
      this.value = value;
    }
    
    public static Bool of(boolean bool) {
      return bool ? TRUE : FALSE;
    }
    
    public static Bool of(Value value) {
      return value.match(
        function -> TRUE,
        symbol -> TRUE,
        object -> of(!object.entries.isEmpty()),
        string -> of(string.value.length() != 0),
        number -> of(number.value != 0),
        bool -> bool,
        nil -> FALSE
      );
    }
    
    public Bool not() {
      return value ? FALSE : TRUE;
    }
    
    @Override
    public <R> R match(Function.Matcher<R> function, Symbol.Matcher<R> symbol, Object.Matcher<R> object, String.Matcher<R> string, Number.Matcher<R> number, Bool.Matcher<R> bool, Nil.Matcher<R> nil) {
      return bool == null ? null : bool.match(this);
    }
    
    @Override
    public Symbol getType() {
      return Symbol.bool;
    }
    
    @Override
    public Object getMetaobject() {
      return META;
    }
    
    @Override
    public java.lang.String toString() {
      return Boolean.toString(value);
    }
    
    @Override
    public boolean equals(java.lang.Object o) {
      return this == o;
    }
    
    @Override
    public int hashCode() {
      return value ? 1 : 0;
    }
    
    public interface Matcher<R> {
      
      R match(Bool bool);
      
    }
    
  }
  
  public static final class Nil extends Value {
    
    public static final Object META = Object.of(
      Symbol.invoke, Function.of(null, true, (thisArg, args) -> thisArg)
    );
    public static final Nil VALUE = new Nil();
    
    private Nil() {}
    
    @Override
    public <R> R match(Function.Matcher<R> function, Symbol.Matcher<R> symbol, Object.Matcher<R> object, String.Matcher<R> string, Number.Matcher<R> number, Bool.Matcher<R> bool, Nil.Matcher<R> nil) {
      return nil == null ? null : nil.match(this);
    }
    
    @Override
    public Symbol getType() {
      return Symbol.nil;
    }
    
    @Override
    public Object getMetaobject() {
      return META;
    }
    
    @Override
    public java.lang.String toString() {
      return "nil";
    }
    
    @Override
    public boolean equals(java.lang.Object o) {
      return this == VALUE;
    }
    
    @Override
    public int hashCode() {
      return 0;
    }
    
    public interface Matcher<R> {
      
      R match(Nil nil);
      
    }
    
  }
  
  public static class Exception extends RuntimeException {
    
    public final Value value;
    
    public Exception(Value value) {
      this.value = value;
    }
    
    public Exception(Value value, java.lang.String message) {
      super(message);
      this.value = value;
    }
    
    public Exception(java.lang.String message) {
      this(null, message);
    }
    
    public final <V extends Value> V throwValue() {
      throw this;
    }
    
  }
  
  @FunctionalInterface
  public interface BinOpFunc {
    
    public static final BinOpFunc
      SEQUENCE = (left, right) -> right,
      ADD = (left, right) -> left.match(
        function -> right instanceof String ? new String(function + ((String) right).value) : right instanceof Nil ? right : badOpType("add"),
        symbol -> right instanceof String ? new String(symbol + ((String) right).value) : right instanceof Nil ? right : badOpType("add"),
        object -> right instanceof String ? new String(object + ((String) right).value) : right instanceof Nil ? right : badOpType("add"),
        string -> right instanceof Nil ? right : new String(string.value + right),
        number -> right.match(
          function -> badOpType("add"),
          symbol -> badOpType("add"),
          object -> badOpType("add"),
          string -> new String(number + string.value),
          number2 -> new Number(number.value + number2.value),
          bool -> bool.value ? new Number(number.value + 1) : number,
          nil -> nil
        ),
        bool -> right.match(
          function -> badOpType("add"),
          symbol -> badOpType("add"),
          object -> badOpType("add"),
          string -> new String(bool + string.value),
          number -> bool.value ? new Number(number.value + 1) : number,
          bool2 -> new Bool(bool.value | bool2.value),
          nil -> nil
        ),
        nil -> right instanceof String ? new String(nil + ((String) right).value) : nil
      ),
      SUB = (left, right) -> left.match(
        function -> right instanceof Nil ? right : badOpType("sub"),
        symbol -> right instanceof Nil ? right : badOpType("sub"),
        object -> right instanceof Nil ? right : badOpType("sub"),
        string -> right instanceof Nil ? right : badOpType("sub"),
        number -> right.match(
          function -> badOpType("sub"),
          symbol -> badOpType("sub"),
          object -> badOpType("sub"),
          string -> badOpType("sub"),
          number2 -> new Number(number.value - number2.value),
          bool -> bool.value ? new Number(number.value - 1) : number,
          nil -> nil
        ),
        bool -> right.match(
          function -> badOpType("sub"),
          symbol -> badOpType("sub"),
          object -> badOpType("sub"),
          string -> badOpType("sub"),
          number -> new Number((bool.value ? 1 : 0) - number.value),
          bool2 -> new Bool(bool.value ^ bool2.value),
          nil -> nil
        ),
        nil -> nil
      ),
      MUL = (left, right) -> left.match(
        function -> right instanceof Bool ? ((Bool) right).value ? function : Nil.VALUE : right instanceof Nil ? Nil.VALUE : badOpType("mul"),
        symbol -> right instanceof Bool ? ((Bool) right).value ? symbol : Nil.VALUE : right instanceof Nil ? Nil.VALUE : badOpType("mul"),
        object -> right instanceof Bool ? ((Bool) right).value ? object : Nil.VALUE : right instanceof Nil ? Nil.VALUE : badOpType("mul"),
        string -> right.match(
          function -> badOpType("mul"),
          symbol -> badOpType("mul"),
          object -> badOpType("mul"),
          string2 -> badOpType("mul"),
          number -> repeatString(string, number),
          bool -> bool.value ? string : Nil.VALUE,
          nil -> nil
        ),
        number -> right.match(
          function -> badOpType("mul"),
          symbol -> badOpType("mul"),
          object -> badOpType("mul"),
          string -> repeatString(string, number),
          number2 -> new Number(number.value * number2.value),
          bool -> bool.value ? number : Nil.VALUE,
          nil -> nil
        ),
        bool -> right instanceof Bool ? Bool.of(bool.value & ((Bool) right).value) : bool.value ? right : Nil.VALUE,
        nil -> nil
      ),
      DIV = (left, right) -> left.match(
        function -> right == Bool.TRUE ? function : right instanceof Nil || right == Bool.FALSE ? Nil.VALUE : badOpType("div"),
        symbol -> right == Bool.TRUE ? symbol : right instanceof Nil || right == Bool.FALSE ? Nil.VALUE : badOpType("div"),
        object -> right == Bool.TRUE ? object : right instanceof Nil || right == Bool.FALSE ? Nil.VALUE : badOpType("div"),
        string -> right == Bool.TRUE ? string : right instanceof Nil || right == Bool.FALSE ? Nil.VALUE : badOpType("div"),
        number -> right.match(
          function -> badOpType("div"),
          symbol -> badOpType("div"),
          object -> badOpType("div"),
          string -> badOpType("div"),
          number2 -> number2.value == 0 ? number.value < 0 ? Number.NEGATIVE_INFINITY : Number.INFINITY : new Number(number.value / number2.value),
          bool -> bool.value ? number : Nil.VALUE,
          nil -> nil
        ),
        bool -> right instanceof Bool ? Bool.of(bool.value == ((Bool) right).value) : bool.value ? right : Nil.VALUE,
        nil -> nil
      ),
      MOD = (left, right) -> left.match(
        function -> right == Bool.FALSE ? function : right instanceof Nil || right == Bool.TRUE ? Nil.VALUE : badOpType("mod"),
        symbol -> right == Bool.FALSE ? symbol : right instanceof Nil || right == Bool.TRUE ? Nil.VALUE : badOpType("mod"),
        object -> right == Bool.FALSE ? object : right instanceof Nil || right == Bool.TRUE ? Nil.VALUE : badOpType("mod"),
        string -> right == Bool.FALSE ? string : right instanceof Nil || right == Bool.TRUE ? Nil.VALUE : badOpType("mod"),
        number -> right.match(
          function -> badOpType("mod"),
          symbol -> badOpType("mod"),
          object -> badOpType("mod"),
          string -> badOpType("mod"),
          number2 -> number2.value == 0 ? number : new Number(number.value % number2.value),
          bool -> bool.value ? Nil.VALUE : number,
          nil -> nil
        ),
        bool -> right instanceof Bool ? bool.value ? ((Bool) right).not() : Bool.FALSE : bool.value ? Nil.VALUE : right,
        nil -> nil
      ),
      EQ = (left, right) -> Bool.of(left.match(
        function -> function.equals(right),
        symbol -> symbol.equals(right),
        object -> object.equals(right),
        string -> string.equals(right),
        number -> number.equals(Number.of(right)),
        bool -> bool.equals(Bool.of(right)),
        nil -> nil.equals(right) && Bool.of(right).equals(nil)
      ) || right.match(
        function -> false,
        symbol -> false,
        object -> false,
        string -> false,
        number -> number.equals(Number.of(left)),
        bool -> bool.equals(Bool.of(left)),
        nil -> Bool.of(left).equals(nil)
      )),
      NEQ = (left, right) -> ((Bool) EQ.operate(left, right)).not(),
      SAME = (left, right) -> Bool.of(left instanceof Object ? left == right : left.equals(right)),
      NSAME = (left, right) -> ((Bool) SAME.operate(left, right)).not(),
      LT = (left, right) -> left.match(
        function -> badOpType("lt"),
        symbol -> badOpType("lt"),
        object -> badOpType("lt"),
        string -> right instanceof String ? Bool.of(string.value.compareTo(((String) right).value) < 0) : badOpType("lt"),
        number -> badOpType("lt", () -> Bool.of(number.value < Number.of(right).value)),
        bool -> badOpType("lt", () -> Bool.of(Number.of(bool).value < Number.of(right).value)),
        nil -> badOpType("lt", () -> Bool.of(Number.of(nil).value < Number.of(right).value))
      ),
      GT = (left, right) -> left.match(
        function -> badOpType("gt"),
        symbol -> badOpType("gt"),
        object -> badOpType("gt"),
        string -> right instanceof String ? Bool.of(string.value.compareTo(((String) right).value) > 0) : badOpType("gt"),
        number -> badOpType("gt", () -> Bool.of(number.value > Number.of(right).value)),
        bool -> badOpType("gt", () -> Bool.of(Number.of(bool).value > Number.of(right).value)),
        nil -> badOpType("gt", () -> Bool.of(Number.of(nil).value > Number.of(right).value))
      ),
      LTE = (left, right) -> badOpType("lte", () -> ((Bool) GT.operate(left, right)).not()),
      GTE = (left, right) -> badOpType("gte", () -> ((Bool) LT.operate(left, right)).not()),
      AND = (left, right) -> Bool.of(left).value ? right : left,
      OR = (left, right) -> Bool.of(left).value ? left : right,
      RANGE = (left, right) -> {
        Number start = badOpType("range", () -> Number.of(left)), end = badOpType("range", () -> Number.of(right));
        long starti = Math.round(start.value), endi = Math.round(end.value);
        if (starti != start.value || endi != end.value) throw new Value.Exception("Range operands must be integers");
        var arr = Object.of();
        long i = 0;
        if (starti == endi) arr.define(Number.ZERO, start, true);
        else if (starti > endi) for (; starti >= endi; starti--) arr.define(Number.of(i++), Number.of(starti), true);
        else for (; starti <= endi; starti++) arr.define(Number.of(i++), Number.of(starti), true);
        return arr;
      };
    
    private static Value repeatString(String string, Number number) {
      var i = Math.floor(number.value); 
      if (i <= 0) return Nil.VALUE;
      var builder = new StringBuilder();
      while (i --> 0) builder.append(string.value);
      return new String(builder.toString());
    }
    
    private static <V extends Value> V badOpType(java.lang.String name) {
      return new TypeException("Invalid operand type for unary operator '" + name + '\'').throwValue();
    }
    
    private static <V extends Value> V badOpType(java.lang.String name, Supplier<V> supplier) {
      try {
        return supplier.get();
      } catch (TypeException e) {
        return badOpType(name);
      }
    }
    
    Value operate(Value left, Value right);
    
  }
  
  @FunctionalInterface
  public interface UnOpFunc {
    
    public static final UnOpFunc
      ABS = value -> value.match(
        function -> badOpType("abs"),
        symbol -> badOpType("abs"),
        object -> new Number(object.entries.size()),
        string -> new Number(string.value.length()),
        number -> new Number(Math.abs(number.value)),
        bool -> Number.of(bool),
        nil -> Number.of(nil)
      ),
      POS = value -> badOpType("pos", () -> Number.of(value)),
      NEG = value -> badOpType("neg", () -> Number.of(value).negative()),
      NOT = value -> badOpType("not", () -> Bool.of(value).not()),
      INC = value -> badOpType("inc", () -> new Number(Number.of(value).value + 1)),
      DEC = value -> badOpType("dec", () -> new Number(Number.of(value).value - 1)),
      SPREAD = value -> new SpreadException(value).throwValue();
    
    private static <V extends Value> V badOpType(java.lang.String name) {
      return new TypeException("Invalid operand type for unary operator '" + name + '\'').throwValue();
    }
    
    private static <V extends Value> V badOpType(java.lang.String name, Supplier<V> supplier) {
      try {
        return supplier.get();
      } catch (TypeException e) {
        return badOpType(name);
      }
    }
    
    Value operate(Value value);
    
  }
  
}