package edu.westminstercollege.cs.jade.classfile;

public sealed interface Constant {

    record Utf8(java.lang.String value) implements Constant { }
    record Integer(int value) implements Constant { }
    record Float(float value) implements Constant { }
    record Long(long value) implements Constant { }
    record Double(double value) implements Constant { }
    record Class(int nameIndex) implements Constant { }
    record String(int stringIndex) implements Constant { }
    record FieldRef(int classIndex, int nameAndTypeIndex) implements Constant { }
    record MethodRef(int classIndex, int nameAndTypeIndex) implements Constant { }
    record InterfaceMethodRef(int classIndex, int nameAndTypeIndex) implements Constant { }
    record NameAndType(int nameIndex, int descriptorIndex) implements Constant { }
    record MethodHandle(int referenceKind, int referenceIndex) implements Constant { }
    record MethodType(int descriptorIndex) implements Constant { }
    record Dynamic(int bootstrapMethodAttrIndex, int nameAndTypeIndex) implements Constant { }
    record InvokeDynamic(int bootstrapMethodAttrIndex, int nameAndTypeIndex) implements Constant { }
    record Module(int nameIndex) implements Constant { }
    record Package(int nameIndex) implements Constant { }

}
