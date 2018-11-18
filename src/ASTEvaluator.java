package thing;

import java.io.Closeable;
import java.util.List;
import java.util.Stack;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.function.Supplier;

import static thing.AST.*;

class ASTEvaluator implements ASTVisitor<Value> {
  
  Scope scope = Scope.empty();
  private Value.Object currentObject = null;
  private Value thisVal = null;
  private boolean writeThis = false;
  
  private Value get(String name) {
    return scope.get(name);
  }
  
  private Value set(String name, Value value) {
    scope.set(name, value);
    return value;
  }
  
  private Value define(String name, Value value, boolean mutable) {
    scope.define(name, value, mutable);
    return value;
  }
  
  private Value get(Value object, Value member, boolean optional) {
    if (!(object instanceof Value.Object)) object = object.getMetaobject();
    var value = ((Value.Object) object).get(member);
    if (value == null) {
      if (optional) value = Value.Nil.VALUE;
      else throw new DataException("missing object member");
    }
    return value;
  }
  
  private Value set(Value object, Value member, Value value, boolean optional) {
    if (!(object instanceof Value.Object)) object = object.getMetaobject();
    value = ((Value.Object) object).set(member, value);
    if (value == null) {
      if (optional) value = Value.Nil.VALUE;
      else throw new DataException("missing object member");
    }
    return value;
  }
  
  private Value define(Value object, Value member, Value value, boolean mutable, boolean optional) {
    if (!(object instanceof Value.Object)) object = object.getMetaobject();
    value = ((Value.Object) object).define(member, value, mutable);
    if (value == null) {
      if (optional) value = Value.Nil.VALUE;
      else throw new DataException("duplicate object member");
    }
    return value;
  }
  
  private AutoScope scope() {
    return new AutoScope();
  }
  
  private <R> R scope(Supplier<R> scoped) {
    try (var scope = scope()) {
      return scoped.get();
    }
  }
  
  private AutoScope withScope(Value.Object with) {
    return new AutoScope(with);
  }
  
  private void brotherScope() {
    scope = scope.brother();
  }
  
  public Value visitDefs(DefsNode node) {
    Value last = null;
    for (var def : node.defs) last = def.accept(this);
    return last;
  }
  
  public Value visitDef(DefNode node) {
      if (node.def instanceof VarNode) return define(((VarNode) node.def).name, node.value == null ? null : scope(() -> node.value.accept(this)), node.mutable);
      if (node.def instanceof MemberNode) {
        var def = (MemberNode) node.def;
        Value object = null, member = null, value = null;
        try (var scope = scope()) {
          object = def.object.accept(this);
          member = def.member.accept(this);
          if (node.value != null) value = node.value.accept(this);
        }
        return define(object, member, value, def.optional, node.mutable);
      }
      if (node.def == null) return scope(() -> node.value.accept(this));
      throw new DataException("invalid definition");
  }
  
  public Value visitParams(ParamsNode node) {
    throw new Value.Exception("ASTEvaluator should never directly visit a ParamsNode");
  }
  
  public Value visitBranch(BranchNode node) {
    var condition = Value.Bool.of(node.condition.accept(this)).value;
    try (var branch = scope()) {
      return condition ? node.tBranch.accept(this) : node.fBranch == null ? Value.Nil.VALUE : node.fBranch.accept(this);
    }
  }
  
  public Value visitLoop(LoopNode node) {
    Value value = null;
    try (var whole = scope()) {
      var combinator = node.combinator == null ? null : Value.Function.functor(node.combinator.accept(this));
      if (node.combinator != null && combinator == null) throw new TypeException("loop combinators must be functors");
      if (node.init != null) node.init.accept(this);
      var done = false;
      while (!done && scope(() -> Value.Bool.of(node.condition.accept(this))).value) {
        try (var loop = scope()) {
          if (value == null || combinator == null) value = node.loop.accept(this);
          else value = combinator.call(List.of(value, node.loop.accept(this)));
        } catch (JumpNode.Exception e) {
          if (e.type == JumpNode.Type.BREAK || e.type == JumpNode.Type.CONTINUE) {
            if (value == null || combinator == null) value = e.value;
            else value = combinator.call(List.of(value, e.value));
            if (e.type == JumpNode.Type.BREAK) done = true;
          } else {
            throw e;
          }
        }
        if (node.loopEnd != null) try (var loopEnd = scope()) {
          node.loopEnd.accept(this);
        }
      }
      if (value == null && node.noLoop != null) try (var noLoop = scope()) {
        value = node.noLoop.accept(this);
      }
    }
    return value;
  }
  
  public Value visitIterator(IteratorNode node) {
    Value value = null;
    try (var whole = scope()) {
      var combinator = node.combinator == null ? null : Value.Function.functor(node.combinator.accept(this));
      if (node.combinator != null && combinator == null) throw new TypeException("loop combinators must be functors");
      for (var element : Value.Object.iterable(node.iterable.accept(this))) try (var loop = scope()) {
        if (node.name != null) define(node.name, element, true);
        if (value == null || combinator == null) value = node.loop.accept(this);
        else value = combinator.call(List.of(value, node.loop.accept(this)));
      } catch (JumpNode.Exception e) {
        if (e.type == JumpNode.Type.BREAK || e.type == JumpNode.Type.CONTINUE) {
          if (value == null || combinator == null) value = e.value;
          else value = combinator.call(List.of(value, e.value));
          if (e.type == JumpNode.Type.BREAK) break;
        } else {
          throw e;
        }
      }
      if (value == null && node.noLoop != null) try (var noLoop = scope()) {
        value = node.noLoop.accept(this);
      }
    }
    return value;
  }
  
  public Value visitException(ExceptionNode node) {
    Value value;
    JumpNode.Exception jump = null;
    try (var throwing = scope()) {
      value = node.throwing.accept(this);
    } catch (JumpNode.Exception e) {
      brotherScope();
      if (e.type != JumpNode.Type.THROW) jump = e;
      if (node.name != null) define(node.name, e.value, true);
      if (node.handler == null) return Value.Nil.VALUE;
      value = node.handler.accept(this);
    }
    if (node.always != null) try (var always = scope()) {
      node.always.accept(this);
    }
    if (jump != null) throw jump;
    return value;
  }
  
  public Value visitScope(ScopeNode node) {
    var implicit = node.implicit.accept(this);
    if (implicit instanceof Value.Nil) return node.value.accept(this);
    if (!(implicit instanceof Value.Object)) throw new TypeException("with scopes must be objects");
    try (var with = withScope((Value.Object) implicit)) {
      return node.value.accept(this);
    }
  }
  
  public Value visitBinOp(BinOpNode node) {
    if (node.assign) {
      if (node.left instanceof VarNode) {
        var left = (VarNode) node.left;
        return node.op == null ? set(left.name, node.right.accept(this)) : set(left.name, node.op.operate(get(left.name), node.right.accept(this)));
      }
      if (node.left instanceof MemberNode) {
        var left = (MemberNode) node.left;
        Value object = left.object.accept(this), member = left.member.accept(this);
        return node.op == null ? set(object, member, node.right.accept(this), left.optional) : set(object, member, node.op.operate(get(object, member, left.optional), node.right.accept(this)), left.optional);
      }
      throw new DataException("bad assignment");
    } else {
      return node.op.operate(node.left.accept(this), node.right.accept(this));
    }
  }
  
  public Value visitUnOp(UnOpNode node) {
    return node.op.operate(node.operand.accept(this));
  }
  
  public Value visitModOp(ModOpNode node) {
    if (node.operand instanceof VarNode) {
      var operand = (VarNode) node.operand;
      var value = get(operand.name);
      if (node.after) set(operand.name, node.op.operate(value));
      else value = set(operand.name, node.op.operate(value));
      return value;
    }
    if (node.operand instanceof MemberNode) {
      var operand = (MemberNode) node.operand;
      Value object = operand.object.accept(this), member = operand.member.accept(this), value = get(object, member, operand.optional);
      if (node.after) set(object, member, node.op.operate(value), operand.optional);
      else value = set(object, member, node.op.operate(value), operand.optional);
      return value;
    }
    throw new DataException("bad assignment");
  }
  
  public Value visitJump(JumpNode node) {
    throw new JumpNode.Exception(node.type, node.operand == null ? Value.Nil.VALUE : node.operand.accept(this));
  }
  
  public Value visitType(TypeNode node) {
    return node.operand.accept(this).getType();
  }
  
  public Value visitCall(CallNode node) {
    var thisValBefore = thisVal;
    writeThis = node.callee instanceof MemberNode;
    var callee = node.callee.accept(this);
    var thisArg = callee instanceof Value.Function ? thisVal : callee;
    callee = Value.Function.functor(callee);
    thisVal = thisValBefore;
    if (callee == null) throw new TypeException("only functors can be called");
    var args = new ArrayList<Value>();
    for (var arg : node.args) try {
      args.add(arg.accept(this));
    } catch (SpreadException e) {
      Value.Object.iterable(e.value).forEach(args::add);
    }
    return ((Value.Function) callee).apply(thisArg, args);
  }
  
  public Value visitMember(MemberNode node) {
    var writeThisBefore = writeThis;
    writeThis = false;
    return get(writeThisBefore ? thisVal = node.object.accept(this) : node.object.accept(this), node.member.accept(this), node.optional);
  }
  
  public Value visitVar(VarNode node) {
    return get(node.name);
  }
  
  public Value visitFunc(FuncNode node) {
    var params = new ArrayList<String>();
    var defaults = new ArrayList<Value>();
    for (var param : node.params.params) {
      if (!(param.def instanceof VarNode)) throw new DataException("params must be simple ids");
      params.add(((VarNode) param.def).name);
      defaults.add(param.value == null ? null : param.value.accept(this));
    }
    return Value.Function.of(this, params.toArray(new String[params.size()]), defaults.toArray(new Value[defaults.size()]), node.params.varargs, node.body, scope);
  }
  
  public Value visitObj(ObjNode node) {
    var meta = node.meta == null ? null : node.meta.accept(this);
    if (meta != null && !(meta instanceof Value.Object)) throw new TypeException("metaobjects must be objects");
    Value.Object oldObject = currentObject, value = currentObject = Value.Object.of((Value.Object) meta);
    for (var field : node.fields) field.accept(this);
    currentObject = oldObject;
    return value;
  }
  
  public Value visitField(FieldNode node) {
    Value key = null, value = null;
    try (var scope = scope()) {
      if (node.key != null) key = node.key.accept(this);
      if (key != null) brotherScope();
      if (node.value != null) value = node.value.accept(this);
    }
    return key == null ? value : define(currentObject, key, value, node.mutable, false);
  }
  
  public Value visitString(StringNode node) {
    return Value.String.of(node.value);
  }
  
  public Value visitNumber(NumberNode node) {
    return Value.Number.of(node.value);
  }
  
  public Value visitBool(BoolNode node) {
    return Value.Bool.of(node.value);
  }
  
  public Value visitNil(NilNode node) {
    return Value.Nil.VALUE;
  }
  
  private final class AutoScope implements Closeable {
    
    public AutoScope() {
      scope = scope.child();
    }
    
    public AutoScope(Value.Object with) {
      scope = scope.with(with);
    }
    
    @Override
    public void close() {
      scope = scope.parent;
    }
    
  }
  
}