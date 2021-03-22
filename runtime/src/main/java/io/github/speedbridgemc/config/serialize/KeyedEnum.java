package io.github.speedbridgemc.config.serialize;

import org.jetbrains.annotations.NotNull;

/**
 * This interface allows you to customize how enums are serialized,
 * by specifying <em>unique keys</em> for each and every enum value.
 * @param <T> type of keys
 */
public interface KeyedEnum<T> {
    /**
     * Gets the key that matches this enum value. <em>These should be unique for each and every enum value!</em>
     * @return this value's key
     */
    @NotNull T getKey();

    /**
     * Gets this enum value's aliases. <em>An enum shouldn't have another enum's key as an alias, and vice versa!</em><p>
     * By default, returns an empty array.
     * @return this value's aliases, or an empty array if it has no aliases
     */
    default @NotNull T @NotNull [] getAliases() {
        return KeyedEnumHelpers.getEmptyAliasArray(this);
    }
}
