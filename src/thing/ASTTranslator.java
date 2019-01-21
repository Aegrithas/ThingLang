package thing;

import java.util.List;
import java.util.ArrayList;

import static thing.AST.*;
import static thing.ThingParser.*;

class ASTTranslator extends ThingBaseVisitor<AST> {
  
  private boolean mutable = false;
  
  @Override
  public DefsNode visitFile(FileContext ctx) {
    var defs = new ArrayList<DefNode>();
    for (var def : ctx.def()) {
      var result = def.accept(this);
      if (result instanceof DefsNode) for (var subdef : ((DefsNode) result).defs) defs.add(subdef);
      else defs.add((DefNode) result);
    }
    return new DefsNode(defs);
  }
  
  @Override
  public AST visitDef(DefContext ctx) {
    var varDef = ctx.varDef();
    if (varDef != null) return varDef.accept(this);
    var funcDef = ctx.funcDef();
    if (funcDef != null) return funcDef.accept(this);
    var doBlock = ctx.doBlock();
    if (doBlock != null) return doBlock.accept(this);
    return null;
  }
  
  @Override
  public DefsNode visitVarDef(VarDefContext ctx) {
    var defs = new ArrayList<DefNode>();
    for (var varInit : ctx.varInit()) {
      mutable = ctx.LET() == null;
      defs.add((DefNode) varInit.accept(this));
    }
    return new DefsNode(defs);
  }
  
  @Override
  public DefNode visitVarInit(VarInitContext ctx) {
    var mutable = this.mutable;
    return new DefNode(ctx.factor().accept(this), ctx.ASSIGN() == null ? null : ctx.noSpliceExpr().accept(this), mutable);
  }
  
  @Override
  public DefNode visitFuncDef(FuncDefContext ctx) {
    return new DefNode(ctx.factor().accept(this), new FuncNode((ParamsNode) ctx.params().accept(this), ctx.expr().accept(this)), false);
  }
  
  @Override
  public ParamsNode visitParams(ParamsContext ctx) {
    var params = new ArrayList<DefNode>();
    for (var param : ctx.param()) params.add((DefNode) param.accept(this));
    return new ParamsNode(params, ctx.ELLIPSIS() == null ? null : ctx.ID().getSymbol().getText());
  }
  
  @Override
  public DefNode visitParam(ParamContext ctx) {
    return new DefNode(new VarNode(ctx.ID().getSymbol().getText()), ctx.ASSIGN() == null ? ctx.QMARK() == null ? null : NilNode.VALUE : ctx.noSpliceExpr().accept(this), true);
  }
  
  @Override
  public DefNode visitDoBlock(DoBlockContext ctx) {
    return new DefNode(null, ctx.expr().accept(this), false);
  }
  
  @Override
  public AST visitExpr(ExprContext ctx) {
    var value = ctx.noSpliceExpr().accept(this);
    if (ctx.SEMI() != null) value = new BinOpNode(value, ctx.expr().accept(this), Value.BinOpFunc.SEQUENCE);
    return value;
  }
  
  @Override
  public AST visitNoSpliceExpr(NoSpliceExprContext ctx) {
    var control = ctx.control();
    if (control != null) return control.accept(this);
    var binOpExpr = ctx.binOpExpr();
    if (binOpExpr != null) return binOpExpr.accept(this);
    return null;
  }
  
  @Override
  public AST visitControl(ControlContext ctx) {
    var i = 0;
    if (ctx.IF() != null) {
      AST condition = ctx.noSpliceExpr(i++).accept(this), tBranch = ctx.noSpliceExpr(i++).accept(this), fBranch = null;
      if (ctx.ELSE() != null) fBranch = ctx.noSpliceExpr(i++).accept(this);
      return new BranchNode(condition, tBranch, fBranch);
    } else if (ctx.WHILE() != null) {
      AST condition = ctx.noSpliceExpr(i++).accept(this), loop = null, noLoop = null, combinator = null;
      if (ctx.ON() != null) {
        var binOp = ctx.binOp();
        final VarNode leftVar = new VarNode("l"), rightVar = new VarNode("r");
        combinator = binOp == null ? ctx.noSpliceExpr(i++).accept(this) : new FuncNode(new ParamsNode(List.of(new DefNode(leftVar, null, true), new DefNode(rightVar, null, true)), null), ((BinOpNode) binOp.accept(this)).withOperands(leftVar, rightVar));
      }
      loop = ctx.noSpliceExpr(i++).accept(this);
      if (ctx.ELSE() != null) noLoop = ctx.noSpliceExpr(i++).accept(this);
      return new LoopNode(null, condition, null, loop, noLoop, combinator);
    } else if (ctx.FOR() != null) {
      if (ctx.iterable == null) {
        AST loop = null, noLoop = null, combinator = null;
        if (ctx.init != null) i++;
        if (ctx.condition != null) i++;
        if (ctx.loopEnd != null) i++;
        if (ctx.ON() != null) {
          var binOp = ctx.binOp();
          final VarNode leftVar = new VarNode("l"), rightVar = new VarNode("r");
          combinator = binOp == null ? ctx.noSpliceExpr(i++).accept(this) : new FuncNode(new ParamsNode(List.of(new DefNode(leftVar, null, true), new DefNode(rightVar, null, true)), null), ((BinOpNode) binOp.accept(this)).withOperands(leftVar, rightVar));
        }
        loop = ctx.noSpliceExpr(i++).accept(this);
        if (ctx.ELSE() != null) noLoop = ctx.noSpliceExpr(i++).accept(this);
        return new LoopNode(ctx.init == null ? null : ctx.init.accept(this), ctx.condition == null ? BoolNode.TRUE : ctx.condition.accept(this), ctx.loopEnd == null ? null : ctx.loopEnd.accept(this), loop, noLoop, combinator);
      } else {
        AST loop = null, noLoop = null, combinator = null;
        i++;
        if (ctx.ON() != null) {
          var binOp = ctx.binOp();
          final VarNode leftVar = new VarNode("l"), rightVar = new VarNode("r");
          combinator = binOp == null ? ctx.noSpliceExpr(i++).accept(this) : new FuncNode(new ParamsNode(List.of(new DefNode(leftVar, null, true), new DefNode(rightVar, null, true)), null), ((BinOpNode) binOp.accept(this)).withOperands(leftVar, rightVar));
        }
        loop = ctx.noSpliceExpr(i++).accept(this);
        if (ctx.ELSE() != null) noLoop = ctx.noSpliceExpr(i++).accept(this);
        return new IteratorNode(ctx.IN() == null ? null : ctx.ID().getSymbol().getText(), ctx.iterable.accept(this), loop, noLoop, combinator);
      }
    } else if (ctx.TRY() != null) {
      AST throwing = ctx.noSpliceExpr(i++).accept(this), handler = null, always = null;
      if (ctx.CATCH() != null) handler = ctx.noSpliceExpr(i++).accept(this);
      if (ctx.FINALLY() != null) always = ctx.noSpliceExpr(i++).accept(this);
      return new ExceptionNode(throwing, handler, always, ctx.WITH() == null ? null : ctx.ID().getSymbol().getText());
    } else if (ctx.WITH() != null) {
      return new ScopeNode(ctx.noSpliceExpr(i++).accept(this), ctx.noSpliceExpr(i++).accept(this));
    }
    throw null;
  }
  
  @Override
  public AST visitBinOpExpr(BinOpExprContext ctx) {
    var value = ctx.unOpExpr().accept(this);
    var binOp = ctx.binOp();
    if (binOp != null) value = ((BinOpNode) binOp.accept(this)).withOperands(value, ctx.noSpliceExpr().accept(this));
    return value;
  }
  
  @Override
  public AST visitUnOpExpr(UnOpExprContext ctx) {
    if (ctx.abs == null) {
      if (ctx.ELLIPSIS() == null) {
        var value = ctx.modOpExpr().accept(this);
        for (var unOp : ctx.unOp()) value = ((UnOpNode) unOp.accept(this)).withOperand(value);
        return value;
      } else {
        return new UnOpNode(ctx.modOpExpr().accept(this), Value.UnOpFunc.SPREAD);
      }
    } else {
      return new UnOpNode(ctx.abs.accept(this), Value.UnOpFunc.ABS);
    }
  }
  
  @Override
  public AST visitModOpExpr(ModOpExprContext ctx) {
    return ctx.pre != null ? ((ModOpNode) ctx.pre.accept(this)).withOperand(ctx.factor().accept(this), false) : ctx.post != null ? ((ModOpNode) ctx.post.accept(this)).withOperand(ctx.factor().accept(this), true) : ctx.factor().accept(this);
  }
  
  @Override
  public BinOpNode visitBinOp(BinOpContext ctx) {
    var assign = ctx.ASSIGN() != null;
    switch (ctx.start.getType()) {
      case ThingParser.ADD: return new BinOpNode(null, null, Value.BinOpFunc.ADD, assign);
      case ThingParser.SUB: return new BinOpNode(null, null, Value.BinOpFunc.SUB, assign);
      case ThingParser.MUL: return new BinOpNode(null, null, Value.BinOpFunc.MUL, assign);
      case ThingParser.DIV: return new BinOpNode(null, null, Value.BinOpFunc.DIV, assign);
      case ThingParser.MOD: return new BinOpNode(null, null, Value.BinOpFunc.MOD, assign);
      case ThingParser.EQ: return new BinOpNode(null, null, Value.BinOpFunc.EQ);
      case ThingParser.NEQ: return new BinOpNode(null, null, Value.BinOpFunc.NEQ);
      case ThingParser.SAME: return new BinOpNode(null, null, Value.BinOpFunc.SAME);
      case ThingParser.NSAME: return new BinOpNode(null, null, Value.BinOpFunc.NSAME);
      case ThingParser.LT: return new BinOpNode(null, null, Value.BinOpFunc.LT);
      case ThingParser.LTE: return new BinOpNode(null, null, Value.BinOpFunc.LTE);
      case ThingParser.GT: return new BinOpNode(null, null, Value.BinOpFunc.GT);
      case ThingParser.GTE: return new BinOpNode(null, null, Value.BinOpFunc.GTE);
      case ThingParser.AND: return new BinOpNode(null, null, Value.BinOpFunc.AND);
      case ThingParser.OR: return new BinOpNode(null, null, Value.BinOpFunc.OR);
      case ThingParser.RANGE: return new BinOpNode(null, null, Value.BinOpFunc.RANGE);
      default: if (assign) return new BinOpNode(null, null, null, true);
    }
    throw null;
  }
  
  @Override
  public UnOpNode visitUnOp(UnOpContext ctx) {
    switch (ctx.start.getType()) {
      case ThingParser.ADD: return new UnOpNode(null, Value.UnOpFunc.POS);
      case ThingParser.SUB: return new UnOpNode(null, Value.UnOpFunc.NEG);
      case ThingParser.NOT: return new UnOpNode(null, Value.UnOpFunc.NOT);
    }
    throw null;
  }
  
  @Override
  public ModOpNode visitModOp(ModOpContext ctx) {
    switch (ctx.start.getType()) {
      case ThingParser.ADDADD: return new ModOpNode(null, Value.UnOpFunc.INC, false);
      case ThingParser.SUBSUB: return new ModOpNode(null, Value.UnOpFunc.DEC, false);
      case ThingParser.NOTNOT: return new ModOpNode(null, Value.UnOpFunc.NOT, false);
    }
    throw null;
  }
  
  @Override
  public AST visitFactor(FactorContext ctx) {
    var factor = ctx.factor();
    if (factor == null) {
      if (ctx.LPAREN() != null) return ctx.expr(0).accept(this);
      var literal = ctx.literal();
      if (literal != null) return literal.accept(this);
      var varDef = ctx.varDef();
      if (varDef != null) return varDef.accept(this);
      if (ctx.THIS() != null) return new VarNode(Value.Function.THIS);
      var ID = ctx.ID();
      if (ID != null) return new VarNode(ID.getSymbol().getText());
      JumpNode.Type type = null;
      switch (ctx.start.getType()) {
        case ThingParser.RETURN:
          type = JumpNode.Type.RETURN;
          break;
        case ThingParser.THROW:
          type = JumpNode.Type.THROW;
          break;
        case ThingParser.BREAK:
          type = JumpNode.Type.BREAK;
          break;
        case ThingParser.CONTINUE:
          type = JumpNode.Type.CONTINUE;
          break;
      }
      var noSpliceExpr = ctx.noSpliceExpr();
      return new JumpNode(type, noSpliceExpr == null ? null : noSpliceExpr.accept(this));
    } else {
      if (ctx.TYPEOF() != null) {
        return new TypeNode(factor.accept(this));
      } else if (ctx.META() != null) {
        var brackLit = ctx.brackLit();
        var obj = (ObjNode) (brackLit == null ? ctx.braceLit().accept(this) : brackLit.accept(this));
        return new ObjNode(factor.accept(this), obj.fields);
      } else if (ctx.LBRACK() != null || ctx.QUOT() != null) {
        AST value = null;
        var expr = ctx.expr(0);
        if (expr != null) value = expr.accept(this);
        var simpleLit = ctx.simpleLit();
        if (simpleLit != null) value = simpleLit.accept(this);
        var ID = ctx.ID();
        if (ID != null) value = new StringNode(ID.getSymbol().getText());
        return new MemberNode(factor.accept(this), value, ctx.QMARK() != null);
      } else {
        var callee = factor.accept(this);
        if (ctx.LPAREN() != null) {
          var args = new ArrayList<AST>();
          for (var expr : ctx.expr()) args.add(expr.accept(this));
          return new CallNode(callee, args);
        }
        return new CallNode(callee, List.of(ctx.simpleLit().accept(this)));
      }
    }
  }
  
  @Override
  public AST visitFuncLit(FuncLitContext ctx) {
    AST value = new FuncNode((ParamsNode) ctx.params().accept(this), ctx.noSpliceExpr().accept(this));
    var factor = ctx.factor();
    if (factor != null) value = new DefNode(factor.accept(this), value, false);
    return value;
  }
  
  @Override
  public ObjNode visitBrackLit(BrackLitContext ctx) {
    var fields = new ArrayList<FieldNode>();
    var i = 0;
    AST key = null;
    if (ctx.COLON(0) == null) for (var expr : ctx.expr()) fields.add(new FieldNode(new NumberNode(i++) , expr.accept(this), true));
    else for (var expr : ctx.expr()) if (i++ % 2 == 0) key = expr.accept(this);
    else fields.add(new FieldNode(key, expr.accept(this), true));
    return new ObjNode(null, fields);
  }
  
  @Override
  public ObjNode visitBraceLit(BraceLitContext ctx) {
    var fields = new ArrayList<FieldNode>();
    if (ctx.COLON(0) == null) for (var objDef : ctx.objDef()) {
      var result = objDef.accept(this);
      if (result instanceof ObjNode) for (var field : ((ObjNode) result).fields) fields.add(field);
      else fields.add((FieldNode) result);
    } else {
      var i = 0;
      for (var objId : ctx.objId()) fields.add(new FieldNode(objId.accept(this), ctx.expr(i++).accept(this), true));
    }
    return new ObjNode(null, fields);
  }
  
  @Override
  public AST visitSimpleLit(SimpleLitContext ctx) {
    switch (ctx.start.getType()) {
      case ThingParser.STRING: return StringNode.of(ctx.start);
      case ThingParser.NUMBER: return NumberNode.of(ctx.start);
      case ThingParser.TRUE: return BoolNode.TRUE;
      case ThingParser.FALSE: return BoolNode.FALSE;
      case ThingParser.NIL: return NilNode.VALUE;
    }
    throw null;
  }
  
  @Override
  public AST visitObjId(ObjIdContext ctx) {
    if (ctx.LBRACK() != null) return ctx.expr().accept(this);
    var simpleLit = ctx.simpleLit();
    if (simpleLit != null) return simpleLit.accept(this);
    return new StringNode(ctx.ID().getSymbol().getText());
  }
  
  @Override
  public AST visitObjDef(ObjDefContext ctx) {
    var objVarDef = ctx.objVarDef();
    if (objVarDef != null) return objVarDef.accept(this);
    var objFuncDef = ctx.objFuncDef();
    if (objFuncDef != null) return objFuncDef.accept(this);
    var doBlock = ctx.doBlock();
    if (doBlock != null) return new FieldNode(null, ((DefNode) doBlock.accept(this)).value, false);
    return null;
  }
  
  @Override
  public ObjNode visitObjVarDef(ObjVarDefContext ctx) {
    var fields = new ArrayList<FieldNode>();
    for (var objVarInit : ctx.objVarInit()) {
      mutable = ctx.LET() == null;
      fields.add((FieldNode) objVarInit.accept(this));
    }
    return new ObjNode(null, fields);
  }
  
  @Override
  public FieldNode visitObjVarInit(ObjVarInitContext ctx) {
    var mutable = this.mutable;
    var noSpliceExpr = ctx.noSpliceExpr();
    return new FieldNode(ctx.objId().accept(this), noSpliceExpr == null ? NilNode.VALUE : noSpliceExpr.accept(this), mutable);
  }
  
  @Override
  public FieldNode visitObjFuncDef(ObjFuncDefContext ctx) {
    return new FieldNode(ctx.objId().accept(this), new FuncNode((ParamsNode) ctx.params().accept(this), ctx.expr().accept(this)), false);
  }
  
}