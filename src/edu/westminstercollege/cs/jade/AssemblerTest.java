package edu.westminstercollege.cs.jade;

import edu.westminstercollege.cs.jade.assembler.Assembler;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.io.StringReader;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class AssemblerTest {

    private static final String CODE = """
            .source Test.java
            .class public Test
            .super java/lang/Object
            
            .method public static main ([Ljava/lang/String;)V
            .code
                .limit locals 2
                .limit stack 100
                
                getstatic java/lang/System/out Ljava/io/PrintStream;
                ldc "Hello, world!"
                invokevirtual java/io/PrintStream/println (Ljava/lang/String;)V
                
                iconst_0
                istore_1
                
                iinc 1 1
                iinc 1 1
                iinc 1 1
                
                getstatic java/lang/System/out Ljava/io/PrintStream;
                iload_1
                invokevirtual java/io/PrintStream/println (I)V
                
                return
            .end code
            """;

    public static void main(String... args) throws IOException, SyntaxException {
       try (var channel = Files.newByteChannel(Path.of("Test.class"), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
           var buffer = ((FileChannel)channel).map(FileChannel.MapMode.READ_WRITE, 0, 65536 /* ??? */);
           var assembler = new Assembler();
           assembler.assemble(new StringReader(CODE), buffer);
           channel.truncate(buffer.position());
       }
    }
}
