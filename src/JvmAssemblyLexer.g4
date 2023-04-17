lexer grammar JvmAssemblyLexer;

DIR_SOURCE
    : '.source' -> pushMode(M_FILENAME);

DIR_CLASS
    : ('.class' | '.interface' | '.enum' | '.annotation' | '.module') -> pushMode(M_CLASS)
    ;

DIR_SUPER
    : '.super' -> pushMode(M_CLASSNAME);

DIR_IMPLEMENTS
    : '.implements' -> pushMode(M_CLASSNAME);

DIR_FIELD
    : '.field' -> pushMode(M_FIELD);

DIR_METHOD
    : '.method' -> pushMode(M_METHOD);

DIR_CODE
    : '.code' -> pushMode(M_CODE);

fragment ID
    : ~('.' | ';' | '[' | '/' | '(' | ')' | [ \r\n\t])+
    ;

fragment DESC
    : ~('.' | [ \r\n\t] | '(' | ')')+
    ;

WS
    : [ \t\r\n]+ -> skip
    ;

COMMENT
    : ';' (~'\n')* -> skip;

// ---- M_FILENAME ----
mode M_FILENAME;
FILENAME
    : (~[ \t\r\n])+
    ;

FILENAME_WS
    : [ \t]+ -> skip;

FILENAME_COMMENT
    : COMMENT -> skip;

FILENAME_NEWLINE
    : [\r\n] -> skip, popMode;

// ---- M_CLASS ----
mode M_CLASS;
CLASS_FLAG
    : 'public' | 'final' | 'abstract' | 'synthetic'
    ;

CLASS_ID
    : (ID '/')* ID
    ;

CLASS_WS
    : [ \t]+ -> skip;

CLASS_COMMENT
    : COMMENT -> skip;

NEWLINE
    : [\r\n] -> skip, popMode;

// ---- M_FIELD ----
mode M_FIELD;
FIELD_FLAG
    : 'public' | 'private' | 'protected'
    | 'static' | 'final' | 'volatile' | 'transient' | 'synthetic' | 'enum'
    ;

FIELD_ID
    : ID -> pushMode(M_FIELD_DESC);

FIELD_WS
    : [ \t]+ -> skip;

FIELD_COMMENT
    : COMMENT -> skip;

FIELD_LINE
    : [\r\n] -> skip, popMode;

// ---- M_FIELD_DESC ----
mode M_FIELD_DESC;
FIELD_DESC
    : DESC;

FIELD_DESC_WS
    : [ \t]+ -> skip;

FIELD_DESC_COMMENT
    : COMMENT -> skip;

FIELD_DESC_NEWLINE
    : [\r\n] -> skip, popMode, popMode;

// ---- M_METHOD ----
mode M_METHOD;
METHOD_FLAG
    : 'public' | 'private' | 'protected'
    | 'static' | 'final' | 'synchronized' | 'bridge' | 'varargs' | 'native' | 'abstract' | 'strict' | 'synthetic';

METHOD_ID
    : ID
    | '<init>'
    | '<clinit>'
    ;

METHOD_DESC
    : '(' DESC* ')' DESC
    ;

METHOD_WS
    : [ \t]+ -> skip;

METHOD_COMMENT
    : COMMENT -> skip;

METHOD_NEWLINE
    : [\r\n] -> skip, popMode;

// ---- M_CLASSNAME ----
mode M_CLASSNAME;
CLASSNAME
    : (ID '/')* ID;

CLASSNAME_WS
    : [ \t]+ -> skip
    ;

CLASSNAME_COMMENT
    : COMMENT -> skip;

CLASSNAME_NEWLINE
    : [\r\n] -> skip, popMode
    ;

// ---- M_CODE ----
mode M_CODE;

DIR_LIMIT_LOCALS
    : '.limit' [ \t]+ 'locals'
    ;

DIR_LIMIT_STACK
    : '.limit' [ \t]+ 'stack'
    ;

CODE_WORD
    : [a-zA-Z_$/;()[] ([a-zA-Z0-9_$/;()[] | '<init>' | '<clinit>')*
    ;

COLON
    : ':';

INT
    : [+-]? [0-9]+
    | '0' [Xx] [0-9a-fA-F]+
    ;

LONG
    : INT [Ll]
    ;

DOUBLE
    : [+-]? ([0-9]+ '.' [0-9]* | [0-9]* '.' [0-9]+)
    | [+-]? [0-9]+ ('.' [0-9]*)? [Ee] [+-]? [0-9]+
    ;

FLOAT
    : DOUBLE [Ff]
    ;

STRING
    : '"' -> more, pushMode(M_STRING)
    ;

CODE_NEWLINE
    : [\r\n]+
    ;

CODE_WS
    : [ \t]+ -> skip
    ;

CODE_COMMENT
    : COMMENT -> skip;

DIR_END_CODE
    : '.end' [ \t]+ 'code' -> popMode;

// ---- M_STRING ----
mode M_STRING;

fragment STRING_CHAR
    : ~('\\' | [\r\n\t])
    | '\\' [bstnfr"'\\]
    | '\\u' [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F] [0-9a-fA-F]
    | '\\'
        ([0-7] | [0-7] [0-7] | [0-3] [0-7] [0-7])
    ;

STRING_STRING
    : STRING_CHAR* '"'  -> popMode
    ;