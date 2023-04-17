package edu.westminstercollege.cs.jade.assembler;

import edu.westminstercollege.cs.jade.SyntaxException;
import edu.westminstercollege.cs.jade.classfile.AccessFlag;
import edu.westminstercollege.cs.jade.classfile.Classfile;
import edu.westminstercollege.cs.jade.classfile.instruction.Opcode;
import edu.westminstercollege.cs.jade.classfile.instruction.OperandType;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class Assembler_by_hand {

    private class Slot<T> {
        T value;
    }

    private BufferedReader in;
    private Deque<String> lineBuffer = new LinkedList<>();
    private int lineNumber = 0;
    private List<Error> errors = new ArrayList<>(), warnings = new ArrayList<>();
    private Set<String> processedDirectives = new HashSet<>();

    private String sourceFile;
    private EnumSet<AccessFlag> classFlags = EnumSet.noneOf(AccessFlag.class);
    private String superclass;
    private List<String> interfaces = new ArrayList<>();
    private List<FieldDef> fields = new ArrayList<>();
    private List<MethodDef> methods = new ArrayList<>();

    public Assembler_by_hand(BufferedReader in) {
        this.in = in;
    }

    public Classfile assemble() throws IOException, SyntaxException {
        Slot<String> tail = new Slot<>();
        for (String line = nextLine(); line != null; line = nextLine()) {
            if (matchHead(line, ".source", tail))
                processSourceDirective(tail.value);
            else if (matchHead(line, ".class", tail))
                processClassDirective("class", tail.value);
            else if (matchHead(line, ".super", tail))
                processSuperDirective(tail.value);
            else if (matchHead(line, ".field", tail))
                processFieldDirective(tail.value);
            else if (matchHead(line, ".method", tail))
                processMethod(tail.value);
        }

        throw new RuntimeException("Unimplemented");
    }

    private void processSourceDirective(String tail) {
        if (processedDirectives.contains("source"))
            warning("Repeated .source directive");
        processedDirectives.add("source");
        sourceFile = tail.trim();
        if (sourceFile.isEmpty())
            warning("Blank .source directive");
    }

    private void processClassDirective(String type, String tail) {
        if (processedDirectives.contains("class"))
            warning("Multiple .class/.interface/.enum/.annotation/.module directives");
        processedDirectives.add("class");

        if (tail.isEmpty()) {
            error("Missing name of class");
            return;
        }
        var words = tail.trim().split("\\s+");

        for (int i = 0; i + 1 < words.length; ++i) {
            var flag = accessFlag(words[i]);
            if (flag == null || !isClassFlag(flag))
                error("Bad class flag: %s", words[i]);
            if (classFlags.contains(flag))
                warning("Repeated class flag: %s", words[i]);
            classFlags.add(flag);
        }

        String className = words[words.length - 1];
    }

    private void processSuperDirective(String tail) {
        if (processedDirectives.contains("super"))
            warning("Multiple .super directives");
        processedDirectives.add("super");

        if (tail.isEmpty()) {
            error("Missing name of superclass");
            return;
        }

        var words = tail.split("\\s+");
        if (words.length > 1)
            error("Trailing words after .super");
        else
            superclass = words[0];
    }

    private void processImplementsDirective(String tail) {
        if (tail.isEmpty()) {
            error("Missing name of interface");
            return;
        }

        var words = tail.split("\\s+");
        if (words.length > 1)
            error("Trailing words after .interface");
        else
            interfaces.add(words[0]);
    }

    private void processFieldDirective(String tail) {
        var words = tail.split("\\s+");
        if (words.length < 2) {
            error("Missing name or descriptor of field");
            return;
        }

        EnumSet<AccessFlag> fieldFlags = EnumSet.noneOf(AccessFlag.class);
        for (int i = 0; i + 2 < words.length; ++i) {
            var maybeFlag = accessFlag(words[i]);
            if (maybeFlag != null && isFieldFlag(maybeFlag)) {
                if (fieldFlags.contains(maybeFlag))
                    warning("Repeated field flag: %s", words[i]);
                fieldFlags.add(maybeFlag);
            } else
                error("Bad field flag: %s", words[i]);
        }

        fields.add(new FieldDef(words[words.length - 2], fieldFlags, words[words.length - 1]));
    }

    private void processMethod(String tail) throws IOException, SyntaxException {
        var words = tail.split("\\s+");
        if (words.length < 2)
            error("Missing name and descriptor of method");

        var methodFlags = EnumSet.noneOf(AccessFlag.class);

        for (int i = 0; i + 2 < words.length; ++i) {
            var maybeFlag = accessFlag(words[i]);
            if (maybeFlag == null || !isMethodFlag(maybeFlag))
                error("Bad method flag: %s", words[i]);
            else {
                if (methodFlags.contains(maybeFlag))
                    warning("Repeated method flag: %s", words[i]);
                methodFlags.add(maybeFlag);
            }
        }

        String methodName = words[words.length - 2];
        String methodDescriptor = words[words.length - 1];
        Integer locals = null, stack = null;

        String line = nextLine();
        List<InstrDef> instructions = List.of();
        if (!line.equals(".code")) {
            pushLine(line);
            locals = stack = 0;
        } else {
            Slot<String> limitTail = new Slot<>();
            while (true) {
                line = nextLine();
                if (line == null)
                    immediatelyFatalError("Unexpected end of file");
                else if (matchHead(line, ".limit", limitTail)) {
                    Pattern limitPattern = Pattern.compile("^(locals|stack)\\s+(\\d+)$");
                    var matcher = limitPattern.matcher(limitTail.value);
                    if (!matcher.matches())
                        error("Invalid .limit directive");
                    else {
                        int limit = Integer.parseInt(matcher.group(2));
                        if (matcher.group(1).equals("stack")) {
                            if (locals != null)
                                warning("Multiple .limit stack directives");
                            locals = limit;
                        } else {
                            if (stack != null)
                                warning("Multiple .limit stack directives");
                            stack = limit;
                        }
                    }
                } else if (line.equals(".end method"))
                    break;
                else {
                    pushLine(line);
                    parseInstruction(instructions);
                }
            }
        }

        if (locals == null)
            error("Missing .limit stack directive");
        if (stack == null)
            error("Missing .limit stack directive");
        methods.add(new MethodDef(methodName, methodFlags, methodDescriptor, locals == null ? 0 : locals, stack == null ? 0 : stack, instructions));
    }

    private void parseInstruction(List<InstrDef> instructions) throws IOException, SyntaxException {
        String line = nextLine();
        String[] words = line.split("\\s+");
        // TODO: support for labels, .line (.line probably in processMethod())

        var maybeOpcode = Opcode.of(words[0]);
        if (maybeOpcode.isEmpty()) {
            error("Unknown instruction mnemonic: %s", words[0]);
            return;
        }

        var opcode = maybeOpcode.get();
        var operandTypes = opcode.operandTypes();
        if (words.length != 1 + operandTypes.size()) {
            error("%s: expected %d operands, got %d", words[0], operandTypes.size(), words.length - 1);
            return;
        }

        List<Object> operands = new ArrayList<>(operandTypes.size());
        for (int i = 0; i < operandTypes.size(); ++i)
            operands.add(parseOperand(words[i + 1], operandTypes.get(i)));

        instructions.add(new InstrDef(opcode, operands));
    }

    private Object parseOperand(String text, OperandType type) {
        /*return switch (type) {
            case U8, U16, S8, S16, S32 -> {

            }
        };*/
        throw new RuntimeException("Unimplemented");
    }

    private String nextLine() throws IOException {
        while (!lineBuffer.isEmpty()) {
            String line = lineBuffer.pop();
            if (!line.isEmpty())
                return line;
        }

        while (true) {
            ++lineNumber;
            String line = in.readLine();
            if (line == null)
                return null;

            line = line.trim();
            if (line.isEmpty())
                continue;

            return line;
        }
    }

    private void pushLine(String line) {
        lineBuffer.push(line);
    }

    private boolean matchHead(String text, String head, Slot<String> tail) {
        if (text.startsWith(head)) {
            tail.value = text.substring(head.length()).trim();
            return true;
        }

        return false;
    }

    private AccessFlag accessFlag(String text) {
        try {
            return AccessFlag.valueOf("ACC_" + text.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void error(String messagePattern, Object... args) {
        errors.add(new Error(lineNumber, String.format(messagePattern, args), Error.Severity.Error));
    }

    private void warning(String messagePattern, Object... args) {
        warnings.add(new Error(lineNumber, String.format(messagePattern, args), Error.Severity.Warning));
    }

    private void immediatelyFatalError(String messagePattern, Object... args) throws SyntaxException {
        error(messagePattern, args);
        throw new SyntaxException();
    }

    private boolean isClassFlag(AccessFlag flag) {
        return switch (flag) {
            case ACC_PUBLIC, ACC_FINAL, ACC_ABSTRACT, ACC_SYNTHETIC -> true;
            default -> false;
        };
    }

    private boolean isFieldFlag(AccessFlag flag) {
        return switch (flag) {
            case ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED,
                    ACC_STATIC, ACC_FINAL, ACC_VOLATILE, ACC_TRANSIENT, ACC_SYNTHETIC, ACC_ENUM -> true;
            default -> false;
        };
    }

    private boolean isMethodFlag(AccessFlag flag) {
        return switch (flag) {
            case ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED,
                    ACC_STATIC, ACC_FINAL, ACC_SYNCHRONIZED, ACC_BRIDGE, ACC_VARARGS,
                    ACC_NATIVE, ACC_ABSTRACT, ACC_STRICT, ACC_SYNTHETIC -> true;
            default -> false;
        };
    }
}
