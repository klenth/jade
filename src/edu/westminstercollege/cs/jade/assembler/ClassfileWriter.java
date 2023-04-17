package edu.westminstercollege.cs.jade.assembler;

import static edu.westminstercollege.cs.jade.assembler.Node.*;

import edu.westminstercollege.cs.jade.classfile.AccessFlag;
import edu.westminstercollege.cs.jade.classfile.Constant;
import edu.westminstercollege.cs.jade.classfile.ConstantPool;
import edu.westminstercollege.cs.jade.classfile.instruction.Opcode;
import edu.westminstercollege.cs.jade.classfile.instruction.Operand;
import edu.westminstercollege.cs.jade.classfile.instruction.OperandType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

class ClassfileWriter {

    private Assembler a;
    private ConstantPool cp;


    private int thisClassIndex = -1;
    private int superClassIndex = 0;
    private int sourceFileIndex = -1;
    private List<Integer> interfaceIndices = new ArrayList<>();

    ClassfileWriter(Assembler assembler) throws IOException {
        this.a = assembler;
    }

    void write(ByteBuffer out) throws IOException {
        if (cp == null)
            cp = assembleConstantPool();

        out.order(ByteOrder.BIG_ENDIAN);
        out.putInt(0xCAFE_BABE); // magic

        out.putShort((short)0);     // minor version
        out.putShort((short)56);    // major version

        writeConstantPool(out, cp);

        writeAccessFlags(out, a.classFlags);

        out.putShort((short)thisClassIndex);
        out.putShort((short)superClassIndex);

        out.putShort((short)interfaceIndices.size());
        interfaceIndices.forEach(i -> out.putShort((short)(int)i));

        writeFields(out, a.fields);
        writeMethods(out, a.methods);

        out.putShort((short)0); // attribute count

    }

    private ConstantPool assembleConstantPool() {
        var builder = new ConstantPoolBuilder();
        assembleConstantPool(a.listener.file, builder);
        return builder.build();
    }

    private void assembleConstantPool(Node node, ConstantPoolBuilder builder) {
        switch (node) {
            case AsmFile af -> {
                for (var directive : af.directives())
                    assembleConstantPool(directive, builder);
            }

            case SourceDirective sd ->
                sourceFileIndex = builder.constant(new Constant.Utf8(sd.sourceFile()));
                // TODO: SourceFile attribute

            case ClassDirective cd -> {
                var classNameIndex = builder.constant(new Constant.Utf8(cd.name()));
                thisClassIndex = builder.constant(new Constant.Class(classNameIndex));
            }

            case SuperDirective sd -> {
                var superclassNameIndex = builder.constant(new Constant.Utf8(sd.superclassName()));
                superClassIndex = builder.constant(new Constant.Class(superclassNameIndex));
            }

            case ImplementsDirective id -> {
                var interfaceNameIndex = builder.constant(new Constant.Utf8(id.interfaceName()));
                interfaceIndices.add(builder.constant(new Constant.Class(interfaceNameIndex)));
            }

            case FieldDirective fd -> {
                builder.constant(new Constant.Utf8(fd.name()));
                builder.constant(new Constant.Utf8(fd.descriptor()));
            }

            case MethodDirective md -> {
                builder.constant(new Constant.Utf8(md.name()));
                builder.constant(new Constant.Utf8(md.descriptor()));

                if (md.code().isPresent()) {
                    builder.constant(new Constant.Utf8("Code"));
                    assembleConstantPool(md.code().get(), builder);
                }
            }

            case Code c -> {
                boolean wide = false;
                for (var line : c.lines()) {
                    if (line instanceof Instruction i) {
                        var opcode = Opcode.of(i.opcode()).get();
                        var instrOperandsIt = i.operands().iterator();
                        var operandTypes = wide ? opcode.wideOperandTypes() : opcode.operandTypes();
                        var operandTypeIt = operandTypes.iterator();

                        while (operandTypeIt.hasNext()) {
                            var operandType = operandTypeIt.next();
                            var classfileOperand = a.classfileOperand(instrOperandsIt, operandType);

                            assembleConstantPool(operandType, classfileOperand, builder);
                        }

                        wide = (opcode == Opcode.WIDE);
                    }
                }
            }

            default -> {}
        }
    }

    private void assembleConstantPool(OperandType operandType, Operand operand, ConstantPoolBuilder builder) {
        switch (operand) {
            case Operand.Imm8.Integer(int i) -> builder.constant(new Constant.Integer(i));
            case Operand.Imm8.Float(float f) -> builder.constant(new Constant.Float(f));
            case Operand.Imm8.String(String s) -> builder.constant(new Constant.String(builder.constant(new Constant.Utf8(s))));

            case Operand.Imm16.Integer(int i) -> builder.constant(new Constant.Integer(i));
            case Operand.Imm16.Float(float f) -> builder.constant(new Constant.Float(f));
            case Operand.Imm16.String(String s) -> builder.constant(new Constant.String(builder.constant(new Constant.Utf8(s))));
            case Operand.Imm16.Long(long l) -> builder.constant(new Constant.Long(l));
            case Operand.Imm16.Double(double d) -> builder.constant(new Constant.Double(d));

            case Operand.RefType(String s) -> builder.constant(new Constant.Class(builder.constant(new Constant.Utf8(s))));

            case Operand.Field(String className, String fieldName, String descriptor) -> {
                var classIndex = builder.constant(new Constant.Class(builder.constant(new Constant.Utf8(className))));
                var nameAndTypeIndex = builder.constant(new Constant.NameAndType(
                        builder.constant(new Constant.Utf8(fieldName)),
                        builder.constant(new Constant.Utf8(descriptor))
                ));
                builder.constant(new Constant.FieldRef(classIndex, nameAndTypeIndex));
            }

            case Operand.Method(String className, String methodName, String descriptor) -> {
                var classIndex = builder.constant(new Constant.Class(builder.constant(new Constant.Utf8(className))));
                var nameAndTypeIndex = builder.constant(new Constant.NameAndType(
                        builder.constant(new Constant.Utf8(methodName)),
                        builder.constant(new Constant.Utf8(descriptor))
                ));
                builder.constant(new Constant.MethodRef(classIndex, nameAndTypeIndex));
            }

            default -> {}
        }
    }

    private void writeConstantPool(ByteBuffer out, ConstantPool pool) {
        out.putShort((short)cp.constants().length); // constant pool size

        for (var constant : pool.constants()) {
            if (constant == null)
                // should only be the case for the index 0 and "back halves" of double/long values
                continue;

            switch (constant) {
                case Constant.Utf8(String s) -> {
                    out.put((byte)1);
                    byte[] stringBytes = s.getBytes(StandardCharsets.UTF_8);
                    out.putShort((short)stringBytes.length);
                    out.put(stringBytes);
                }

                case Constant.Integer(int i) -> {
                    out.put((byte)3);
                    out.putInt(i);
                }

                case Constant.Float(float f) -> {
                    out.put((byte)4);
                    out.putFloat(f);
                }

                case Constant.Long(long l) -> {
                    out.put((byte)5);
                    out.putLong(l);
                }

                case Constant.Double(double d) -> {
                    out.put((byte)6);
                    out.putDouble(d);
                }

                case Constant.Class(int n) -> {
                    out.put((byte)7);
                    out.putShort((short)n);
                }

                case Constant.String(int n) -> {
                    out.put((byte)8);
                    out.putShort((short)n);
                }

                case Constant.FieldRef(int classIndex, int nameAndTypeIndex) -> {
                    out.put((byte)9);
                    out.putShort((short)classIndex);
                    out.putShort((short)nameAndTypeIndex);
                }

                case Constant.MethodRef(int classIndex, int nameAndTypeIndex) -> {
                    out.put((byte)10);
                    out.putShort((short)classIndex);
                    out.putShort((short)nameAndTypeIndex);
                }

                case Constant.InterfaceMethodRef(int classIndex, int nameAndTypeIndex) -> {
                    out.put((byte)11);
                    out.putShort((short)classIndex);
                    out.putShort((short)nameAndTypeIndex);
                }

                case Constant.NameAndType(int nameIndex, int descriptorIndex) -> {
                    out.put((byte)12);
                    out.putShort((short)nameIndex);
                    out.putShort((short)descriptorIndex);
                }

                case Constant.MethodHandle(int referenceKind, int referenceIndex) -> {
                    out.put((byte)15);
                    out.put((byte)referenceKind);
                    out.putShort((short)referenceIndex);
                }

                case Constant.MethodType(int descriptorIndex) -> {
                    out.put((byte)16);
                    out.putShort((short)descriptorIndex);
                }

                case Constant.Dynamic(int bootstrapMethodAttribute, int nameAndTypeIndex) -> {
                    out.put((byte)17);
                    out.putShort((short)bootstrapMethodAttribute);
                    out.putShort((short)nameAndTypeIndex);
                }

                case Constant.InvokeDynamic(int bootstrapMethodAttrIndex, int nameAndTypeIndex) -> {
                    out.put((byte)18);
                    out.putShort((short)bootstrapMethodAttrIndex);
                    out.putShort((short)nameAndTypeIndex);
                }

                case Constant.Module(int nameIndex) -> {
                    out.put((byte)19);
                    out.putShort((short)nameIndex);
                }

                case Constant.Package(int nameIndex) -> {
                    out.put((byte)20);
                    out.putShort((short)nameIndex);
                }
            }
        }
    }

    private short findConstant(Constant c) {
        for (int i = 0; i < cp.constants().length; ++i)
            if (c.equals(cp.constants()[i]))
                return (short)i;
        throw new RuntimeException("Internal assembler error: constant not found in pool: " + c);
    }

    private void writeAccessFlags(ByteBuffer out, Collection<AccessFlag> c) {
        int flags = 0;
        for (var flag : c)
            flags |= flag.mask();
        out.putShort((short)flags);
    }

    private void writeFields(ByteBuffer out, Collection<FieldDirective> fields) {
        out.putShort((short)fields.size());
        for (var field : fields) {
            writeAccessFlags(out, field.flags());
            out.putShort(findConstant(new Constant.Utf8(field.name())));
            out.putShort(findConstant(new Constant.Utf8(field.descriptor())));
            // TODO: attributes
            out.putShort((short)0); // attribute count
        }
    }

    private void writeMethods(ByteBuffer out, Collection<MethodDirective> methods) {
        out.putShort((short)methods.size());
        for (var method : methods) {
            writeAccessFlags(out, method.flags());
            out.putShort(findConstant(new Constant.Utf8(method.name())));
            out.putShort(findConstant(new Constant.Utf8(method.descriptor())));
            // TODO: other attributes
            if (method.code().isPresent()) {
                out.putShort((short)1); // number of attributes
                writeCode(out, method.code().get());
            } else
                out.putShort((short)0);
        }
    }

    private void writeCode(ByteBuffer out, Code code) {
        int codeSize = calculateCodeSize(code);
        int exceptionTableLength = 0;
        int attributeSize = 2 + 2 + 4 + codeSize + 2 + exceptionTableLength * (2 + 2 + 2 + 2) + 2;

        int maxLocals = -1, maxStack = -1;
        for (var line : code.lines()) {
            if (line instanceof LimitLocals ll) {
                maxLocals = ll.locals();
                if (maxStack >= 0)
                    break;
            } else if (line instanceof LimitStack ls) {
                maxStack = ls.stack();
                if (maxLocals >= 0)
                    break;
            }
        }

        out.putShort(findConstant(new Constant.Utf8("Code")));
        out.putInt(attributeSize);
        out.putShort((short)maxStack);
        out.putShort((short)maxLocals);
        out.putInt(codeSize);

        boolean wide = false;
        for (var line : code.lines()) {
            if (!(line instanceof Instruction instr))
                continue;

            writeInstruction(out, instr, wide);

            wide = (Opcode.of(instr.opcode()).get() == Opcode.WIDE);
        }

        out.putShort((short)exceptionTableLength);

        // Exception table goes here

        out.putShort((short)0); // attribute count

        // Attributes go here
    }

    private int calculateCodeSize(Code code) {
        int exceptionTableLength = 0;
        int size = 0;

        boolean wide = false;
        for (var line : code.lines()) {
            if (!(line instanceof Instruction instr))
                continue;
            size += instructionLength(instr, wide);
            wide = (Opcode.of(instr.opcode()).get() == Opcode.WIDE);
        }

        return size;
    }

    private int instructionLength(Instruction i, boolean wide) {
        var opcode = Opcode.of(i.opcode()).get();
        var operands = wide ? opcode.wideOperandTypes() : opcode.operandTypes();
        var stats = operands.stream().mapToInt(OperandType::bytes).summaryStatistics();
        if (operands.size() > 0 && stats.getMax() < 0)
            throw new RuntimeException("Variable-length operands unimplemented");
        return 1 + (int)stats.getSum();
    }

    private void writeInstruction(ByteBuffer out, Instruction instr, boolean wide) {
        var opcode = Opcode.of(instr.opcode()).get();
        out.put((byte)opcode.value());

        var instrOperandsIt = instr.operands().iterator();
        var operandTypes = wide ? opcode.wideOperandTypes() : opcode.operandTypes();
        var operandTypeIt = operandTypes.iterator();

        while (operandTypeIt.hasNext()) {
            var operandType = operandTypeIt.next();
            var classfileOperand = a.classfileOperand(instrOperandsIt, operandType);

            writeOperand(out, classfileOperand);
        }
    }

    private void writeOperand(ByteBuffer out, Operand operand) {
        Function<Short, Byte> byteOrThrow = (Short n) -> {
            if (n < 0 || n > 255) throw new RuntimeException("Index too large for Imm8");
            return (byte)(int)n;
        };

        switch (operand) {
            case Operand.U8(int n) -> out.put((byte)n);
            case Operand.U16(int n) -> out.putShort((short)n);
            case Operand.S8(int n) -> out.put((byte)n);
            case Operand.S16(int n) -> out.putShort((short)n);
            case Operand.S32(int n) -> out.putInt(n);

            // Problem here — "ldc" instruction should probably switch to "ldc_w" transparently if constant is not in
            // first 255 entries!
            case Operand.Imm8.Integer(int n) -> out.put(byteOrThrow.apply(findConstant(new Constant.Integer(n))));
            case Operand.Imm8.Float(float f) -> out.put(byteOrThrow.apply(findConstant(new Constant.Float(f))));
            case Operand.Imm8.String(String s) -> out.put(byteOrThrow.apply(findConstant(new Constant.String(findConstant(new Constant.Utf8(s))))));

            case Operand.Imm16.Integer(int n) -> out.putShort(findConstant(new Constant.Integer(n)));
            case Operand.Imm16.Float(float f) -> out.putShort(findConstant(new Constant.Float(f)));
            case Operand.Imm16.String(String s) -> out.putShort(findConstant(new Constant.String(findConstant(new Constant.Utf8(s)))));
            case Operand.Imm16.Long(long l) -> out.putShort(findConstant(new Constant.Long(l)));
            case Operand.Imm16.Double(double d) -> out.putShort(findConstant(new Constant.Double(d)));

            case Operand.RefType(String text) -> out.putShort(findConstant(new Constant.Class(findConstant(new Constant.Utf8(text)))));

            case Operand.Field(String className, String fieldName, String descriptor) ->
                out.putShort(findConstant(new Constant.FieldRef(
                        findConstant(new Constant.Class(findConstant(new Constant.Utf8(className)))),
                        findConstant(new Constant.NameAndType(
                                findConstant(new Constant.Utf8(fieldName)),
                                findConstant(new Constant.Utf8(descriptor))
                        ))
                )));
            case Operand.Method(String className, String methodName, String descriptor) ->
                    out.putShort(findConstant(new Constant.MethodRef(
                            findConstant(new Constant.Class(findConstant(new Constant.Utf8(className)))),
                            findConstant(new Constant.NameAndType(
                                    findConstant(new Constant.Utf8(methodName)),
                                    findConstant(new Constant.Utf8(descriptor))
                            ))
                    )));

            case Operand.DynamicCallSite dcs -> throw new RuntimeException("Dynamic call sites unsupported");

            case Operand.LUT lut -> throw new RuntimeException("LUT operands unsupported");

            case Operand.JT jt -> throw new RuntimeException("JT operands unsupported");

            case Operand.AType(String name) -> out.put((byte)switch(name) {
                case "boolean" -> 4;
                case "char" -> 5;
                case "float" -> 6;
                case "double" -> 7;
                case "byte" -> 8;
                case "short" -> 9;
                case "int" -> 10;
                case "long" -> 11;
                default -> throw new RuntimeException("Internal assembler error: invalid array type: " + name);
            });

            case Operand.BranchOffset16(int offset) -> /* TODO: fix this */ out.putShort((short)offset);
            case Operand.BranchOffset32(int offset) -> /* TODO: and this */ out.putInt(offset);
        }
    }
}
