package edu.westminstercollege.cs.jade;

import static edu.westminstercollege.cs.jade.classfile.AccessFlag.*;

import edu.westminstercollege.cs.jade.classfile.*;
import edu.westminstercollege.cs.jade.classfile.attribute.StandardAttribute;
import edu.westminstercollege.cs.jade.classfile.attribute.StandardAttributes;
import edu.westminstercollege.cs.jade.classfile.instruction.Instruction;
import edu.westminstercollege.cs.jade.classfile.instruction.Opcode;
import edu.westminstercollege.cs.jade.classfile.instruction.Operand;
import edu.westminstercollege.cs.jade.util.Bitmask;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Disassembler {

    private Classfile cf;
    private ConstantPool cp;

    public Disassembler(Classfile cf) {
        this.cf = cf;
        this.cp = cf.constantPool();
    }

    public void disassemble(OutputStream out) throws InvalidClassException {
        disassemble(new PrintWriter(out));
    }

    public void disassemble(PrintWriter out) throws InvalidClassException {
        try {
            printSourceFile(out);
            printClass(out);
            printSuperclass(out);
            printSuperinterfaces(out);

            out.println();

            printFields(out);

            if (cf.fields().length > 0)
                out.println();

            printMethods(out);
        } finally {
            out.flush();
        }
    }

    private void printSourceFile(PrintWriter out) throws InvalidClassException {
        getAttributeValue(cf.attributes(), StandardAttributes.SourceFile)
                .ifPresent(sourceFile -> out.printf(".source %s\n", sourceFile));
    }

    private void printClass(PrintWriter out) {
        var flags = Bitmask.fromMask(AccessFlag.class, cf.accessFlags());

        boolean isInterface = flags.contains(ACC_INTERFACE),
                isAnnotation = flags.contains(ACC_ANNOTATION),
                isEnum = flags.contains(ACC_ENUM),
                isModule = flags.contains(ACC_MODULE),
                isNormalClass = !(isInterface || isEnum || isModule);

        String whatIs = isInterface ? "interface"
                : isAnnotation ? "annotation"
                : isEnum ? "enum"
                : isModule ? "module"
                : "class";

        List<String> modifiers = new ArrayList<>(2);
        if (flags.contains(ACC_PUBLIC))
            modifiers.add("public");
        if (!isInterface && flags.contains(ACC_ABSTRACT))
            modifiers.add("abstract");
        if (flags.contains(ACC_FINAL))
            modifiers.add("final");

        String modString = "";
        if (modifiers.size() > 0)
            modString = " " + String.join(" ", modifiers);

        String className = cp.string(cp.clazz(cf.thisClass()));

        out.printf(".%s%s %s\n", whatIs, modString, className);
    }

    private void printSuperclass(PrintWriter out) {
        out.printf(".super %s\n", cp.string(cp.clazz(cf.superClass())));
    }

    private void printSuperinterfaces(PrintWriter out) {
        for (var iface : cf.interfaces())
            out.printf(".implements %s\n", cp.string(cp.clazz(iface)));
    }

    private void printFields(PrintWriter out) {
        for (var field : cf.fields())
            printField(out, field);
    }

    private void printField(PrintWriter out, Field field) {
        var flags = Bitmask.fromMask(AccessFlag.class, field.accessFlags());
        List<String> modifiers = new ArrayList<>();

        if (flags.contains(ACC_PUBLIC))
            modifiers.add("public");
        if (flags.contains(ACC_PRIVATE))
            modifiers.add("private");
        if (flags.contains(ACC_PROTECTED))
            modifiers.add("protected");
        if (flags.contains(ACC_STATIC))
            modifiers.add("static");
        if (flags.contains(ACC_FINAL))
            modifiers.add("final");
        if (flags.contains(ACC_VOLATILE))
            modifiers.add("volatile");
        if (flags.contains(ACC_TRANSIENT))
            modifiers.add("transient");
        if (flags.contains(ACC_ENUM))
            modifiers.add("enum");

        String modString = "";
        if (modifiers.size() > 0)
            modString = " " + String.join(" ", modifiers);

        out.printf(".field%s %s %s\n", modString, cp.string(field.nameIndex()), cp.string(field.descriptorIndex()));
    }

    private void printMethods(PrintWriter out) throws InvalidClassException {
        for (var method : cf.methods()) {
            printMethod(out, method);
            out.println();
        }
    }

    private void printMethod(PrintWriter out, Method method) throws InvalidClassException {
        var flags = Bitmask.fromMask(AccessFlag.class, method.accessFlags());
        List<String> modifiers = new ArrayList<>();

        if (flags.contains(ACC_PUBLIC))
            modifiers.add("public");
        if (flags.contains(ACC_PRIVATE))
            modifiers.add("private");
        if (flags.contains(ACC_PROTECTED))
            modifiers.add("protected");
        if (flags.contains(ACC_STATIC))
            modifiers.add("static");
        if (flags.contains(ACC_FINAL))
            modifiers.add("final");
        if (flags.contains(ACC_SYNCHRONIZED))
            modifiers.add("synchronized");
        if (flags.contains(ACC_VARARGS))
            modifiers.add("varargs");
        if (flags.contains(ACC_NATIVE))
            modifiers.add("native");
        if (flags.contains(ACC_ABSTRACT))
            modifiers.add("abstract");
        if (flags.contains(ACC_STRICT))
            modifiers.add("strictfp");


        String modString = "";
        if (modifiers.size() > 0)
            modString = " " + String.join(" ", modifiers);

        out.printf(".method%s %s %s\n", modString, cp.string(method.nameIndex()), cp.string(method.descriptorIndex()));

        var maybeCode = getAttributeValue(method.attributes(), StandardAttributes.Code);
        if (maybeCode.isPresent()) {
            var code = maybeCode.get();
            out.println(".code");
            out.printf(".limit stack %d\n", code.maxLocals());
            out.printf(".limit stack %d\n", code.maxStack());

            printInstructions(out, code.code());

            out.println(".end code");
        }
    }

    private <T> Optional<T> getAttributeValue(Attribute[] attributes, StandardAttribute<T> attribute) throws InvalidClassException {
        for (var attr : attributes) {
            String attrName = cp.string(attr.nameIndex());
            if (attrName.equals(attribute.getName())) {
                return Optional.of(attribute.decode(ByteBuffer.wrap(attr.info()), cp));
            }
        }

        return Optional.empty();
    }

    private void printInstructions(PrintWriter out, byte[] code) throws InvalidClassException {
        var b = ByteBuffer.wrap(code);
        boolean wide = false;
        while (b.position() < b.limit()) {
            int pos = b.position();
            out.printf("%-8d    ", pos);
            var instr = Instruction.read(b, cp, wide);
            out.printf("    %-18s", instr.opcode().mnemonic());
            var operands = instr.operands();
            if (operands.size() > 0)
                out.printf("    %s", String.join(" ", operands.stream().map(this::operandToString).toList()));
            out.println();

            wide = (instr.opcode() == Opcode.WIDE);
        }
    }

    private String operandToString(Operand op) {
        return switch (op) {
            case Operand.U8(int i) -> "" + i;
            case Operand.U16(int i) -> "" + i;
            case Operand.S8(int i) -> "" + i;
            case Operand.S16(int i) -> "" + i;
            case Operand.S32(int i) -> "" + i;
            case Operand.Imm8.Integer(int i) -> "" + i;
            case Operand.Imm8.Float(float f) -> String.format("%ff", f);
            case Operand.Imm8.String(String s) -> String.format("\"%s\"", s);
            case Operand.Imm16.Integer(int i) -> "" + i;
            case Operand.Imm16.Float(float f) -> String.format("%ff", f);
            case Operand.Imm16.String(String s) -> String.format("\"%s\"", s);
            case Operand.Imm16.Long(long l) -> String.format("%dL", l);
            case Operand.Imm16.Double(double d) -> "" + d;
            case Operand.RefType(String name) -> name;
            case Operand.Field(String className, String fieldName, String descriptor) ->
                String.format("%s/%s %s", className, fieldName, descriptor);
            case Operand.DynamicCallSite() -> "~dynamic~";
            case Operand.Method(String className, String methodName, String descriptor) ->
                String.format("%s/%s %s", className, methodName, descriptor);
            case Operand.LUT() -> "[LUT]";
            case Operand.JT() -> "[JT]";
            case Operand.AType(String name) -> name;
            case Operand.BranchOffset16(int offset) -> "" + offset;
            case Operand.BranchOffset32(int offset) -> "" + offset;
        };
    }

    public static void main(String... args) throws IOException {
        //final String classFilename = "String.class";
        final String classFilename = "Test.class";

        try (var channel = Files.newByteChannel(Path.of(classFilename), StandardOpenOption.READ)){
            var bytes = ((FileChannel)channel).map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

            var d = new ClassfileReader();
            var classfile = d.read(bytes);

            System.out.println(classfile);
            System.out.println("——");
            new Disassembler(classfile).disassemble(System.out);
        } catch (InvalidClassException | UnsupportedClassFeatureException e) {
            System.err.println(e.getMessage());
        }
    }
}
