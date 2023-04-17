package edu.westminstercollege.cs.jade.classfile;

public record Code(int maxStack, int maxLocals, byte[] code, /*ExceptionTableEntry[] exceptionTable,*/ Attribute[] attributes) {
}
