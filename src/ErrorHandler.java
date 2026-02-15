package src;

public class ErrorHandler {

    public void reportError(String errorType, int line, int column, String lexeme, String reason) {
        System.err.printf("Error: [%s] at Line %d, Column %d%n", errorType, line, column);
        System.err.printf("    Lexeme: \"%s\"%n", lexeme);
        System.err.printf("    Reason: %s%n", reason);
        System.err.println("--------------------------------------------------");
    }
}
