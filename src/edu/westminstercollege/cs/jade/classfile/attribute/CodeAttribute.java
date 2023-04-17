package edu.westminstercollege.cs.jade.classfile.attribute;

import edu.westminstercollege.cs.jade.ClassfileReader;
import edu.westminstercollege.cs.jade.InvalidClassException;
import edu.westminstercollege.cs.jade.classfile.Attribute;
import edu.westminstercollege.cs.jade.classfile.Code;
import edu.westminstercollege.cs.jade.classfile.ConstantPool;

import java.nio.ByteBuffer;

public class CodeAttribute implements StandardAttribute<Code> {

    @Override
    public String getName() {
        return "Code";
    }

    @Override
    public Code decode(ByteBuffer info, ConstantPool constantPool) throws InvalidClassException {
        int maxStack = info.getShort();
        int maxLocals = info.getShort();
        int codeLength = info.getInt();
        byte[] code = new byte[codeLength];
        info.get(code);
        int exceptionTableLength = info.getShort();
        info.position(info.position() + exceptionTableLength * 8);

        Attribute[] attributes = new ClassfileReader().readAttributes(info);

        return new Code(maxStack, maxLocals, code, attributes);
    }
}
