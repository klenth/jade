package edu.westminstercollege.cs.jade.classfile.attribute;

import edu.westminstercollege.cs.jade.classfile.ConstantPool;

import java.nio.ByteBuffer;

public class SourceFileAttribute implements StandardAttribute<String> {

    @Override
    public String getName() {
        return "SourceFile";
    }

    @Override
    public String decode(ByteBuffer info, ConstantPool constantPool) {
        return constantPool.string(info.getShort());
    }
}
