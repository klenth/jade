package edu.westminstercollege.cs.jade.classfile;

import java.util.Arrays;

public record Field(
    int accessFlags,
    int nameIndex,
    int descriptorIndex,
    Attribute[] attributes) {

    @Override
    public String toString() {
        String attributesString = "[]";
        if (attributes.length > 0)
            attributesString = "[\n"
                    + String.join(",\n", Arrays.stream(attributes).map(f -> "            " + f).toList())
                    + "\n        ]";
        return String.format("Field[accessFlags=%d, nameIndex=%d, descriptorIndex=%d, attributes=%s]",
                accessFlags, nameIndex, descriptorIndex, attributesString);
    }
}
