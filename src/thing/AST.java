package thing;

import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.function.ToDoubleFunction;

import org.antlr.v4.runtime.Token;

abstract class AST {
  
  private AST() {}
  
  public abstract <R> R accept(ASTVisitor<R> visitor);
  
  public static final class DefsNode extends AST {
    
    public final Iterable<DefNode> defs;
    
    public DefsNode(Iterable<DefNode> defs) {
      this.defs = defs;
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitDefs(this);
    }
    
  }
  
  public static final class DefNode extends AST {
    
    public final AST def, value;
    public final boolean mutable;
    
    public DefNode(AST def, AST value, boolean mutable) {
      this.def = def;
      this.value = value;
      this.mutable = mutable;
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitDef(this);
    }
    
  }
  
  public static final class ParamsNode extends AST {
    
    public final Iterable<DefNode> params;
    public final String varargs;
    
    public ParamsNode(Iterable<DefNode> params, String varargs) {
      this.params = params;
      this.varargs = varargs;
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitParams(this);
    }
    
  }
  
  public static final class BranchNode extends AST {
    
    public final AST condition, tBranch, fBranch;
    
    public BranchNode(AST condition, AST tBranch, AST fBranch) {
      this.condition = condition;
      this.tBranch = tBranch;
      this.fBranch = fBranch;
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitBranch(this);
    }
    
  }
  
  public static final class LoopNode extends AST {
    
    public final AST init, condition, loopEnd, loop, noLoop, combinator;
    
    public LoopNode(AST init, AST condition, AST loopEnd, AST loop, AST noLoop, AST combinator) {
      this.init = init;
      this.condition = condition;
      this.loopEnd = loopEnd;
      this.loop = loop;
      this.noLoop = noLoop;
      this.combinator = combinator;
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitLoop(this);
    }
    
  }
  
  public static final class IteratorNode extends AST {
    
    public final String name;
    public final AST iterable, loop, noLoop, combinator;
    
    public IteratorNode(String name, AST iterable, AST loop, AST noLoop, AST combinator) {
      this.name = name;
      this.iterable = iterable;
      this.loop = loop;
      this.noLoop = noLoop;
      this.combinator = combinator;
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitIterator(this);
    }
    
  }
  
  public static final class ExceptionNode extends AST {
    
    public final AST throwing, handler, always;
    public final String name;
    
    public ExceptionNode(AST throwing, AST handler, AST always, String name) {
      this.throwing = throwing;
      this.handler = handler;
      this.always = always;
      this.name = name;
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitException(this);
    }
    
  }
  
  public static final class ScopeNode extends AST {
    
    public final AST implicit, value;
    
    public ScopeNode(AST implicit, AST value) {
      this.implicit = implicit;
      this.value = value;
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitScope(this);
    }
    
  }
  
  public static final class BinOpNode extends AST {
    
    public final AST left, right;
    public final Value.BinOpFunc op;
    public final boolean assign;
    
    public BinOpNode(AST left, AST right, Value.BinOpFunc op, boolean assign) {
      this.left = left;
      this.right = right;
      this.op = op;
      this.assign = assign;
    }
    
    public BinOpNode(AST left, AST right, Value.BinOpFunc op) {
      this(left, right, op, false);
    }
    
    public BinOpNode withOperands(AST left, AST right) {
      return new BinOpNode(left, right, op, assign);
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitBinOp(this);
    }
    
  }
  
  public static final class UnOpNode extends AST {
    
    public final AST operand;
    public final Value.UnOpFunc op;
    
    public UnOpNode(AST operand, Value.UnOpFunc op) {
      this.operand = operand;
      this.op = op;
    }
    
    public UnOpNode withOperand(AST operand) {
      return new UnOpNode(operand, op);
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitUnOp(this);
    }
    
  }
  
  public static final class ModOpNode extends AST {
    
    public final AST operand;
    public final Value.UnOpFunc op;
    public final boolean after;
    
    public ModOpNode(AST operand, Value.UnOpFunc op, boolean after) {
      this.operand = operand;
      this.op = op;
      this.after = after;
    }
    
    public ModOpNode withOperand(AST operand, boolean after) {
      return new ModOpNode(operand, op, after);
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitModOp(this);
    }
    
  }
  
  public static final class JumpNode extends AST {
    
    public final Type type;
    public final AST operand;
    
    public JumpNode(Type type, AST operand) {
      this.type = type;
      this.operand = operand;
    }
    
    public JumpNode(Type type) {
      this(type, null);
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitJump(this);
    }
    
    public static final class Exception extends Value.Exception {
      
      public final Type type;
      
      public Exception(Type type, Value value) {
        super(value);
        this.type = type;
      }
      
    }
    
    public enum Type {
      
      RETURN,
      THROW,
      BREAK,
      CONTINUE
      
    }
    
  }
  
  public static final class TypeNode extends AST {
    
    public final AST operand;
    
    public TypeNode(AST operand) {
      this.operand = operand;
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitType(this);
    }
    
  }
  
  public static final class CallNode extends AST {
    
    public final AST callee;
    public final Iterable<AST> args;
    
    public CallNode(AST callee, Iterable<AST> args) {
      this.callee = callee;
      this.args = args;
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitCall(this);
    }
    
  }
  
  public static final class MemberNode extends AST {
    
    public final AST object, member;
    public final boolean optional;
    
    public MemberNode(AST object, AST member, boolean optional) {
      this.object = object;
      this.member = member;
      this.optional = optional;
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitMember(this);
    }
    
  }
  
  public static final class VarNode extends AST {
    
    public final String name;
    
    public VarNode(String name) {
      this.name = name;
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitVar(this);
    }
    
  }
  
  public static final class FuncNode extends AST {
    
    public final ParamsNode params;
    public final AST body;
    
    public FuncNode(ParamsNode params, AST body) {
      this.params = params;
      this.body = body;
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitFunc(this);
    }
    
  }
  
  public static final class ObjNode extends AST {
    
    public final AST meta;
    public final Iterable<FieldNode> fields;
    
    public ObjNode(AST meta, Iterable<FieldNode> fields) {
      this.meta = meta;
      this.fields = fields;
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitObj(this);
    }
    
  }
  
  public static final class FieldNode extends AST {
    
    public final AST key, value;
    public final boolean mutable;
    
    public FieldNode(AST key, AST value, boolean mutable) {
      this.key = key;
      this.value = value;
      this.mutable = mutable;
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitField(this);
    }
    
  }
  
  public static final class StringNode extends AST {
    
    public final String value;
    
    public StringNode(String value) {
      this.value = value;
    }
    
    public static StringNode of(Token token) {
      var value = token.getText();
      return new StringNode(value.substring(1, value.length() - 1));
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitString(this);
    }
    
  }
  
  public static final class NumberNode extends AST {
    
    public final double value;
    
    public NumberNode(double value) {
      this.value = value;
    }
    
    public static NumberNode of(Token token) {
      var source = token.getText();
      var radix = source.length() < 2 ? 0 : source.charAt(1);
      if (source.charAt(0) == '0' && "dxob".indexOf(radix) > -1) source = source.substring(2);
      else radix = 0;
      return new NumberNode(radix == 0 ? Double.parseDouble(source) : (double) Long.parseLong(source, Map.of('d', 10, 'x', 16, 'o', 8, 'b', 2).get(radix)));
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitNumber(this);
    }
    
  }
  
  public static final class BoolNode extends AST {
    
    public static final BoolNode TRUE = new BoolNode(true), FALSE = new BoolNode(false);
    public final boolean value;
    
    private BoolNode(boolean value) {
      this.value = value;
    }
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitBool(this);
    }
    
  }
  
  public static final class NilNode extends AST {
    
    public static final NilNode VALUE = new NilNode();
    
    private NilNode() {}
    
    @Override
    public <R> R accept(ASTVisitor<R> visitor) {
      return visitor.visitNil(this);
    }
    
  }
  
}