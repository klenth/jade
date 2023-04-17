package edu.westminstercollege.cs.jade.classfile.attribute;

import edu.westminstercollege.cs.jade.InvalidClassException;
import edu.westminstercollege.cs.jade.classfile.Constant;
import edu.westminstercollege.cs.jade.classfile.ConstantPool;

import java.nio.ByteBuffer;

public interface StandardAttribute<T> {

    String getName();
    T decode(ByteBuffer info, ConstantPool constantPool) throws InvalidClassException;
}
