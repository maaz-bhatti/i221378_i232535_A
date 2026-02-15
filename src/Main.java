package src;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String baseDir = System.getProperty("user.dir");
        String[] testFiles = {
                baseDir + "/tests/test1.txt",
                baseDir + "/tests/test2.txt",
                baseDir + "/tests/test3.txt",
                baseDir + "/tests/test4.txt",
                baseDir + "/tests/test5.txt"
        };

        // If argument provided, just run that one (legacy mode or specific test)
        if (args.length > 0) {
            runComparison(args[0]);
        } else {
            // Run all
            for (String file : testFiles) {
                runComparison(file);
            }
        }
    }

    private static void runComparison(String fileName) {
        System.out.println("==================================================");
        System.out.println("COMPARISON REPORT FOR: " + fileName);
        System.out.println("==================================================");

        ErrorHandler errorHandler = new ErrorHandler(); // Shared error handler

        // 1. Run Manual Scanner
        long manualStartTime = System.nanoTime();
        List<String> manualTokens = new ArrayList<>();

        System.out.println("\n[Manual Scanner Output]");
        SymbolTable symbolTable = new SymbolTable();
        try {
            ManualScanner manual = new ManualScanner(fileName, symbolTable, errorHandler);
            Token token;
            while ((token = manual.nextToken()).getType() != TokenType.EOF) {
                String t = token.toString();
                System.out.println(t);
                manualTokens.add(t);
            }
            manual.printStats();
            symbolTable.display();
        } catch (Exception e) {
            System.err.println("Manual Scanner Error: " + e.getMessage());
        }
        long manualDuration = System.nanoTime() - manualStartTime;

        // 2. Run JFlex Scanner
        long jflexStartTime = System.nanoTime();
        List<String> jflexTokens = new ArrayList<>();
        int jflexTotalTokens = 0;
        java.util.Map<TokenType, Integer> jflexTokenCounts = new java.util.EnumMap<>(TokenType.class);

        System.out.println("\n[JFlex Scanner Output]");
        try (java.io.FileReader reader = new java.io.FileReader(fileName)) {
            Scanner jflex = new Scanner(reader);
            jflex.setErrorHandler(errorHandler);
            Token token;
            while ((token = jflex.yylex()) != null) {
                jflexTotalTokens++;
                jflexTokenCounts.put(token.getType(),
                        jflexTokenCounts.getOrDefault(token.getType(), 0) + 1);
                String t = token.toString();
                System.out.println(t);
                jflexTokens.add(t);
            }

            // Print JFlex Statistics
            System.out.println("\nJFlex Scanner Statistics:");
            System.out.println("Total Tokens: " + jflexTotalTokens);
            for (TokenType t : jflexTokenCounts.keySet()) {
                System.out.println(t + ": " + jflexTokenCounts.get(t));
            }
        } catch (Error e) {
            // JFlex throws Error on lexical error sometimes
            System.err.println("JFlex Lexical Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("JFlex Error: " + e.getMessage());
        }
        long jflexDuration = System.nanoTime() - jflexStartTime;

        // 3. Comparison Logic
        System.out.println("\n[Performance Comparison]");
        System.out.printf("Manual Scanner Time: %.3f ms%n", manualDuration / 1e6);
        System.out.printf("JFlex Scanner Time:  %.3f ms%n", jflexDuration / 1e6);

        System.out.println("\n[Output Correctness]");
        if (manualTokens.equals(jflexTokens)) {
            System.out.println("[PASS] Outputs MATCH exactly.");
        } else {
            System.out.println("[FAIL] Outputs DIFFER.");
            // Print diff?
            int minLen = Math.min(manualTokens.size(), jflexTokens.size());
            for (int i = 0; i < minLen; i++) {
                if (!manualTokens.get(i).equals(jflexTokens.get(i))) {
                    System.out.println("Difference at Token #" + (i + 1));
                    System.out.println("  Manual: " + manualTokens.get(i));
                    System.out.println("  JFlex:  " + jflexTokens.get(i));
                }
            }
            if (manualTokens.size() != jflexTokens.size()) {
                System.out.println(
                        "Token count mismatch: Manual=" + manualTokens.size() + ", JFlex=" + jflexTokens.size());
            }
        }
        System.out.println("\n");
    }
}
