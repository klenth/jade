package edu.westminstercollege.cs.jade.classfile;

import java.util.Arrays;
import java.util.stream.IntStream;

public record Classfile(
    int minorVersion,
    int majorVersion,
    ConstantPool constantPool,
    int accessFlags,
    int thisClass,
    int superClass,
    int[] interfaces,
    Field[] fields,
    Method[] methods,
    Attribute[] attributes) {

    @Override
    public String toString() {
        String cpString = "[]";
        record Z<T>(int index, T value) {}

        if (constantPool.size() > 1) {
            int poolDigits = (int)Math.ceil(Math.log10(constantPool.size()));

            cpString = "[\n"
                + String.join(",\n",
                    IntStream.range(1, constantPool.size())
                            .mapToObj(i -> new Z<>(i, constantPool.get(i)))
                            .map(z -> String.format("        [%" + poolDigits + "d] %s", z.index(), z.value()))
                                    .toList())
                    + "\n    ]";
        }

        String interfacesString = "[" + String.join(", ", Arrays.stream(interfaces).mapToObj(String::valueOf).toList()) + "]";

        String fieldsString = "[]";
        if (fields.length > 0)
            fieldsString = "[\n"
                + String.join(",\n", Arrays.stream(fields).map(f -> "        " + f).toList())
                    + "\n    ]";

        String methodsString = "[]";
        if (methods.length > 0)
            methodsString = "[\n"
                    + String.join(",\n", Arrays.stream(methods).map(f -> "        " + f).toList())
                    + "\n    ]";

        String attributesString = "[]";
        if (attributes.length > 0)
            attributesString = "[\n"
                    + String.join(",\n", Arrays.stream(attributes).map(f -> "        " + f).toList())
                    + "\n    ]";

        return String.format("""
                Classfile[
                    minorVersion=%d,
                    majorVersion=%d,
                    constantPool=%s,
                    accessFlags=%d,
                    thisClass=%d,
                    superClass=%d,
                    interfaces=%s,
                    fields=%s,
                    methods=%s,
                    attributes=%s
                ]""",
                minorVersion, majorVersion,
                cpString,
                accessFlags,
                thisClass, superClass,
                interfacesString,
                fieldsString, methodsString,
                attributesString
        );
    }
}
