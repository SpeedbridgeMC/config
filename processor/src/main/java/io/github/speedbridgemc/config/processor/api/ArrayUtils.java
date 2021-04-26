package io.github.speedbridgemc.config.processor.api;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.HashMap;

public final class ArrayUtils {
    private ArrayUtils() { }

    private static final HashMap<Class<?>, Object[]> EMPTY_ARRAY_CACHE = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> T @NotNull [] emptyArrayOf(@NotNull Class<T> clazz) {
        return (T[]) EMPTY_ARRAY_CACHE.computeIfAbsent(clazz, aClass -> (Object[]) Array.newInstance(clazz, 0));
    }
}
