parser grammar JvmAssemblyParser;

options {
    tokenVocab = JvmAssemblyLexer;
}

assemblyFile
    : topLevelDirective* EOF
    ;

topLevelDirective
    : sourceDirective
    | classDirective
    | superDirective
    | implementsDirective
    | fieldDirective
    | methodDirective
    ;

sourceDirective
    : DIR_SOURCE FILENAME
    ;

classDirective
    : DIR_CLASS CLASS_FLAG* CLASS_ID
    ;

superDirective
    : DIR_SUPER CLASSNAME
    ;

implementsDirective
    : DIR_IMPLEMENTS CLASSNAME
    ;

fieldDirective
    : DIR_FIELD FIELD_FLAG* FIELD_ID FIELD_DESC
    ;

methodDirective
    : DIR_METHOD METHOD_FLAG* METHOD_ID METHOD_DESC
        methodCode?
    ;

methodCode
    : DIR_CODE CODE_NEWLINE codeLine* DIR_END_CODE
    ;

codeLine
    : limitLocals
    | limitStack
    | labelInstruction
    ;

limitLocals
    : DIR_LIMIT_LOCALS INT CODE_NEWLINE
    ;

limitStack
    : DIR_LIMIT_STACK INT CODE_NEWLINE
    ;

labelInstruction
    : (CODE_WORD COLON CODE_NEWLINE*)? instruction
    ;

instruction
    : CODE_WORD operand* CODE_NEWLINE
    ;

operand
    : INT
    | LONG
    | DOUBLE
    | FLOAT
    | CODE_WORD
    | STRING_STRING
    ;