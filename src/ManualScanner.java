package src;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * DFA-based lexical scanner for the custom language.
 * Uses explicit state transitions and longest-match-first strategy.
 * 
 * DFA Design:
 * - States are integer constants representing scanner positions
 * - transition(state, char) is the pure DFA transition function
 * - acceptType(state, lexeme) maps accepting states to TokenType
 * - nextToken() drives the DFA with longest-match backtracking
 */
public class ManualScanner {
    private String input;
    private int currentPos;
    private int line;
    private int column;
    private SymbolTable symbolTable;
    private ErrorHandler errorHandler;

    // Statistics
    private int totalTokens = 0;
    private int linesProcessed = 0;
    private int commentsRemoved = 0;
    private Map<TokenType, Integer> tokenCounts = new EnumMap<>(TokenType.class);

    // ==================== DFA STATE CONSTANTS ====================
    private static final int DEAD = -1;
    private static final int S_START = 0;

    // Uppercase identifier: [A-Z][a-z0-9]*
    private static final int S_IDENT = 1;

    // Lowercase word: [a-z][a-zA-Z0-9]* (keyword, boolean, or invalid ident)
    private static final int S_WORD = 2;

    // Integer: [0-9]+
    private static final int S_INT = 3;

    // Float states
    private static final int S_DOT = 4; // digits + "."
    private static final int S_FRAC = 5; // digits + "." + digits
    private static final int S_EXP_E = 6; // ...e/E
    private static final int S_EXP_SIGN = 7; // ...e[+-]
    private static final int S_EXP_DIG = 8; // ...e[+-]?digits

    // String literal
    private static final int S_STR = 9; // inside "..."
    private static final int S_STR_ESC = 10; // after \ in string
    private static final int S_STR_DONE = 11; // closing " found

    // Char literal
    private static final int S_CHR = 12; // after opening '
    private static final int S_CHR_CH = 13; // after char content
    private static final int S_CHR_ESC = 14; // after \ in char
    private static final int S_CHR_ESC_CH = 15; // after escape char
    private static final int S_CHR_DONE = 16; // closing ' found

    // Comment states
    private static final int S_HASH = 17; // after #
    private static final int S_CMT_SL = 18; // single-line ##...
    private static final int S_CMT_ML = 19; // multi-line #*...
    private static final int S_CMT_ML_STAR = 20; // after * in multi-line
    private static final int S_CMT_ML_DONE = 21; // after *#

    // Single-char operators (accepting)
    private static final int S_PLUS = 22;
    private static final int S_MINUS = 23;
    private static final int S_STAR = 24;
    private static final int S_SLASH = 25;
    private static final int S_PCT = 26;
    private static final int S_EQ = 27;
    private static final int S_BANG = 28;
    private static final int S_LT = 29;
    private static final int S_GT = 30;
    private static final int S_AMP = 31; // non-accepting (needs &&)
    private static final int S_PIPE = 32; // non-accepting (needs ||)

    // Multi-char operators (all accepting)
    private static final int S_PLUS_PLUS = 33;
    private static final int S_MINUS_MINUS = 34;
    private static final int S_STAR_STAR = 35;
    private static final int S_PLUS_EQ = 36;
    private static final int S_MINUS_EQ = 37;
    private static final int S_STAR_EQ = 38;
    private static final int S_SLASH_EQ = 39;
    private static final int S_EQ_EQ = 40;
    private static final int S_BANG_EQ = 41;
    private static final int S_LT_EQ = 42;
    private static final int S_GT_EQ = 43;
    private static final int S_AMP_AMP = 44;
    private static final int S_PIPE_PIPE = 45;

    // Punctuator
    private static final int S_PUNCT = 46;

    // Sign + digits: [+-][0-9]+...
    private static final int S_SIGN_INT = 47;
    private static final int S_SIGN_DOT = 48;
    private static final int S_SIGN_FRAC = 49;
    private static final int S_SIGN_EXP_E = 50;
    private static final int S_SIGN_EXP_SIGN = 51;
    private static final int S_SIGN_EXP_DIG = 52;

    // (S_INVALID_START removed — @/$ treated as single invalid chars)

    // ==================== KEYWORD/BOOLEAN LOOKUP ====================
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "start", "finish", "loop", "condition", "declare", "output",
            "input", "function", "return", "break", "continue", "else"));
    private static final Set<String> BOOLEANS = new HashSet<>(Arrays.asList("true", "false"));

    // ==================== CONSTRUCTOR ====================
    public ManualScanner(String filePath, SymbolTable symbolTable, ErrorHandler errorHandler) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));
        this.input = new String(bytes);
        this.currentPos = 0;
        this.line = 1;
        this.column = 1;
        this.symbolTable = symbolTable;
        this.errorHandler = errorHandler;
        this.linesProcessed = this.input.length() - this.input.replace("\n", "").length() + 1;
    }

    // ==================== DFA TRANSITION FUNCTION ====================
    // Pure function: (current state, input character, chars consumed so far) ->
    // next state
    private int transition(int state, char c, int charsConsumed) {
        switch (state) {
            case S_START:
                if (c >= 'A' && c <= 'Z')
                    return S_IDENT;
                if (c >= 'a' && c <= 'z')
                    return S_WORD;
                if (c >= '0' && c <= '9')
                    return S_INT;
                if (c == '"')
                    return S_STR;
                if (c == '\'')
                    return S_CHR;
                if (c == '#')
                    return S_HASH;
                if (c == '+')
                    return S_PLUS;
                if (c == '-')
                    return S_MINUS;
                if (c == '*')
                    return S_STAR;
                if (c == '/')
                    return S_SLASH;
                if (c == '%')
                    return S_PCT;
                if (c == '=')
                    return S_EQ;
                if (c == '!')
                    return S_BANG;
                if (c == '<')
                    return S_LT;
                if (c == '>')
                    return S_GT;
                if (c == '&')
                    return S_AMP;
                if (c == '|')
                    return S_PIPE;
                if ("(){}[],;:".indexOf(c) >= 0)
                    return S_PUNCT;
                return DEAD;

            // --- Identifiers ---
            // Identifier: [A-Z][a-z0-9]{0,30} — max 31 total chars
            case S_IDENT:
                if (((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) && charsConsumed < 31)
                    return S_IDENT;
                return DEAD;

            // --- Lowercase words ---
            case S_WORD:
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))
                    return S_WORD;
                return DEAD;

            // --- Integer / Float ---
            case S_INT:
                if (c >= '0' && c <= '9')
                    return S_INT;
                if (c == '.')
                    return S_DOT;
                if (c == 'e' || c == 'E')
                    return S_EXP_E;
                return DEAD;
            case S_DOT:
                if (c >= '0' && c <= '9')
                    return S_FRAC;
                return DEAD;
            case S_FRAC:
                if (c >= '0' && c <= '9')
                    return S_FRAC;
                if (c == 'e' || c == 'E')
                    return S_EXP_E;
                return DEAD;
            case S_EXP_E:
                if (c == '+' || c == '-')
                    return S_EXP_SIGN;
                if (c >= '0' && c <= '9')
                    return S_EXP_DIG;
                return DEAD;
            case S_EXP_SIGN:
                if (c >= '0' && c <= '9')
                    return S_EXP_DIG;
                return DEAD;
            case S_EXP_DIG:
                if (c >= '0' && c <= '9')
                    return S_EXP_DIG;
                return DEAD;

            // --- String literal ---
            case S_STR:
                if (c == '"')
                    return S_STR_DONE;
                if (c == '\\')
                    return S_STR_ESC;
                if (c == '\n' || c == '\r')
                    return DEAD;
                return S_STR;
            case S_STR_ESC:
                if (c == '"' || c == '\\' || c == 'n' || c == 't' || c == 'r')
                    return S_STR;
                return DEAD;
            case S_STR_DONE:
                return DEAD;

            // --- Char literal ---
            case S_CHR:
                if (c == '\\')
                    return S_CHR_ESC;
                if (c == '\'' || c == '\n' || c == '\r')
                    return DEAD;
                return S_CHR_CH;
            case S_CHR_CH:
                if (c == '\'')
                    return S_CHR_DONE;
                return DEAD;
            case S_CHR_ESC:
                if (c == '\\' || c == '\'' || c == 'n' || c == 't' || c == 'r')
                    return S_CHR_ESC_CH;
                return DEAD;
            case S_CHR_ESC_CH:
                if (c == '\'')
                    return S_CHR_DONE;
                return DEAD;
            case S_CHR_DONE:
                return DEAD;

            // --- Comments ---
            case S_HASH:
                if (c == '#')
                    return S_CMT_SL;
                if (c == '*')
                    return S_CMT_ML;
                return DEAD;
            case S_CMT_SL:
                if (c == '\n' || c == '\r')
                    return DEAD;
                return S_CMT_SL;
            case S_CMT_ML:
                if (c == '*')
                    return S_CMT_ML_STAR;
                return S_CMT_ML;
            case S_CMT_ML_STAR:
                if (c == '#')
                    return S_CMT_ML_DONE;
                if (c == '*')
                    return S_CMT_ML_STAR;
                return S_CMT_ML;
            case S_CMT_ML_DONE:
                return DEAD;

            // --- Operators ---
            case S_PLUS:
                if (c == '+')
                    return S_PLUS_PLUS;
                if (c == '=')
                    return S_PLUS_EQ;
                if (c >= '0' && c <= '9')
                    return S_SIGN_INT;
                return DEAD;
            case S_MINUS:
                if (c == '-')
                    return S_MINUS_MINUS;
                if (c == '=')
                    return S_MINUS_EQ;
                if (c >= '0' && c <= '9')
                    return S_SIGN_INT;
                return DEAD;
            case S_STAR:
                if (c == '*')
                    return S_STAR_STAR;
                if (c == '=')
                    return S_STAR_EQ;
                return DEAD;
            case S_SLASH:
                if (c == '=')
                    return S_SLASH_EQ;
                return DEAD;
            case S_EQ:
                if (c == '=')
                    return S_EQ_EQ;
                return DEAD;
            case S_BANG:
                if (c == '=')
                    return S_BANG_EQ;
                return DEAD;
            case S_LT:
                if (c == '=')
                    return S_LT_EQ;
                return DEAD;
            case S_GT:
                if (c == '=')
                    return S_GT_EQ;
                return DEAD;
            case S_AMP:
                if (c == '&')
                    return S_AMP_AMP;
                return DEAD;
            case S_PIPE:
                if (c == '|')
                    return S_PIPE_PIPE;
                return DEAD;

            // All double operators and punctuator are terminal
            case S_PLUS_PLUS:
            case S_MINUS_MINUS:
            case S_STAR_STAR:
            case S_PLUS_EQ:
            case S_MINUS_EQ:
            case S_STAR_EQ:
            case S_SLASH_EQ:
            case S_EQ_EQ:
            case S_BANG_EQ:
            case S_LT_EQ:
            case S_GT_EQ:
            case S_AMP_AMP:
            case S_PIPE_PIPE:
            case S_PCT:
            case S_PUNCT:
                return DEAD;

            // --- Sign + number states ---
            case S_SIGN_INT:
                if (c >= '0' && c <= '9')
                    return S_SIGN_INT;
                if (c == '.')
                    return S_SIGN_DOT;
                if (c == 'e' || c == 'E')
                    return S_SIGN_EXP_E;
                return DEAD;
            case S_SIGN_DOT:
                if (c >= '0' && c <= '9')
                    return S_SIGN_FRAC;
                return DEAD;
            case S_SIGN_FRAC:
                if (c >= '0' && c <= '9')
                    return S_SIGN_FRAC;
                if (c == 'e' || c == 'E')
                    return S_SIGN_EXP_E;
                return DEAD;
            case S_SIGN_EXP_E:
                if (c == '+' || c == '-')
                    return S_SIGN_EXP_SIGN;
                if (c >= '0' && c <= '9')
                    return S_SIGN_EXP_DIG;
                return DEAD;
            case S_SIGN_EXP_SIGN:
                if (c >= '0' && c <= '9')
                    return S_SIGN_EXP_DIG;
                return DEAD;
            case S_SIGN_EXP_DIG:
                if (c >= '0' && c <= '9')
                    return S_SIGN_EXP_DIG;
                return DEAD;

            default:
                return DEAD;
        }
    }

    // ==================== ACCEPT STATE MAPPING ====================
    // Returns TokenType for accepting states, null for non-accepting
    private TokenType acceptType(int state, String lexeme) {
        switch (state) {
            case S_IDENT:
                if (lexeme.length() > 31)
                    return TokenType.UNKNOWN; // too long
                return TokenType.IDENTIFIER;
            case S_WORD:
                if (KEYWORDS.contains(lexeme))
                    return TokenType.KEYWORD;
                if (BOOLEANS.contains(lexeme))
                    return TokenType.BOOLEAN_LITERAL;
                return TokenType.UNKNOWN; // invalid identifier
            case S_INT:
            case S_SIGN_INT:
                return TokenType.INTEGER_LITERAL;
            case S_FRAC:
            case S_EXP_DIG:
            case S_SIGN_FRAC:
            case S_SIGN_EXP_DIG:
                return TokenType.FLOAT_LITERAL;
            case S_STR_DONE:
                return TokenType.STRING_LITERAL;
            case S_CHR_DONE:
                return TokenType.CHAR_LITERAL;
            case S_CMT_SL:
            case S_CMT_ML_DONE:
                return TokenType.COMMENT;
            case S_PLUS:
            case S_MINUS:
            case S_STAR:
            case S_SLASH:
            case S_PCT:
            case S_EQ:
            case S_BANG:
            case S_LT:
            case S_GT:
            case S_PLUS_PLUS:
            case S_MINUS_MINUS:
            case S_STAR_STAR:
            case S_PLUS_EQ:
            case S_MINUS_EQ:
            case S_STAR_EQ:
            case S_SLASH_EQ:
            case S_EQ_EQ:
            case S_BANG_EQ:
            case S_LT_EQ:
            case S_GT_EQ:
            case S_AMP_AMP:
            case S_PIPE_PIPE:
                return TokenType.OPERATOR;
            case S_PUNCT:
                return TokenType.PUNCTUATOR;
            default:
                return null; // non-accepting state
        }
    }

    // Returns error message for UNKNOWN token types
    private String errorMessage(int state, String lexeme) {
        switch (state) {
            case S_IDENT:
                if (lexeme.length() > 31)
                    return "Identifier exceeds maximum length of 31 characters";
                return null;
            case S_WORD:
                if (!KEYWORDS.contains(lexeme) && !BOOLEANS.contains(lexeme))
                    return "Invalid identifier (must start with uppercase)";
                return null;
            default:
                return null;
        }
    }

    // ==================== MAIN TOKENIZATION (DFA DRIVER) ====================
    public Token nextToken() {
        // Skip whitespace
        while (currentPos < input.length()) {
            char c = input.charAt(currentPos);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                updatePositionSingle(c);
                currentPos++;
            } else {
                break;
            }
        }

        if (currentPos >= input.length()) {
            return new Token(TokenType.EOF, "", line, column);
        }

        int startLine = line;
        int startCol = column;

        // DFA longest-match loop
        int state = S_START;
        int lastAcceptState = DEAD;
        int lastAcceptPos = -1;
        // Save pos at each position for backtracking
        int pos = currentPos;

        while (pos < input.length()) {
            char c = input.charAt(pos);
            int nextState = transition(state, c, pos - currentPos);

            if (nextState == DEAD)
                break;

            state = nextState;
            pos++;

            // Track line/column for the character just consumed
            // (We'll update the real line/column after we determine the match)

            // Check if this is an accepting state
            String tentativeLexeme = input.substring(currentPos, pos);
            TokenType type = acceptType(state, tentativeLexeme);
            if (type != null) {
                lastAcceptState = state;
                lastAcceptPos = pos;
            }
        }

        // If no accepting state was ever reached, handle as error
        if (lastAcceptState == DEAD) {
            // Check for special error patterns
            String invalidChar = String.valueOf(input.charAt(currentPos));

            // Check for unterminated string
            if (input.charAt(currentPos) == '"') {
                int end = currentPos + 1;
                while (end < input.length() && input.charAt(end) != '\n' && input.charAt(end) != '\r')
                    end++;
                String badStr = input.substring(currentPos, end);
                errorHandler.reportError("Lexical Error", startLine, startCol, badStr, "Unterminated string literal");
                updatePosition(badStr);
                currentPos = end;
                Token errorToken = new Token(TokenType.UNKNOWN, badStr, startLine, startCol);
                updateStats(errorToken);
                return errorToken;
            }

            // Check for unclosed multi-line comment
            if (currentPos + 1 < input.length() && input.charAt(currentPos) == '#'
                    && input.charAt(currentPos + 1) == '*') {
                String badComment = input.substring(currentPos);
                errorHandler.reportError("Lexical Error", startLine, startCol, badComment,
                        "Unclosed multi-line comment");
                updatePosition(badComment);
                currentPos = input.length();
                Token errorToken = new Token(TokenType.UNKNOWN, badComment, startLine, startCol);
                updateStats(errorToken);
                return errorToken;
            }

            errorHandler.reportError("Invalid Character", startLine, startCol, invalidChar,
                    "Character not recognized by scanner");
            updatePositionSingle(input.charAt(currentPos));
            currentPos++;
            Token errorToken = new Token(TokenType.UNKNOWN, invalidChar, startLine, startCol);
            updateStats(errorToken);
            return errorToken;
        }

        // We have a match — extract the lexeme
        String lexeme = input.substring(currentPos, lastAcceptPos);
        TokenType type = acceptType(lastAcceptState, lexeme);

        // Update position tracking
        updatePosition(lexeme);
        currentPos = lastAcceptPos;

        // Handle UNKNOWN (error tokens) — report and return
        if (type == TokenType.UNKNOWN) {
            String errMsg = errorMessage(lastAcceptState, lexeme);
            if (errMsg != null) {
                errorHandler.reportError("Lexical Error", startLine, startCol, lexeme, errMsg);
            }
            Token errorToken = new Token(TokenType.UNKNOWN, lexeme, startLine, startCol);
            updateStats(errorToken);
            return errorToken;
        }

        // Handle comments — skip but count
        if (type == TokenType.COMMENT) {
            commentsRemoved++;
            return nextToken();
        }

        // Normal token
        Token token = new Token(type, lexeme, startLine, startCol);

        // Update symbol table for identifiers
        if (type == TokenType.IDENTIFIER) {
            symbolTable.addIdentifier(lexeme, startLine);
        }

        updateStats(token);
        return token;
    }

    private void updateStats(Token token) {
        totalTokens++;
        tokenCounts.put(token.getType(), tokenCounts.getOrDefault(token.getType(), 0) + 1);
    }

    // ==================== POSITION TRACKING ====================
    private void updatePosition(String text) {
        for (char c : text.toCharArray()) {
            updatePositionSingle(c);
        }
    }

    private void updatePositionSingle(char c) {
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
    }

    // ==================== STATISTICS ====================
    public void printStats() {
        System.out.println("\nScanner Statistics:");
        System.out.println("Total Tokens: " + totalTokens);
        System.out.println("Lines Processed: " + linesProcessed);
        System.out.println("Comments Removed: " + commentsRemoved);
        for (TokenType t : tokenCounts.keySet()) {
            System.out.println(t + ": " + tokenCounts.get(t));
        }
    }
}
