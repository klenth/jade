package edu.westminstercollege.cs.jade;

import org.antlr.v4.runtime.CharStreams;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LexerTest {

    private static final String TEST_INPUT = """
            .source Stuff.java
            
            .class public abstract Badger
            .super java/lang/Object
            .implements java/util/Comparable
            .implements java/io/Serializable
            
            .field private static in Ljava/util/Scanner;
            
            .method public <init> ()V
            .code
                top: pop
                ldc "Hi there!"
                ldc "Hi\\nthere!"
                ldc "Hi\\\\there!"
                ldc "Hi\\u2346there!"
                ldc "Hi\\0there!"
                bipush 25
                bipush -25
                bipush 0xcafebabe
                ldc 234L
                ldc -1.
                ldc -.1
                ldc +1.5
                ldc 1e5
                ldc -1E+05
            .end code
            """;
    private static final Map<Integer, String> TOKEN_TYPES;

    static {
        TOKEN_TYPES = new HashMap<>();
        try (var tokensIn = Files.newBufferedReader(Path.of("gen/edu/westminstercollege/cs/jade/JvmAssemblyLexer.tokens"))) {
            tokensIn.lines()
                    .map(s -> s.split("="))
                    .filter(w -> !w[0].startsWith("'"))
                    .forEach(w -> {
                        String name = w[0];
                        int value = Integer.parseInt(w[1]);
                        if (TOKEN_TYPES.containsKey(value))
                            System.err.printf("Warning: duplicate token type %d (already have %s, now getting %s)\n", value, TOKEN_TYPES.get(value), name);
                        else
                            TOKEN_TYPES.put(value, name);
                    });
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String... args) {
        //var lexer = new JvmAssemblyLexer(CharStreams.fromString(TEST_INPUT));
        var lexer = new JvmAssemblyLexer(CharStreams.fromString("bipush"));
        lexer.pushMode(JvmAssemblyLexer.M_CODE);

        while (true) {
            var token = lexer.nextToken();

            if (token.getType() == JvmAssemblyLexer.EOF)
                break;
            String tokenType = tokenType(token.getType());
            System.out.printf("[%s] '%s'\n", tokenType, token.getText());
        }
    }

    private static String tokenType(int type) {
        return TOKEN_TYPES.getOrDefault(type, "???");
    }
}
