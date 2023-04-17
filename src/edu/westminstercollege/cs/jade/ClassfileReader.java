package edu.westminstercollege.cs.jade;

import edu.westminstercollege.cs.jade.classfile.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ClassfileReader {

    public Classfile read(ByteBuffer bytes) throws InvalidClassException, UnsupportedClassFeatureException {
        bytes.order(ByteOrder.BIG_ENDIAN);

        if (bytes.getInt() != 0xCAFEBABE) {
            throw new InvalidClassException("Invalid magic number");
        }

        int minorVersion = bytes.getShort();
        int majorVersion = bytes.getShort();

        var constantPool = readConstantPool(bytes);

        int accessFlags = bytes.getShort();
        int thisClass = bytes.getShort();
        int superClass = bytes.getShort();

        int[] interfaces = readInterfaces(bytes);
        Field[] fields = readFields(bytes);
        Method[] methods = readMethods(bytes);
        Attribute[] attributes = readAttributes(bytes);

        if (bytes.position() != bytes.limit())
            throw new InvalidClassException("Classfile has trailing bytes");

        return new Classfile(
                minorVersion, majorVersion,
                new ConstantPool(constantPool),
                accessFlags,
                thisClass, superClass,
                interfaces,
                fields, methods,
                attributes
        );
    }

    public Constant[] readConstantPool(ByteBuffer bytes) throws InvalidClassException, UnsupportedClassFeatureException {
        int count = bytes.getShort();
        if (count < 1)
            throw new InvalidClassException(String.format("Invalid constant pool count: %d", count));

        var pool = new Constant[count];

        for (int i = 1; i < count; ++i) {
            pool[i] = readConstant(bytes);
            if (pool[i] instanceof Constant.Long || pool[i] instanceof Constant.Double)
                ++i;
        }

        return pool;
    }

    public Constant readConstant(ByteBuffer bytes) throws InvalidClassException, UnsupportedClassFeatureException {
        int tag = bytes.get();
        return switch (tag) {
            case 1 -> {
                int length = bytes.getShort();
                byte[] utf8 = new byte[length];
                bytes.get(utf8);
                yield new Constant.Utf8(new String(utf8, StandardCharsets.UTF_8));
            }

            case 3 -> new Constant.Integer(bytes.getInt());
            case 4 -> new Constant.Float(bytes.getFloat());
            case 5 -> new Constant.Long(bytes.getLong());
            case 6 -> new Constant.Double(bytes.getDouble());

            case 7 -> new Constant.Class(bytes.getShort());
            case 8 -> new Constant.String(bytes.getShort());
            case 9 -> new Constant.FieldRef(bytes.getShort(), bytes.getShort());
            case 10 -> new Constant.MethodRef(bytes.getShort(), bytes.getShort());
            case 11 -> new Constant.InterfaceMethodRef(bytes.getShort(), bytes.getShort());
            case 12 -> new Constant.NameAndType(bytes.getShort(), bytes.getShort());

            case 15 -> new Constant.MethodHandle(bytes.get(), bytes.getShort());
            case 16 -> new Constant.MethodType(bytes.getShort());
            case 17 -> new Constant.Dynamic(bytes.getShort(), bytes.getShort());
            case 18 -> new Constant.InvokeDynamic(bytes.getShort(), bytes.getShort());
            case 19 -> new Constant.Module(bytes.getShort());
            case 20 -> new Constant.Package(bytes.getShort());

            default -> throw new UnsupportedClassFeatureException(String.format("Unknown constant pool tag %d at address %d", tag, bytes.position() - 1));
        };
    }

    public int[] readInterfaces(ByteBuffer bytes) throws InvalidClassException, UnsupportedClassFeatureException {
        int count = bytes.getShort();
        if (count < 0)
            throw new InvalidClassException(String.format("Invalid interface count: %d", count));

        int[] interfaces = new int[count];
        for (int i = 0; i < count; ++i)
            interfaces[i] = bytes.getShort();

        return interfaces;
    }

    public Field[] readFields(ByteBuffer bytes) throws InvalidClassException, UnsupportedClassFeatureException {
        int count = bytes.getShort();
        if (count < 0)
            throw new InvalidClassException(String.format("Invalid field count: %d", count));

        Field[] fields = new Field[count];
        for (int i = 0; i < count; ++i)
            fields[i] = readField(bytes);

        return fields;
    }

    public Field readField(ByteBuffer bytes) throws InvalidClassException, UnsupportedClassFeatureException {
        int accessFlags = bytes.getShort();
        int nameIndex = bytes.getShort();
        int descriptorIndex = bytes.getShort();
        Attribute[] attributes = readAttributes(bytes);

        return new Field(accessFlags, nameIndex, descriptorIndex, attributes);
    }

    public Method[] readMethods(ByteBuffer bytes) throws InvalidClassException, UnsupportedClassFeatureException {
        int count = bytes.getShort();
        if (count < 0)
            throw new InvalidClassException(String.format("Invalid method count: %d", count));

        Method[] methods = new Method[count];
        for (int i = 0; i < count; ++i)
            methods[i] = readMethod(bytes);

        return methods;
    }

    public Method readMethod(ByteBuffer bytes) throws InvalidClassException, UnsupportedClassFeatureException {
        int accessFlags = bytes.getShort();
        int nameIndex = bytes.getShort();
        int descriptorIndex = bytes.getShort();
        Attribute[] attributes = readAttributes(bytes);

        return new Method(accessFlags, nameIndex, descriptorIndex, attributes);
    }

    public Attribute[] readAttributes(ByteBuffer bytes) throws InvalidClassException {
        int count = bytes.getShort();
        if (count < 0)
            throw new InvalidClassException(String.format("Invalid attribute count: %d", count));

        Attribute[] attributes = new Attribute[count];
        for (int i = 0; i < count; ++i)
            attributes[i] = readAttribute(bytes);

        return attributes;
    }

    public Attribute readAttribute(ByteBuffer bytes) throws InvalidClassException {
        int nameIndex = bytes.getShort();
        int length = bytes.getInt();
        byte[] info = new byte[length];
        bytes.get(info);

        return new Attribute(nameIndex, info);
    }

    public static void main(String... args) throws IOException {
        //final String classFilename = "out/production/jade/MediumClass.class";
        final String classFilename = "out/production/jade/edu/westminstercollege/cs/jade/ClassfileReader.class";
        try (var channel = Files.newByteChannel(Path.of(classFilename), StandardOpenOption.READ)){
            var bytes = ((FileChannel)channel).map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

            var d = new ClassfileReader();
            var classfile = d.read(bytes);

            System.out.println(classfile);
        } catch (InvalidClassException | UnsupportedClassFeatureException e) {
            System.err.println(e.getMessage());
        }
    }
}
