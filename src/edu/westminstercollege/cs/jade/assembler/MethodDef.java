package edu.westminstercollege.cs.jade.assembler;

import edu.westminstercollege.cs.jade.classfile.AccessFlag;
import edu.westminstercollege.cs.jade.classfile.instruction.Instruction;

import java.util.EnumSet;
import java.util.List;

public record MethodDef(String name, EnumSet<AccessFlag> flags, String descriptor, int locals, int stack, List<InstrDef> body) {
}
