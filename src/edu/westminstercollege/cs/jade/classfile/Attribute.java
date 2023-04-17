package edu.westminstercollege.cs.jade.classfile;

public record Attribute(
    int nameIndex,
    byte[] info) {

    @Override
    public String toString() {
        return String.format("Attribute[nameIndex=%d, info=byte[%d]]", nameIndex, info.length);
    }
}
