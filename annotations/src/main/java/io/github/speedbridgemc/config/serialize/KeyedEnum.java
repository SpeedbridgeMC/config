package io.github.speedbridgemc.config.serialize;

import org.jetbrains.annotations.NotNull;

public interface KeyedEnum<T> {
    Object[] EMPTY_ARRAY = new Object[0];

    @NotNull T getKey();
    default @NotNull T @NotNull [] getAliases() {
        //noinspection unchecked
        return (T[]) EMPTY_ARRAY;
    }
}
