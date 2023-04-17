package edu.westminstercollege.cs.jade.classfile.instruction;

public sealed interface Operand {

    OperandType type();

    public record U8(int value) implements Operand {
        @Override
        public OperandType type() {
            return OperandType.U8;
        }
    }

    public record U16(int value) implements Operand {
        @Override
        public OperandType type() {
            return OperandType.U16;
        }
    }

    public record S8(int value) implements Operand {
        @Override
        public OperandType type() {
            return OperandType.S8;
        }
    }

    public record S16(int value) implements Operand {
        @Override
        public OperandType type() {
            return OperandType.S16;
        }
    }

    public record S32(int value) implements Operand {
        @Override
        public OperandType type() {
            return OperandType.S32;
        }
    }

    public sealed interface Imm8 extends Operand {
        @Override
        default OperandType type() {
            return OperandType.Imm8;
        }

        public record Integer(int value) implements Imm8 {}
        public record Float(float value) implements Imm8 {}
        public record String(java.lang.String value) implements Imm8 {}
    }

    public sealed interface Imm16 extends Operand {
        @Override
        default OperandType type() {
            return OperandType.Imm16;
        }

        public record Integer(int value) implements Imm16 {}
        public record Float(float value) implements Imm16 {}
        public record String(java.lang.String value) implements Imm16 {}
        public record Long(long value) implements Imm16 {}
        public record Double(double value) implements Imm16 {}
    }

    public record RefType(String text) implements Operand {
        @Override
        public OperandType type() {
            return OperandType.RefType;
        }
    }

    public record Field(String className, String fieldName, String descriptor) implements Operand {
        @Override
        public OperandType type() {
            return OperandType.Field;
        }
    }

    public record DynamicCallSite() implements Operand {
        @Override
        public OperandType type() {
            return OperandType.DynamicCallSite;
        }
    }

    public record Method(String className, String methodName, String descriptor) implements Operand {
        @Override
        public OperandType type() {
            return OperandType.Method;
        }
    }

    public record LUT() implements Operand {
        @Override
        public OperandType type() {
            return OperandType.LUT;
        }
    }

    public record JT() implements Operand {
        @Override
        public OperandType type() {
            return OperandType.JT;
        }
    }

    public record AType(String name) implements Operand {
        @Override
        public OperandType type() {
            return OperandType.AType;
        }
    }

    public record BranchOffset16(int offset) implements Operand {
        @Override
        public OperandType type() {
            return OperandType.BranchOffset16;
        }
    }

    public record BranchOffset32(int offset) implements Operand {
        @Override
        public OperandType type() {
            return OperandType.BranchOffset32;
        }
    }
}
