package io.github.speedbridgemc.config.serialize;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;

/**
 * Helper methods for {@link KeyedEnum}s.
 */
final class KeyedEnumHelpers {
    private KeyedEnumHelpers() { }

    private static final HashMap<KeyedEnum<?>, Class<?>> KEY_TYPE_CACHE = new HashMap<>();

    /**
     * Returns a keyed enum's key type.
     * @param keyedEnum the keyed enum
     * @param <T> type of key
     * @return key type's class
     */
    @SuppressWarnings("unchecked")
    public static <T> @NotNull Class<T> getKeyType(@NotNull KeyedEnum<T> keyedEnum) {
        // this is utterly horrible. but it works, so...
        return (Class<T>) KEY_TYPE_CACHE.computeIfAbsent(keyedEnum, keyedEnum1 -> {
            for (Type interfaceType : keyedEnum1.getClass().getGenericInterfaces()) {
                if (interfaceType instanceof ParameterizedType) {
                    ParameterizedType paramType = (ParameterizedType) interfaceType;
                    Type rawType = paramType.getRawType();
                    if (KeyedEnum.class != rawType)
                        continue;
                    Type[] typeArgs = paramType.getActualTypeArguments();
                    if (typeArgs.length == 0)
                        throw new RuntimeException("Raw keyed enum isn't supported: " + keyedEnum.getClass().toGenericString());
                    return (Class<?>) typeArgs[0];
                }
            }
            throw new RuntimeException("Couldn't find key type for keyed enum: " + keyedEnum.getClass().toGenericString());
        });
    }

    private static final HashMap<KeyedEnum<?>, Object> EMPTY_ALIAS_ARRAY_CACHE = new HashMap<>();

    /**
     * Returns an empty array of keys.
     * @param keyedEnum the keyed enum
     * @param <T> type of key
     * @return an empty array of keys
     */
    @SuppressWarnings("unchecked")
    public static <T> @NotNull T @NotNull [] getEmptyAliasArray(@NotNull KeyedEnum<T> keyedEnum) {
        return (T[]) EMPTY_ALIAS_ARRAY_CACHE.computeIfAbsent(keyedEnum,
                keyedEnum1 -> Array.newInstance(getKeyType(keyedEnum), 0));
    }
}
