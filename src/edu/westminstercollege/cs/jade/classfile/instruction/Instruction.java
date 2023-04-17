package edu.westminstercollege.cs.jade.classfile.instruction;

import edu.westminstercollege.cs.jade.InvalidClassException;
import edu.westminstercollege.cs.jade.classfile.Constant;
import edu.westminstercollege.cs.jade.classfile.ConstantPool;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public record Instruction(Opcode opcode, List<Operand> operands) {

    public static Instruction read(ByteBuffer b, ConstantPool cp, boolean wide) throws InvalidClassException {
        int num = b.get() & 0xff;
        var maybeOpcode = Opcode.of(num);
        if (maybeOpcode.isEmpty())
            throw new InvalidClassException(String.format("Unknown opcode: %d", num));

        Opcode opcode = maybeOpcode.get();

        if (wide && !opcode.isWidenable())
            throw new InvalidClassException(String.format("wide followed by opcode %d, which is not widenable", num));
        if (opcode == Opcode.WIDE)
            throw new RuntimeException("Cannot handle wide instructions");

        var operandTypes = wide ? opcode.wideOperandTypes() : opcode.operandTypes();
        List<Operand> operands = new ArrayList<>(operandTypes.size());

        for (var opType : operandTypes) {
            var operand = switch (opType) {
                case U8 -> new Operand.U8(b.get() & 0xff);
                case U16 -> new Operand.U16(b.getShort() & 0xffff);
                case S8 -> new Operand.S8(b.get());
                case S16 -> new Operand.S16(b.getShort());
                case S32 -> new Operand.S32(b.getInt());

                case Imm8 -> switch (cp.get(b.get() & 0xff)) {
                    case Constant.Integer(int n) -> new Operand.Imm8.Integer(n);
                    case Constant.Float(float f) -> new Operand.Imm8.Float(f);
                    case Constant.String(int index) -> new Operand.Imm8.String(cp.string(index));
                    default -> throw new InvalidClassException("Invalid operand for Imm8");
                };

                case Imm16 -> {
                    var constant = cp.get(b.getShort() & 0xffff);
                    yield switch (constant) {
                        case Constant.Integer(int n) -> new Operand.Imm16.Integer(n);
                        case Constant.Float(float f) -> new Operand.Imm16.Float(f);
                        case Constant.String(int index) -> new Operand.Imm16.String(cp.string(index));
                        case Constant.Long(long l) -> new Operand.Imm16.Long(l);
                        case Constant.Double(double d) -> new Operand.Imm16.Double(d);
                        default -> throw new InvalidClassException("Invalid operand for Imm16: " + constant);
                    };
                }

                case RefType -> switch (cp.get(b.getShort() & 0xffff)) {
                    case Constant.Class(int index) -> new Operand.RefType(cp.string(index));
                    default -> throw new InvalidClassException("Invalid operand for RefType");
                };

                case Field -> {
                    var fInfo = (Constant.FieldRef)cp.get(b.getShort() & 0xffff);
                    var className = cp.string(cp.clazz(fInfo.classIndex()));
                    var fieldName = cp.string(cp.nameAndType(fInfo.nameAndTypeIndex()).nameIndex());
                    var fieldDesc = cp.string(cp.nameAndType(fInfo.nameAndTypeIndex()).descriptorIndex());
                    yield new Operand.Field(className, fieldName, fieldDesc);
                }

                case DynamicCallSite -> {
                    b.getInt();
                    yield new Operand.DynamicCallSite();
                }

                case Method -> {
                    var info = cp.get(b.getShort() & 0xffff);
                    if (info instanceof Constant.MethodRef mInfo) {
                        var className = cp.string(cp.clazz(mInfo.classIndex()));
                        var methodName = cp.string(cp.nameAndType(mInfo.nameAndTypeIndex()).nameIndex());
                        var methodDesc = cp.string(cp.nameAndType(mInfo.nameAndTypeIndex()).descriptorIndex());
                        yield new Operand.Method(className, methodName, methodDesc);
                    } else if (info instanceof Constant.InterfaceMethodRef imInfo) {
                        var className = cp.string(cp.clazz(imInfo.classIndex()));
                        var methodName = cp.string(cp.nameAndType(imInfo.nameAndTypeIndex()).nameIndex());
                        var methodDesc = cp.string(cp.nameAndType(imInfo.nameAndTypeIndex()).descriptorIndex());
                        yield new Operand.Method(className, methodName, methodDesc);
                    } else
                        throw new InvalidClassException("Invalid operand for Method");
                }

                case LUT -> {
                    int p = b.position();
                    int pad = (4 - p % 4) % 4;
                    p += pad + 4;
                    b.position(p);
                    int numPairs = b.getInt();
                    System.out.flush();
                    b.position(p + 8 * numPairs + 4);
                    yield new Operand.LUT();
                }

                case JT -> {
                    int p = b.position();
                    int pad = (4 - p % 4) % 4;
                    p += pad + 4;
                    b.position(p);
                    int low = b.getInt(), high = b.getInt();
                    b.position(p + 8 + (high - low + 1) * 4);
                    yield new Operand.JT();
                }

                case AType -> new Operand.AType(switch (b.get() & 0xff) {
                    case 4 -> "boolean";
                    case 5 -> "char";
                    case 6 -> "float";
                    case 7 -> "double";
                    case 8 -> "byte";
                    case 9 -> "short";
                    case 10 -> "int";
                    case 11 -> "long";
                    default -> throw new InvalidClassException("Invalid type for newarray");
                });

                case BranchOffset16 -> new Operand.BranchOffset16((b.position() - 1) + b.getShort());

                case BranchOffset32 -> new Operand.BranchOffset32((b.position() - 1) + b.getInt());
            };

            operands.add(operand);
        }

        return new Instruction(opcode, operands);
    }

    public static Iterable<Instruction> readAll(ByteBuffer b, ConstantPool cp) {
        return () -> new Iterator<>() {
            private boolean wide = false;

            @Override
            public boolean hasNext() {
                return b.hasRemaining();
            }

            @Override
            public Instruction next() {
                if (!hasNext())
                    throw new IllegalStateException("All instructions exhausted");
                try {
                    var instr = read(b, cp, wide);
                    this.wide = (instr.opcode == Opcode.WIDE);
                    return instr;
                } catch (InvalidClassException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    public static Stream<Instruction> instructionStream(ByteBuffer buffer, ConstantPool cp) {
        final ByteBuffer b = buffer.duplicate();
        final Iterator<Instruction> it = readAll(b, cp).iterator();

        var spliterator = new Spliterator<Instruction>() {
            @Override
            public int characteristics() {
                return IMMUTABLE
                        | NONNULL
                        | ORDERED;
            }

            @Override
            public long estimateSize() {
                return (b.limit() - b.position()) / 2;
            }

            @Override
            public boolean tryAdvance(Consumer<? super Instruction> action) {
                if (!it.hasNext())
                    return false;

                var next = it.next();
                action.accept(next);
                return true;
            }

            @Override
            public Spliterator<Instruction> trySplit() {
                return null;
            }
        };

        return StreamSupport.stream(spliterator, false);
    }
}
