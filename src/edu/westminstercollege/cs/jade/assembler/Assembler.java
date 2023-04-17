package edu.westminstercollege.cs.jade.assembler;

import static edu.westminstercollege.cs.jade.assembler.Node.*;
import static edu.westminstercollege.cs.jade.classfile.instruction.Operand.*;

import edu.westminstercollege.cs.jade.JvmAssemblyLexer;
import edu.westminstercollege.cs.jade.JvmAssemblyParser;
import edu.westminstercollege.cs.jade.SyntaxException;
import edu.westminstercollege.cs.jade.classfile.AccessFlag;
import edu.westminstercollege.cs.jade.classfile.instruction.Opcode;
import edu.westminstercollege.cs.jade.classfile.instruction.OperandType;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.*;

public class Assembler {

    AssemblyParseListener listener = new AssemblyParseListener();
    String sourceFile = null;
    String superclass = null;
    List<String> interfaces = new ArrayList<>();
    List<AccessFlag> classFlags;
    String className;
    ClassDirective.Type classType;
    List<FieldDirective> fields = new ArrayList<>();
    List<MethodDirective> methods = new ArrayList<>();

    private Integer methodLocals, methodStack;
    private Set<String> methodLabels;

    private boolean wide = false;

    private List<Error> errors = new ArrayList<>();
    private Deque<Node> nodeStack = new LinkedList<>();

    public void assemble(Reader in, ByteBuffer out) throws IOException, SyntaxException {
        var lexer = new JvmAssemblyLexer(CharStreams.fromReader(in));
        var parser = new JvmAssemblyParser(new CommonTokenStream(lexer));
        parser.addParseListener(listener);
        parser.assemblyFile();

        try {
            verify(listener.file);
        } catch (SyntaxException ex) {}

        if (className == null)
            errors.add(new Error(1, "No .class/.interface/.enum/.annotation/.module directive", Error.Severity.Error));

        errors.sort(Comparator.comparingInt(Error::line));

        boolean hasError = false;
        for (var error : errors) {
            if (error.severity() == Error.Severity.Error)
                hasError = true;

            System.err.printf("%s on line %d: %s\n",
                    error.severity() == Error.Severity.Error ? "Error" : "Warning",
                    error.line(),
                    error.message());
        }

        if (hasError)
            throw new SyntaxException("Assembly aborted due to errors");

        var writer = new ClassfileWriter(this);
        writer.write(out);
    }

    private void verify(Node node) throws SyntaxException {
        nodeStack.push(node);
        try {
            switch (node) {
                case AsmFile af -> {
                    for (var directive : af.directives())
                        verify(directive);
                }

                case SourceDirective sd -> {
                    if (sourceFile != null)
                        error("Duplicate .source directive");
                    sourceFile = sd.sourceFile();
                }

                case ClassDirective cd -> {
                    if (classType != null) {
                        if (cd.type() == classType)
                            error("Duplicate .%s directive", classType.toString().toLowerCase());
                        else
                            error(".%s directive after %s", cd.type().toString().toLowerCase(), classType.toString().toLowerCase());
                    }

                    classType = cd.type();
                    className = cd.name();
                    classFlags = cd.flags();
                }

                case SuperDirective sd -> {
                    if (superclass != null)
                        error("Duplicate .super directive");
                    superclass = sd.superclassName();
                }

                case ImplementsDirective id -> {
                    if (interfaces.contains(id.interfaceName()))
                        warning("Redundant .implements: %s");
                    else
                        interfaces.add(id.interfaceName());
                }

                case FieldDirective fd -> {
                    var repeatedField = fields.stream()
                            .filter(fdef -> fdef.name().equals(fd.name()) && fdef.descriptor().equals(fd.descriptor()))
                            .findAny();
                    if (repeatedField.isPresent())
                        error("Duplicate .field directive: %s %s", fd.name(), fd.descriptor());
                    else
                        fields.add(fd);
                }

                case MethodDirective md -> {
                    var repeatedMethod = fields.stream()
                            .filter(mdef -> mdef.name().equals(md.name())
                                && paramTypes(mdef.descriptor()).equals(paramTypes(md.descriptor())))
                            .findAny();
                    if (repeatedMethod.isPresent())
                        error("Duplicate .method directive: %s %s", md.name(), md.descriptor());
                    else {
                        methods.add(md);
                        if (md.code().isPresent())
                            verify(md.code().get());
                    }
                }

                case Code c -> {
                    methodLocals = methodStack = null;
                    methodLabels = new HashSet<>();
                    wide = false;
                    for (var line : c.lines())
                        verify(line);
                }

                case LimitLocals ll -> {
                    if (methodLocals != null)
                        error("Duplicate .limit stack");
                    methodLocals = ll.locals();
                }

                case LimitStack ls -> {
                    if (methodStack != null)
                        error("Duplicate .limit stack");
                    methodStack = ls.stack();
                }

                case Instruction i -> {
                    if (i.label().isPresent() && methodLabels.contains(i.label().get()))
                        error("Duplicate label: %s", i.label().get());
                    else if (i.label().isPresent())
                        methodLabels.add(i.label().get());

                    verifyInstruction(i);
                    wide = i.opcode().equals(Opcode.WIDE.mnemonic());
                }

                default -> throw new RuntimeException("Internal assembler error: unimplemented node: " + node);
            }
        } finally {
            nodeStack.pop();
        }
    }

    private void verifyInstruction(Instruction instr) throws SyntaxException {
        var maybeOpcode = Opcode.of(instr.opcode());
        if (maybeOpcode.isEmpty()) {
            error("Invalid instruction mnemonic: %s", instr.opcode());
            return;
        }

        var opcode = maybeOpcode.get();

        if (wide && !opcode.isWidenable()) {
            error("Instruction %s cannot be widened", instr.opcode());
            return;
        }

        var operandTypes = wide ? opcode.wideOperandTypes() : opcode.operandTypes();
        int expectedTextOperands = 0;

        for (var opType : operandTypes) {
            expectedTextOperands += switch (opType) {
                case Field, Method ->  2;
                default -> 1;
            };
        }

        if (expectedTextOperands != instr.operands().size()) {
            error("Wrong number of operands to instruction %s", instr.opcode());
            return;
        }

        var instrOperandsIt = instr.operands().iterator();
        var operandTypeIt = operandTypes.iterator();
        while (operandTypeIt.hasNext())
            verifyOperand(instrOperandsIt, operandTypeIt.next());
    }

    private void verifyOperand(Iterator<Operand> operand, OperandType type) throws SyntaxException {
        try {
            if (classfileOperand(operand, type) != null)
                return;
        } catch (ClassCastException | NoSuchElementException ex) {
            error("Invalid operand");
        }
    }

    edu.westminstercollege.cs.jade.classfile.instruction.Operand classfileOperand(Iterator<Operand> operand, OperandType type) {
        return switch (type) {
            case U8 -> new U8(intValue(operand.next(), 0x00, 0xff));
            case U16 -> new U16(intValue(operand.next(), 0x0000, 0xffff));
            case S8 -> new S8(intValue(operand.next(), -0x80, 0x7f));
            case S16 -> new S16(intValue(operand.next(), -0x8000, 0x7fff));
            case S32 -> new S32(intValue(operand.next(), Integer.MIN_VALUE, Integer.MAX_VALUE));

            case Imm8 -> switch (operand.next()) {
                case Operand.Int i -> new Imm8.Integer(intValue(i));
                case Operand.Float f -> new Imm8.Float(Float.parseFloat(f.text()));
                case Operand.Str s -> new Imm8.String(s.text().substring(1, s.text().length() - 1));
                default -> null;
            };

            case Imm16 -> switch (operand.next()) {
                case Operand.Int i -> new Imm16.Integer(intValue(i));
                case Operand.Float f -> new Imm16.Float(Float.parseFloat(f.text()));
                case Operand.Str s -> new Imm16.String(s.text().substring(1, s.text().length() - 1));
                case Operand.Long l -> new Imm16.Long(longValue(l));
                case Operand.Double d -> new Imm16.Double(Double.parseDouble(d.text()));
                default -> null;
            };

            case RefType -> new RefType(((Operand.Word)operand.next()).text());

            case Field -> {
                var fieldName = ((Operand.Word)operand.next()).text();
                var fieldDesc = ((Operand.Word)operand.next()).text();
                int classFieldNameSep = fieldName.lastIndexOf('/');
                String className = "";
                if (classFieldNameSep < 0)
                    error("Missing class name in field: %s", fieldName);
                else {
                    className = fieldName.substring(0, classFieldNameSep);
                    fieldName = fieldName.substring(classFieldNameSep + 1);
                }
                yield new Field(className, fieldName, fieldDesc);
            }

            case Method -> {
                var methodName = ((Operand.Word)operand.next()).text();
                var methodDesc = ((Operand.Word)operand.next()).text();
                int classMethodNameSep = methodName.lastIndexOf('/');
                String className = "";
                if (classMethodNameSep < 0)
                    error("Missing class name in method: %s", methodName);
                else {
                    className = methodName.substring(0, classMethodNameSep);
                    methodName = methodName.substring(classMethodNameSep + 1);
                }
                yield new Method(className, methodName, methodDesc);
            }

            case AType -> {
                var text = ((Operand.Word)operand.next()).text();
                yield new AType(switch (text) {
                    case "boolean", "char", "float", "double", "byte", "short", "int", "long" -> text;
                    default -> {
                        error("Invalid array type: %s", text);
                        yield text;
                    }
                });
            }

            case BranchOffset16 -> new BranchOffset16(intValue(operand.next(), -0x8000, 0x7fff));

            case BranchOffset32 -> new BranchOffset32(intValue(operand.next()));

            default -> throw new RuntimeException("Unimplemented operand type: " + type);
        };
    }

    private int intValue(Operand op, int min, int max) {
        var opInt = (Operand.Int)op;
        var text = opInt.text().toLowerCase();
        int value;
        if (text.startsWith("0x"))
            value = Integer.parseInt(text.substring(2), 0x10);
        else
            value = Integer.parseInt(text);

        if (value < min || value > max)
            error("Value %d out of range: must be between %d and %d", opInt.text(), min, max);
        return value;
    }

    private int intValue(Operand op) {
        return intValue(op, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    private long longValue(Operand op) {
        var opLong = (Operand.Long)op;
        var text = opLong.text().toLowerCase();
        if (text.startsWith("0x"))
            return Long.parseLong(text.substring(2), 0x10);
        return Long.parseLong(text);
    }

    private void error(String messagePattern, Object... args) {
        var lineNumber = nodeStack.peek().ctx().start.getLine();
        errors.add(new Error(lineNumber, String.format(messagePattern, args), Error.Severity.Error));
    }

    private void warning(String message, Object... args) {
        var lineNumber = nodeStack.peek().ctx().start.getLine();
        errors.add(new Error(lineNumber, String.format(message, args), Error.Severity.Warning));
    }

    private void immediatelyFatalError(String message, Object... args) throws SyntaxException {
        error(message, args);
        throw new SyntaxException();
    }

    private String paramTypes(String methodDescriptor) {
        int rparenIndex = methodDescriptor.lastIndexOf(')');
        return methodDescriptor.substring(1, rparenIndex);
    }
}
