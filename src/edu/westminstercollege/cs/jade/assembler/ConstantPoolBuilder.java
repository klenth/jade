package edu.westminstercollege.cs.jade.assembler;

import edu.westminstercollege.cs.jade.classfile.Constant;
import edu.westminstercollege.cs.jade.classfile.ConstantPool;

import java.util.HashMap;
import java.util.Map;

class ConstantPoolBuilder {

    private Map<Constant, Integer> constants = new HashMap<>();

    {
        constants.put(null, 0);
    }

    public int constant(Constant c) {
        Integer index = constants.getOrDefault(c, null);
        if (index == null) {
            index = constants.size();
            constants.put(c, index);
            if (c instanceof Constant.Long || c instanceof Constant.Double)
                constants.put(null, index + 1);
        }

        return index;
    }

    public ConstantPool build() {
        Constant[] constantArray = new Constant[constants.size()];
        for (var entry : constants.entrySet())
            constantArray[entry.getValue()] = entry.getKey();

        return new ConstantPool(constantArray);
    }
}
