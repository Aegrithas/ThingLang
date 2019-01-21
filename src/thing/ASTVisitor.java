package thing;

import static thing.AST.*;

interface ASTVisitor<R> {
  
  default R visit(AST node) {
    return node == null ? null : node.accept(this);
  }
  
  R visitDefs(DefsNode node);
  
  R visitDef(DefNode node);
  
  R visitParams(ParamsNode node);
  
  R visitBranch(BranchNode node);
  
  R visitLoop(LoopNode node);
  
  R visitIterator(IteratorNode node);
  
  R visitException(ExceptionNode node);
  
  R visitScope(ScopeNode node);
  
  R visitBinOp(BinOpNode node);
  
  R visitUnOp(UnOpNode node);
  
  R visitModOp(ModOpNode node);
  
  R visitJump(JumpNode node);
  
  R visitType(TypeNode node);
  
  R visitCall(CallNode node);
  
  R visitMember(MemberNode node);
  
  R visitVar(VarNode node);
  
  R visitFunc(FuncNode node);
  
  R visitObj(ObjNode node);
  
  R visitField(FieldNode node);
  
  R visitString(StringNode node);
  
  R visitNumber(NumberNode node);
  
  R visitBool(BoolNode node);
  
  R visitNil(NilNode node);
  
}