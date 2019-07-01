/* Expr.g4*/
grammar Expr;
//parser
prog: (assn ';' NEWLINE? | expr ';' NEWLINE?)* ;
expr: expr ('*'|'/') expr
| expr ('+'|'-') expr
| num
| ID
| '(' expr ')' ;
assn: ID'='num;
num: ('+'|'-')?REAL;

//lexer
NEWLINE: [\r\n]+;
REAL:[0-9]+[.]*[0-9]* ;
ID: [a-zA-Z]+ ;
WS : [ \t\r\n]+ -> skip ;
