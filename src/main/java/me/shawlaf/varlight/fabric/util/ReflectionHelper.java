package me.shawlaf.varlight.fabric.util;

import java.lang.reflect.Field;

public class ReflectionHelper {

    private ReflectionHelper() {
        throw new IllegalStateException();
    }

    public static Field getDeclaredField(Class clazz, String named, String intermediary) {
        Field field;

        try {
            field = clazz.getDeclaredField(intermediary); // Try to load intermediary first (Normal Environment)
        } catch (NoSuchFieldException ignored) {
            try {
                field = clazz.getDeclaredField(named); // Dev Environment
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(String.format("Failed to find either named or intermediary Field \"%s\" (aka \"%s\") in Class %s", intermediary, named, clazz.getName()), e);
            }
        }

        return field;
    }

}
