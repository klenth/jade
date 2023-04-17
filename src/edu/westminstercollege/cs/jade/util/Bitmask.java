package edu.westminstercollege.cs.jade.util;

import java.util.EnumSet;

public class Bitmask {

    public static interface Masked {
        int mask();
    }

    public static <E extends Enum<E> & Masked> EnumSet<E> fromMask(Class<E> enumClass, int mask) {
        EnumSet<E> set = EnumSet.noneOf(enumClass);

        for (var constant : enumClass.getEnumConstants()) {
            if ((mask & constant.mask()) != 0)
                set.add(constant);
        }

        return set;
    }
}
