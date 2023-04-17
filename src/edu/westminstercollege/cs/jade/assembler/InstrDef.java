package edu.westminstercollege.cs.jade.assembler;

import edu.westminstercollege.cs.jade.classfile.instruction.Opcode;

import java.util.List;

public record InstrDef(Opcode opcode, List<Object> arguments) {
}
