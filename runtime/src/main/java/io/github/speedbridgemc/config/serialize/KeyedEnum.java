package io.github.speedbridgemc.config.serialize;

import org.jetbrains.annotations.NotNull;

public interface KeyedEnum<T> {
    @NotNull T getKey();
    default @NotNull T @NotNull [] getAliases() {
        return KeyedEnumHelpers.getEmptyAliasArray(this);
    }
}
