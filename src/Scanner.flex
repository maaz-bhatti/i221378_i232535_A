import java.io.*;
%%
%public
%class Scanner
%unicode
%line
%column

%{
    private Token createToken(String type) {
        return new Token(type, yytext(), yyline + 1, yycolumn + 1);
    }
%}

DIGIT          = [0-9]
LOWER          = [a-z]
UPPER          = [A-Z]
IDENTIFIER     = {UPPER}([a-z0-9_]){0,30}
INTEGER        = [+-]?{DIGIT}+
FLOAT          = [+]?{DIGIT}+\.{DIGIT}{1,6}([eE][+-]?{DIGIT}+)?
BOOLEAN        = "true" | "false"
STRING         = \"([^\"\\\n]|\\[\"\\ntr])*\"
CHAR           = \'([^\'\\\n]|\\[\'\\ntr])\'
MULTICOMMENT   = "#*" ([^*] | "*"+ [^*#])* "*"+ "#"
SINGLECOMMENT  = "##" [^\n]*
MULTIOP        = "**" | "==" | "!=" | "<=" | ">=" | "&&" | "||" | "++" | "--" | "+=" | "-=" | "*=" | "/="
SINGLEOP       = [+\-*/%<>=!]
PUNCT          = [()\{\}\[\]\,,;:]
KEYWORD        = "start" | "finish" | "loop" | "condition" | "declare" | "output" | "input" | "function" | "return" | "break" | "continue" | "else"
WHITESPACE     = [ \t\r\n]+
%%
{MULTICOMMENT}      { }
{SINGLECOMMENT}     { }
{MULTIOP}           { return createToken("OPERATOR"); }
{KEYWORD}           { return createToken("KEYWORD"); }
{BOOLEAN}           { return createToken("BOOLEAN_LITERAL"); }
{IDENTIFIER}        { return createToken("IDENTIFIER"); }
{FLOAT}             { return createToken("FLOAT_LITERAL"); }
{INTEGER}           { return createToken("INTEGER_LITERAL"); }
{STRING}            { return createToken("STRING_LITERAL"); }
{CHAR}              { return createToken("CHAR_LITERAL"); }
{SINGLEOP}          { return createToken("OPERATOR"); }
{PUNCT}             { return createToken("PUNCTUATOR"); }
{WHITESPACE}        { }

. { 
    System.err.println("Lexical Error, Line: " + (yyline+1) + ", Col: " + (yycolumn+1) + ", Lexeme: " + yytext());
    return createToken("ERROR"); 
}