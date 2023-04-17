package edu.westminstercollege.cs.jade.assembler;

import edu.westminstercollege.cs.jade.classfile.AccessFlag;

import java.util.EnumSet;

record FieldDef(String name, EnumSet<AccessFlag> flags, String descriptor) {
}
