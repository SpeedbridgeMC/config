package io.github.speedbridgemc.config.serialize;

import org.jetbrains.annotations.NotNull;

public interface KeyedEnum<T> {
    @NotNull T getKey();
    @NotNull T @NotNull [] getAliases();
}
