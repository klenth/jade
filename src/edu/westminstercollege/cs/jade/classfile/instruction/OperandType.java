package edu.westminstercollege.cs.jade.classfile.instruction;

public enum OperandType {

    U8, U16,        // unsigned 8-, 16-bit integer
    S8, S16, S32,   // signed 8-, 16-, 32-bit integer
    Imm8, Imm16,    // index of immediate value in CP, 8- or 16-bit
    //Pad4,           // padding to make next instruction addr multiple of 4
    RefType,        // index of reference type (class, interface, array) in CP
    Field,          // index of field in CP
    DynamicCallSite,// invokedynamic BS
    Method,         // index of method in CP
    LUT,            // lookup table for lookupswitch instruction
    JT,             // jump table for tableswitch instruction
    AType,          // array type (for newarray instruction)
    BranchOffset16,
    BranchOffset32;

    /**
     * Returns the number of bytes that this operand uses in a class file, or -1 if it is variable size.
     */
    public int bytes() {
        return switch (this) {
            case U8, S8, Imm8, AType -> 1;
            case U16, S16, Imm16, BranchOffset16,
                    RefType, Field, DynamicCallSite, Method -> 2;
            case S32, BranchOffset32 -> 4;
            case LUT, JT -> -1; // variable size
        };
    }
}
