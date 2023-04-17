package edu.westminstercollege.cs.jade.classfile;

public record ConstantPool(Constant[] constants) {

    public int size() {
        return constants.length;
    }

    public Constant get(int index) {
        return constants[index];
    }

    public String string(int index) {
        return ((Constant.Utf8)constants[index]).value();
    }

    public int clazz(int index) {
        return ((Constant.Class)constants[index]).nameIndex();
    }

    public Constant.NameAndType nameAndType(int index) {
        return (Constant.NameAndType)constants[index];
    }
}
