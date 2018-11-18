grammar Thing;

options {
  language = Java ;
}

file : def* EOF ;
def : ( varDef
      | funcDef
      | doBlock
      )
      DOT
    ;
varDef : (VAR | LET) varInit (COMMA varInit)* ;
varInit : factor (ASSIGN noSpliceExpr)? ;
funcDef : FUNC factor params expr ;
params : LPAREN (param (COMMA param)* (COMMA ID ELLIPSIS)? | ID ELLIPSIS)? RPAREN ;
param : ID (ASSIGN noSpliceExpr | QMARK)? ;
doBlock : DO expr ;
expr : noSpliceExpr (SEMI expr)? ;
noSpliceExpr : control
             | binOpExpr
             ;
control : IF noSpliceExpr THEN noSpliceExpr (ELSE noSpliceExpr)?
        | WHILE noSpliceExpr DO ((noSpliceExpr | binOp) ON)? noSpliceExpr (ELSE noSpliceExpr)?
        | FOR init=noSpliceExpr? SEMI condition=noSpliceExpr? SEMI loopEnd=noSpliceExpr? DO ((noSpliceExpr | binOp) ON)? noSpliceExpr (ELSE noSpliceExpr)?
        | FOR (ID IN)? iterable=noSpliceExpr DO ((noSpliceExpr | binOp) ON)? noSpliceExpr (ELSE noSpliceExpr)?
        | TRY noSpliceExpr (CATCH (ID WITH)? noSpliceExpr)? (FINALLY noSpliceExpr)?
        | WITH noSpliceExpr DO noSpliceExpr
        ;
binOpExpr : unOpExpr (binOp noSpliceExpr)? ;
unOpExpr : unOp* modOpExpr
         | ABS abs=expr ABS
         | modOpExpr ELLIPSIS
         ;
modOpExpr : pre=modOp factor
          | factor post=modOp?
          ;
binOp : (ADD | SUB | MUL | DIV | MOD) ASSIGN?
      | EQ | NEQ | SAME | NSAME | LT | LTE | GT | GTE | AND | OR | RANGE | ASSIGN
      ;
unOp : ADD | SUB | NOT ;
modOp : ADDADD | SUBSUB | NOTNOT ;
factor : LPAREN expr RPAREN
       | (RETURN | THROW | BREAK | CONTINUE) noSpliceExpr?
       | TYPEOF factor
       | factor LPAREN (expr (COMMA expr)*)? RPAREN
       | factor simpleLit
       | factor QMARK? LBRACK expr RBRACK
       | factor QMARK? QUOT (ID | simpleLit)
       | factor META (brackLit | braceLit)
       | literal
       | varDef
       | THIS
       | ID
       ;
literal : funcLit
        | brackLit
        | braceLit
        | simpleLit
        ;
funcLit : FUNC factor? params noSpliceExpr ;
brackLit : LBRACK (expr (COMMA expr)*)? RBRACK
         | LBRACK expr COLON expr (COMMA expr COLON expr)* RBRACK
         ;
braceLit : LBRACE objDef* RBRACE
         | LBRACE objId COLON expr (COMMA objId COLON expr)* RBRACE
         ;
simpleLit : NUMBER
          | STRING
          | TRUE | FALSE
          | NIL
          ;
objId : ID | simpleLit | LBRACK expr RBRACK ;
objDef : (objVarDef | objFuncDef | doBlock) DOT ;
objVarDef : (VAR | LET) objVarInit (COMMA objVarInit)* ;
objVarInit : objId (ASSIGN noSpliceExpr)? ;
objFuncDef : FUNC objId params expr ;

WS : [ \t\r\n]+ -> skip ;
COMMENT : '/*' (~'*' | '*' ~'/')* '*/' -> skip ;
LINECOMMENT : '//' ('\\' ('\r\n' | .) | ~[\r\n])* -> skip ;

VAR : 'var' ;
LET : 'let' ;
FUNC : 'func' ;

IF : 'if' ;
THEN : 'then' ;
ELSE : 'else' ;
WHILE : 'while' ;
DO : 'do' ;
ON : 'on' ;
FOR : 'for' ;
IN : 'in' ;
TRY : 'try' ;
CATCH : 'catch' ;
WITH : 'with' ;
FINALLY : 'finally' ;
RETURN : 'return' ;
THROW : 'throw' ;
BREAK : 'break' ;
CONTINUE : 'continue' ;
AND : 'and' ;
OR : 'or' ;
TYPEOF : 'typeof' ;
THIS : 'this' ;

ASSIGN : '=' ;

EQ : '==' ;
NEQ : '!=' ;
SAME : '===' ;
NSAME : '!==' ;
LT : '<' ;
LTE : '<=' ;
GT : '>' ;
GTE : '>=' ;

ADD : '+' ;
SUB : '-' ;
MUL : '*' ;
DIV : '/' ;
MOD : '%' ;
NOT : '!' ;

ADDADD : '++' ;
SUBSUB : '--' ;
NOTNOT : '!!' ;

DOT : '.' ;
ABS : '|' ;
SEMI : ';' ;
META : '^' ;
QUOT : '\'' ;
COLON : ':' ;
COMMA : ',' ;
QMARK : '?' ;
RANGE : '..' ;
ELLIPSIS : '...' ;

LPAREN : '(' ;
RPAREN : ')' ;
LBRACK : '[' ;
RBRACK : ']' ;
LBRACE : '{' ;
RBRACE : '}' ;

STRING : '"' ('\\' ('\r\n' | .) | ~["])* '"'
       | '`' ('\\' ('\r\n' | .) | ~[`])* '`'
       ;
NUMBER : ([0-9] '_'?)* [0-9] ('.' ([0-9] '_'?)* [0-9])?
       | '0d' ('_'? [0-9])+
       | '0x' ('_'? [0-9A-Fa-f])+
       | '0o' ('_'? [0-7])+
       | '0b' ('_'? [01])+
       ;
TRUE : 'true' ;
FALSE : 'false' ;
NIL : 'nil' ;

ID : [A-Za-z_$] [A-Za-z0-9_$]* ;