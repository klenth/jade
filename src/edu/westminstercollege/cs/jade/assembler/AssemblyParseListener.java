package edu.westminstercollege.cs.jade.assembler;

import static edu.westminstercollege.cs.jade.assembler.Node.*;

import edu.westminstercollege.cs.jade.JvmAssemblyLexer;
import edu.westminstercollege.cs.jade.JvmAssemblyParser;
import edu.westminstercollege.cs.jade.JvmAssemblyParserBaseListener;
import edu.westminstercollege.cs.jade.classfile.AccessFlag;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class AssemblyParseListener extends JvmAssemblyParserBaseListener {

    AsmFile file;
    private List<Directive> fileDirectives;
    private Directive directive;

    private Code methodCode;
    private List<CodeLine> codeLines;
    private CodeLine codeLine;

    private String instrOpcode;
    private List<Operand> instrOperands;

    @Override
    public void enterAssemblyFile(JvmAssemblyParser.AssemblyFileContext ctx) {
        fileDirectives = new ArrayList<>();
    }

    @Override
    public void exitAssemblyFile(JvmAssemblyParser.AssemblyFileContext ctx) {
        file = new AsmFile(ctx, fileDirectives);
    }

    @Override
    public void enterTopLevelDirective(JvmAssemblyParser.TopLevelDirectiveContext ctx) {
        directive = null;
    }

    @Override
    public void exitTopLevelDirective(JvmAssemblyParser.TopLevelDirectiveContext ctx) {
        fileDirectives.add(directive);
    }

    @Override
    public void enterSourceDirective(JvmAssemblyParser.SourceDirectiveContext ctx) {
        // do nothing
    }

    @Override
    public void exitSourceDirective(JvmAssemblyParser.SourceDirectiveContext ctx) {
        directive = new SourceDirective(ctx, ctx.FILENAME().getText());
    }

    @Override
    public void enterClassDirective(JvmAssemblyParser.ClassDirectiveContext ctx) {
        // do nothing
    }

    @Override
    public void exitClassDirective(JvmAssemblyParser.ClassDirectiveContext ctx) {
        var type = switch (ctx.DIR_CLASS().getText()) {
            case ".class" -> ClassDirective.Type.Class;
            case ".interface" -> ClassDirective.Type.Interface;
            case ".enum" -> ClassDirective.Type.Enum;
            case ".annotation" -> ClassDirective.Type.Annotation;
            case ".module" -> ClassDirective.Type.Module;
            default -> throw new RuntimeException(String.format("Internal assembler exception: unknown class directive %s", ctx.DIR_CLASS().getText()));
        };

        var flags = ctx.CLASS_FLAG().stream()
                .map(TerminalNode::getText)
                .map(this::accessFlag)
                .toList();

        directive = new ClassDirective(ctx, type, flags, ctx.CLASS_ID().getText());
    }

    @Override
    public void enterSuperDirective(JvmAssemblyParser.SuperDirectiveContext ctx) {
        directive = null;
    }

    @Override
    public void exitSuperDirective(JvmAssemblyParser.SuperDirectiveContext ctx) {
        directive = new SuperDirective(ctx, ctx.CLASSNAME().getText());
    }

    @Override
    public void enterImplementsDirective(JvmAssemblyParser.ImplementsDirectiveContext ctx) {
        directive = null;
    }

    @Override
    public void exitImplementsDirective(JvmAssemblyParser.ImplementsDirectiveContext ctx) {
        directive = new ImplementsDirective(ctx, ctx.CLASSNAME().getText());
    }

    @Override
    public void enterFieldDirective(JvmAssemblyParser.FieldDirectiveContext ctx) {
        directive = null;
    }

    @Override
    public void exitFieldDirective(JvmAssemblyParser.FieldDirectiveContext ctx) {
        var flags = ctx.FIELD_FLAG().stream()
                .map(TerminalNode::getText)
                .map(this::accessFlag)
                .toList();

        directive = new FieldDirective(ctx, flags, ctx.FIELD_ID().getText(), ctx.FIELD_DESC().getText());
    }

    @Override
    public void enterMethodDirective(JvmAssemblyParser.MethodDirectiveContext ctx) {
        directive = null;
        methodCode = null;
    }

    @Override
    public void exitMethodDirective(JvmAssemblyParser.MethodDirectiveContext ctx) {
        var flags = ctx.METHOD_FLAG().stream()
                .map(TerminalNode::getText)
                .map(this::accessFlag)
                .toList();

        directive = new MethodDirective(ctx, flags, ctx.METHOD_ID().getText(), ctx.METHOD_DESC().getText(),
                Optional.ofNullable(methodCode));
    }

    @Override
    public void enterMethodCode(JvmAssemblyParser.MethodCodeContext ctx) {
        codeLines = new ArrayList<>();
    }

    @Override
    public void exitMethodCode(JvmAssemblyParser.MethodCodeContext ctx) {
        methodCode = new Code(ctx, codeLines);
    }

    @Override
    public void enterCodeLine(JvmAssemblyParser.CodeLineContext ctx) {
        codeLine = null;
    }

    @Override
    public void exitCodeLine(JvmAssemblyParser.CodeLineContext ctx) {
        codeLines.add(codeLine);
    }

    @Override
    public void enterLimitLocals(JvmAssemblyParser.LimitLocalsContext ctx) {
        // do nothing
    }

    @Override
    public void exitLimitLocals(JvmAssemblyParser.LimitLocalsContext ctx) {
        codeLine = new LimitLocals(ctx, Integer.parseInt(ctx.INT().getText()));
    }

    @Override
    public void enterLimitStack(JvmAssemblyParser.LimitStackContext ctx) {
        // do nothing
    }

    @Override
    public void exitLimitStack(JvmAssemblyParser.LimitStackContext ctx) {
        codeLine = new LimitStack(ctx, Integer.parseInt(ctx.INT().getText()));
    }

    @Override
    public void enterLabelInstruction(JvmAssemblyParser.LabelInstructionContext ctx) {
        // do nothing
    }

    @Override
    public void exitLabelInstruction(JvmAssemblyParser.LabelInstructionContext ctx) {
        String maybeLabel = null;
        if (ctx.CODE_WORD() != null)
            maybeLabel = ctx.CODE_WORD().getText();

        codeLine = new Instruction(ctx, Optional.ofNullable(maybeLabel), instrOpcode, instrOperands);
    }

    @Override
    public void enterInstruction(JvmAssemblyParser.InstructionContext ctx) {
        instrOpcode = null;
        instrOperands = new ArrayList<>();
    }

    @Override
    public void exitInstruction(JvmAssemblyParser.InstructionContext ctx) {
        instrOpcode = ctx.CODE_WORD().getText();
    }

    @Override
    public void enterOperand(JvmAssemblyParser.OperandContext ctx) {
        // do nothing
    }

    @Override
    public void exitOperand(JvmAssemblyParser.OperandContext ctx) {
        Operand operand = null;
        if (ctx.INT() != null)
            operand = new Operand.Int(ctx, ctx.INT().getText());
        else if (ctx.LONG() != null)
            operand = new Operand.Long(ctx, ctx.LONG().getText());
        else if (ctx.DOUBLE() != null)
            operand = new Operand.Double(ctx, ctx.DOUBLE().getText());
        else if (ctx.FLOAT() != null)
            operand = new Operand.Float(ctx, ctx.FLOAT().getText());
        else if (ctx.CODE_WORD() != null)
            operand = new Operand.Word(ctx, ctx.CODE_WORD().getText());
        else if (ctx.STRING_STRING() != null)
            operand = new Operand.Str(ctx, ctx.STRING_STRING().getText());
        else
            throw new RuntimeException(String.format("Internal assembler error: unknown instruction operand " + ctx.getText()));

        instrOperands.add(operand);
    }

    private AccessFlag accessFlag(String text) {
        return AccessFlag.valueOf("ACC_" + text.toUpperCase());
    }

    private static final String TEST_INPUT = """
            .source String.java
            .class public final java/lang/String
            .super java/lang/Object
            .implements java/io/Serializable
            .implements java/lang/Comparable
            .implements java/lang/CharSequence
            .implements java/lang/constant/Constable
            .implements java/lang/constant/ConstantDesc
                        
            .field private final value [B
            .field private final coder B
            .field private hash I
            .field private hashIsZero Z
            .field private static final serialVersionUID J
            .field static final COMPACT_STRINGS Z
            .field private static final serialPersistentFields [Ljava/io/ObjectStreamField;
            .field private static final REPL C
            .field public static final CASE_INSENSITIVE_ORDER Ljava/util/Comparator;
            .field static final LATIN1 B
            .field static final UTF16 B
                        
            .method public <init> ()V
            .code
            .limit locals 1
            .limit stack 2
                          aload_0
                          invokespecial         java/lang/Object/<init> ()V
                          aload_0
                          ldc                   ""
                          getfield              java/lang/String/value [B
                          putfield              java/lang/String/value [B
                          aload_0
                          ldc                   ""
                          getfield              java/lang/String/coder B
                          putfield              java/lang/String/coder B
                          return
            .end code
                        
            .method public <init> (Ljava/lang/String;)V
            .code
            .limit locals 2
            .limit stack 2
                           aload_0
                           invokespecial         java/lang/Object/<init> ()V
                           aload_0
                           aload_1
                           getfield              java/lang/String/value [B
                           putfield              java/lang/String/value [B
                          aload_0
                          aload_1
                          getfield              java/lang/String/coder B
                          putfield              java/lang/String/coder B
                          aload_0
                          aload_1
                          getfield              java/lang/String/hash I
                          putfield              java/lang/String/hash I
                          aload_0
                          aload_1
                          getfield              java/lang/String/hashIsZero Z
                          putfield              java/lang/String/hashIsZero Z
                          return
            .end code
                        
            .method public <init> ([C)V
            .code
            .limit locals 2
            .limit stack 5
                           aload_0
                           aload_1
                           iconst_0
                           aload_1
                           arraylength
                           aconst_null
                           invokespecial         java/lang/String/<init> ([CIILjava/lang/Void;)V
                           return
            .end code
                        
            .method public <init> ([CII)V
            .code
            .limit locals 4
            .limit stack 7
                           aload_0
                           aload_1
                           iload_2
                           iload_3
                           aload_1
                           iload_2
                           iload_3
                           invokestatic          java/lang/String/rangeCheck ([CII)Ljava/lang/Void;
                          invokespecial         java/lang/String/<init> ([CIILjava/lang/Void;)V
                          return
            .end code
                        
            .method private static rangeCheck ([CII)Ljava/lang/Void;
            .code
            .limit locals 3
            .limit stack 3
                           iload_1
                           iload_2
                           aload_0
                           arraylength
                           invokestatic          java/lang/String/checkBoundsOffCount (III)V
                           aconst_null
                           areturn
            .end code
                        
            .method public <init> ([III)V
            .code
            .limit locals 5
            .limit stack 4
                           aload_0
                           invokespecial         java/lang/Object/<init> ()V
                           iload_2
                           iload_3
                           aload_1
                           arraylength
                           invokestatic          java/lang/String/checkBoundsOffCount (III)V
                          iload_3
                          ifne                  34
                          aload_0
                          ldc                   ""
                          getfield              java/lang/String/value [B
                          putfield              java/lang/String/value [B
                          aload_0
                          ldc                   ""
                          getfield              java/lang/String/coder B
                          putfield              java/lang/String/coder B
                          return
                          getstatic             java/lang/String/COMPACT_STRINGS Z
                          ifeq                  65
                          aload_1
                          iload_2
                          iload_3
                          invokestatic          java/lang/StringLatin1/toBytes ([III)[B
                          astore                4
                          aload                 4
                          ifnull                65
                          aload_0
                          iconst_0
                          putfield              java/lang/String/coder B
                          aload_0
                          aload                 4
                          putfield              java/lang/String/value [B
                          return
                          aload_0
                          iconst_1
                          putfield              java/lang/String/coder B
                          aload_0
                          aload_1
                          iload_2
                          iload_3
                          invokestatic          java/lang/StringUTF16/toBytes ([III)[B
                          putfield              java/lang/String/value [B
                          return
            .end code
                        
            .method public <init> ([BIII)V
            .code
            .limit locals 7
            .limit stack 5
                           aload_0
                           invokespecial         java/lang/Object/<init> ()V
                           iload_3
                           iload                 4
                           aload_1
                           arraylength
                           invokestatic          java/lang/String/checkBoundsOffCount (III)V
                          iload                 4
                          ifne                  36
                          aload_0
                          ldc                   ""
                          getfield              java/lang/String/value [B
                          putfield              java/lang/String/value [B
                          aload_0
                          ldc                   ""
                          getfield              java/lang/String/coder B
                          putfield              java/lang/String/coder B
                          return
                          getstatic             java/lang/String/COMPACT_STRINGS Z
                          ifeq                  68
                          iload_2
                          i2b
                          ifne                  68
                          aload_0
                          aload_1
                          iload_3
                          iload_3
                          iload                 4
                          iadd
                          invokestatic          java/util/Arrays/copyOfRange ([BII)[B
                          putfield              java/lang/String/value [B
                          aload_0
                          iconst_0
                          putfield              java/lang/String/coder B
                          goto                  126
                          iload_2
                          bipush                8
                          ishl
                          istore_2
                          iload                 4
                          invokestatic          java/lang/StringUTF16/newBytesFor (I)[B
                          astore                5
                          iconst_0
                          istore                6
                          iload                 6
                          iload                 4
                          if_icmpge             115
                          aload                 5
                          iload                 6
                          iload_2
                          aload_1
                          iload_3
                          iinc                  3 1
                         baload
                         sipush                255
                         iand
                         ior
                         invokestatic          java/lang/StringUTF16/putChar ([BII)V
                         iinc                  6 1
                         goto                  83
                         aload_0
                         aload                 5
                         putfield              java/lang/String/value [B
                         aload_0
                         iconst_1
                         putfield              java/lang/String/coder B
                         return
            .end code
                        
            .method public <init> ([BI)V
            .code
            .limit locals 3
            .limit stack 5
                           aload_0
                           aload_1
                           iload_2
                           iconst_0
                           aload_1
                           arraylength
                           invokespecial         java/lang/String/<init> ([BIII)V
                           return
            .end code
                        
            .method public <init> ([BIILjava/lang/String;)V
            .code
            .limit locals 5
            .limit stack 5
                           aload_0
                           aload_1
                           iload_2
                           iload_3
                           aload                 4
                           invokestatic          java/lang/String/lookupCharset (Ljava/lang/String;)Ljava/nio/charset/Charset;
                           invokespecial         java/lang/String/<init> ([BIILjava/nio/charset/Charset;)V
                          return
            .end code
                        
            .method public <init> ([BIILjava/nio/charset/Charset;)V
            .code
            .limit locals 11
            .limit stack 6
                           aload_0
                           invokespecial         java/lang/Object/<init> ()V
                           aload                 4
                           invokestatic          java/util/Objects/requireNonNull (Ljava/lang/Object;)Ljava/lang/Object;
                           pop
                          iload_2
                          iload_3
                          aload_1
                          arraylength
                          invokestatic          java/lang/String/checkBoundsOffCount (III)V
                          iload_3
                          ifne                  42
                          aload_0
                          ldc                   ""
                          getfield              java/lang/String/value [B
                          putfield              java/lang/String/value [B
                          aload_0
                          ldc                   ""
                          getfield              java/lang/String/coder B
                          putfield              java/lang/String/coder B
                          goto                  883
                          aload                 4
                          getstatic             sun/nio/cs/UTF_8/INSTANCE Lsun/nio/cs/UTF_8;
                          if_acmpne             363
                          getstatic             java/lang/String/COMPACT_STRINGS Z
                          ifeq                  311
                          aload_1
                          iload_2
                          iload_3
                          invokestatic          java/lang/StringCoding/countPositives ([BII)I
                          istore                5
                          iload                 5
                          iload_3
                          if_icmpne             88
                          aload_0
                          aload_1
                          iload_2
                          iload_2
                          iload_3
                          iadd
                          invokestatic          java/util/Arrays/copyOfRange ([BII)[B
                          putfield              java/lang/String/value [B
                          aload_0
                          iconst_0
                          putfield              java/lang/String/coder B
                          return
                          iload_2
                          iload_3
                          iadd
                          istore                6
                          iload_3
                          newarray              byte
                          astore                7
                          iload                 5
                         ifle                  118
                         aload_1
                         iload_2
                         aload                 7
                         iconst_0
                         iload                 5
                         invokestatic          java/lang/System/arraycopy (Ljava/lang/Object;ILjava/lang/Object;II)V
                         iload_2
                         iload                 5
                         iadd
                         istore_2
                         iload_2
                         iload                 6
                         if_icmpge             209
                         aload_1
                         iload_2
                         iinc                  2 1
                         baload
                         istore                8
                         iload                 8
                         iflt                  151
                         aload                 7
                         iload                 5
                         iinc                  5 1
                         iload                 8
                         i2b
                         bastore
                         goto                  118
                         iload                 8
                         sipush                254
                         iand
                         sipush                194
                         if_icmpne             203
                         iload_2
                         iload                 6
                         if_icmpge             203
                         aload_1
                         iload_2
                         baload
                         istore                9
                         iload                 9
                         bipush                -64
                         if_icmpge             203
                         aload                 7
                         iload                 5
                         iinc                  5 1
                         iload                 8
                         iload                 9
                         invokestatic          java/lang/String/decode2 (II)C
                         i2b
                         bastore
                         iinc                  2 1
                         goto                  118
                         iinc                  2 -1
                         goto                  209
                         iload_2
                         iload                 6
                         if_icmpne             244
                         iload                 5
                         aload                 7
                         arraylength
                         if_icmpeq             232
                         aload                 7
                         iload                 5
                         invokestatic          java/util/Arrays/copyOf ([BI)[B
                         astore                7
                         aload_0
                         aload                 7
                         putfield              java/lang/String/value [B
                         aload_0
                         iconst_0
                         putfield              java/lang/String/coder B
                         return
                         iload_3
                         iconst_1
                         ishl
                         newarray              byte
                         astore                8
                         aload                 7
                         iconst_0
                         aload                 8
                         iconst_0
                         iload                 5
                         invokestatic          java/lang/StringLatin1/inflate ([BI[BII)V
                         aload                 8
                         astore                7
                         aload_1
                         iload_2
                         iload                 6
                         aload                 7
                         iload                 5
                         iconst_1
                         invokestatic          java/lang/String/decodeUTF8_UTF16 ([BII[BIZ)I
                         istore                5
                         iload                 5
                         iload_3
                         if_icmpeq             297
                         aload                 7
                         iload                 5
                         iconst_1
                         ishl
                         invokestatic          java/util/Arrays/copyOf ([BI)[B
                         astore                7
                         aload_0
                         aload                 7
                         putfield              java/lang/String/value [B
                         aload_0
                         iconst_1
                         putfield              java/lang/String/coder B
                         goto                  883
                         iload_3
                         iconst_1
                         ishl
                         newarray              byte
                         astore                5
                         aload_1
                         iload_2
                         iload_2
                         iload_3
                         iadd
                         aload                 5
                         iconst_0
                         iconst_1
                         invokestatic          java/lang/String/decodeUTF8_UTF16 ([BII[BIZ)I
                         istore                6
                         iload                 6
                         iload_3
                         if_icmpeq             349
                         aload                 5
                         iload                 6
                         iconst_1
                         ishl
                         invokestatic          java/util/Arrays/copyOf ([BI)[B
                         astore                5
                         aload_0
                         aload                 5
                         putfield              java/lang/String/value [B
                         aload_0
                         iconst_1
                         putfield              java/lang/String/coder B
                         goto                  883
                         aload                 4
                         getstatic             sun/nio/cs/ISO_8859_1/INSTANCE Lsun/nio/cs/ISO_8859_1;
                         if_acmpne             415
                         getstatic             java/lang/String/COMPACT_STRINGS Z
                         ifeq                  397
                         aload_0
                         aload_1
                         iload_2
                         iload_2
                         iload_3
                         iadd
                         invokestatic          java/util/Arrays/copyOfRange ([BII)[B
                         putfield              java/lang/String/value [B
                         aload_0
                         iconst_0
                         putfield              java/lang/String/coder B
                         goto                  883
                         aload_0
                         aload_1
                         iload_2
                         iload_3
                         invokestatic          java/lang/StringLatin1/inflate ([BII)[B
                         putfield              java/lang/String/value [B
                         aload_0
                         iconst_1
                         putfield              java/lang/String/coder B
                         goto                  883
                         aload                 4
                         getstatic             sun/nio/cs/US_ASCII/INSTANCE Lsun/nio/cs/US_ASCII;
                         if_acmpne             522
                         getstatic             java/lang/String/COMPACT_STRINGS Z
                         ifeq                  458
                         aload_1
                         iload_2
                         iload_3
                         invokestatic          java/lang/StringCoding/hasNegatives ([BII)Z
                         ifne                  458
                         aload_0
                         aload_1
                         iload_2
                         iload_2
                         iload_3
                         iadd
                         invokestatic          java/util/Arrays/copyOfRange ([BII)[B
                         putfield              java/lang/String/value [B
                         aload_0
                         iconst_0
                         putfield              java/lang/String/coder B
                         goto                  883
                         iload_3
                         iconst_1
                         ishl
                         newarray              byte
                         astore                5
                         iconst_0
                         istore                6
                         iload                 6
                         iload_3
                         if_icmpge             508
                         aload_1
                         iload_2
                         iinc                  2 1
                         baload
                         istore                7
                         aload                 5
                         iload                 6
                         iinc                  6 1
                         iload                 7
                         iflt                  500
                         iload                 7
                         i2c
                         goto                  502
                         ldc                   65533
                         invokestatic          java/lang/StringUTF16/putChar ([BII)V
                         goto                  468
                         aload_0
                         aload                 5
                         putfield              java/lang/String/value [B
                         aload_0
                         iconst_1
                         putfield              java/lang/String/coder B
                         goto                  883
                         aload                 4
                         invokevirtual         java/nio/charset/Charset/newDecoder ()Ljava/nio/charset/CharsetDecoder;
                         astore                5
                         aload                 5
                         instanceof            sun/nio/cs/ArrayDecoder
                         ifeq                  746
                         aload                 5
                         checkcast             sun/nio/cs/ArrayDecoder
                         astore                6
                         aload                 6
                         invokeinterface       sun/nio/cs/ArrayDecoder/isASCIICompatible ()Z 1 0
                         ifeq                  603
                         aload_1
                         iload_2
                         iload_3
                         invokestatic          java/lang/StringCoding/hasNegatives ([BII)Z
                         ifne                  603
                         getstatic             java/lang/String/COMPACT_STRINGS Z
                         ifeq                  587
                         aload_0
                         aload_1
                         iload_2
                         iload_2
                         iload_3
                         iadd
                         invokestatic          java/util/Arrays/copyOfRange ([BII)[B
                         putfield              java/lang/String/value [B
                         aload_0
                         iconst_0
                         putfield              java/lang/String/coder B
                         return
                         aload_0
                         aload_1
                         iload_2
                         iload_3
                         invokestatic          java/lang/StringLatin1/inflate ([BII)[B
                         putfield              java/lang/String/value [B
                         aload_0
                         iconst_1
                         putfield              java/lang/String/coder B
                         return
                         getstatic             java/lang/String/COMPACT_STRINGS Z
                         ifeq                  649
                         aload                 6
                         invokeinterface       sun/nio/cs/ArrayDecoder/isLatin1Decodable ()Z 1 0
                         ifeq                  649
                         iload_3
                         newarray              byte
                         astore                7
                         aload                 6
                         aload_1
                         iload_2
                         iload_3
                         aload                 7
                         invokeinterface       sun/nio/cs/ArrayDecoder/decodeToLatin1 ([BII[B)I 5 0
                         pop
                         aload_0
                         aload                 7
                         putfield              java/lang/String/value [B
                         aload_0
                         iconst_0
                         putfield              java/lang/String/coder B
                         return
                         iload_3
                         aload                 5
                         invokevirtual         java/nio/charset/CharsetDecoder/maxCharsPerByte ()F
                         invokestatic          java/lang/String/scale (IF)I
                         istore                7
                         aload                 5
                         getstatic             java/nio/charset/CodingErrorAction/REPLACE Ljava/nio/charset/CodingErrorAction;
                         invokevirtual         java/nio/charset/CharsetDecoder/onMalformedInput (Ljava/nio/charset/CodingErrorAction;)Ljava/nio/charset/CharsetDecoder;
                         getstatic             java/nio/charset/CodingErrorAction/REPLACE Ljava/nio/charset/CodingErrorAction;
                         invokevirtual         java/nio/charset/CharsetDecoder/onUnmappableCharacter (Ljava/nio/charset/CodingErrorAction;)Ljava/nio/charset/CharsetDecoder;
                         pop
                         iload                 7
                         newarray              char
                         astore                8
                         aload                 6
                         aload_1
                         iload_2
                         iload_3
                         aload                 8
                         invokeinterface       sun/nio/cs/ArrayDecoder/decode ([BII[C)I 5 0
                         istore                9
                         getstatic             java/lang/String/COMPACT_STRINGS Z
                         ifeq                  728
                         aload                 8
                         iconst_0
                         iload                 9
                         invokestatic          java/lang/StringUTF16/compress ([CII)[B
                         astore                10
                         aload                 10
                         ifnull                728
                         aload_0
                         aload                 10
                         putfield              java/lang/String/value [B
                         aload_0
                         iconst_0
                         putfield              java/lang/String/coder B
                         return
                         aload_0
                         iconst_1
                         putfield              java/lang/String/coder B
                         aload_0
                         aload                 8
                         iconst_0
                         iload                 9
                         invokestatic          java/lang/StringUTF16/toBytes ([CII)[B
                         putfield              java/lang/String/value [B
                         return
                         iload_3
                         aload                 5
                         invokevirtual         java/nio/charset/CharsetDecoder/maxCharsPerByte ()F
                         invokestatic          java/lang/String/scale (IF)I
                         istore                6
                         aload                 5
                         getstatic             java/nio/charset/CodingErrorAction/REPLACE Ljava/nio/charset/CodingErrorAction;
                         invokevirtual         java/nio/charset/CharsetDecoder/onMalformedInput (Ljava/nio/charset/CodingErrorAction;)Ljava/nio/charset/CharsetDecoder;
                         getstatic             java/nio/charset/CodingErrorAction/REPLACE Ljava/nio/charset/CodingErrorAction;
                         invokevirtual         java/nio/charset/CharsetDecoder/onUnmappableCharacter (Ljava/nio/charset/CodingErrorAction;)Ljava/nio/charset/CharsetDecoder;
                         pop
                         iload                 6
                         newarray              char
                         astore                7
                         aload                 4
                         invokevirtual         java/lang/Object/getClass ()Ljava/lang/Class;
                         invokevirtual         java/lang/Class/getClassLoader0 ()Ljava/lang/ClassLoader;
                         ifnull                806
                         invokestatic          java/lang/System/getSecurityManager ()Ljava/lang/SecurityManager;
                         ifnull                806
                         aload_1
                         iload_2
                         iload_2
                         iload_3
                         iadd
                         invokestatic          java/util/Arrays/copyOfRange ([BII)[B
                         astore_1
                         iconst_0
                         istore_2
                         aload                 5
                         aload                 7
                         aload_1
                         iload_2
                         iload_3
                         invokestatic          java/lang/String/decodeWithDecoder (Ljava/nio/charset/CharsetDecoder;[C[BII)I
                         istore                8
                         goto                  833
                         astore                9
                         new                   java/lang/Error
                         dup
                         aload                 9
                         invokespecial         java/lang/Error/<init> (Ljava/lang/Throwable;)V
                         athrow
                         getstatic             java/lang/String/COMPACT_STRINGS Z
                         ifeq                  866
                         aload                 7
                         iconst_0
                         iload                 8
                         invokestatic          java/lang/StringUTF16/compress ([CII)[B
                         astore                9
                         aload                 9
                         ifnull                866
                         aload_0
                         aload                 9
                         putfield              java/lang/String/value [B
                         aload_0
                         iconst_0
                         putfield              java/lang/String/coder B
                         return
                         aload_0
                         iconst_1
                         putfield              java/lang/String/coder B
                         aload_0
                         aload                 7
                         iconst_0
                         iload                 8
                         invokestatic          java/lang/StringUTF16/toBytes ([CII)[B
                         putfield              java/lang/String/value [B
                         return
            .end code
            """;
    public static void main(String... args) {
        var lexer = new JvmAssemblyLexer(CharStreams.fromString(TEST_INPUT));
        var parser = new JvmAssemblyParser(new CommonTokenStream(lexer));

        var assembler = new AssemblyParseListener();
        parser.addParseListener(assembler);
        parser.assemblyFile();
        System.out.println("Done!");
    }
}
