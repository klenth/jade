package edu.westminstercollege.cs.jade.classfile.attribute;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class StandardAttributes {

    private static final Map<String, StandardAttribute> attributes = new HashMap<>();

    public static final SourceFileAttribute SourceFile = register(new SourceFileAttribute());
    public static final CodeAttribute Code = register(new CodeAttribute());

    private StandardAttributes() {}

    private static <A extends StandardAttribute> A register(A attribute) {
        attributes.put(attribute.getName(), attribute);
        return attribute;
    }

    public static Optional<StandardAttribute<?>> find(String name) {
        return Optional.ofNullable(attributes.get(name));
    }
}
