package io.github.speedbridgemc.config.processor.api.type;

/**
 * Represents the kind of a {@link ConfigType}.
 */
public enum ConfigTypeKind {
    /**
     * The primitive boolean type, {@literal true} of {@literal false}.
     */
    BOOL,
    /**
     * The primitive byte type.
     */
    BYTE,
    /**
     * The primitive short type.
     */
    SHORT,
    /**
     * The primitive integer type.
     */
    INT,
    /**
     * The primitive long type.
     */
    LONG,
    /**
     * The primitive character type.
     */
    CHAR,
    /**
     * The primitive float type.
     */
    FLOAT,
    /**
     * The primitive double type.
     */
    DOUBLE,
    /**
     * The primitive string type.<p>
     * {@linkplain String This isn't a primitive in Java.}
     */
    STRING,
    /**
     * The array type.<p>
     * Maps to arrays, {@link java.util.Collection} and its subtypes.
     */
    ARRAY,
    /**
     * The map type.<p>
     * Maps to {@link java.util.Map} and its subtypes.
     */
    MAP,
    /**
     * The struct type, otherwise known as the object type.<p>
     * Maps to any arbitrary Java class.
     */
    STRUCT;

    /**
     * Checks if the kind is primitive or not.
     *
     * @return {@code true} if primitive, {@code false} otherwise.
     */
    public boolean isPrimitive() {
        switch (this) {
        case BOOL:
        case BYTE:
        case SHORT:
        case INT:
        case LONG:
        case CHAR:
        case FLOAT:
        case DOUBLE:
        case STRING:
            return true;
        case ARRAY:
        case MAP:
        case STRUCT:
            return false;
        }
        throw new AssertionError(this + " is not handled?!");
    }
}
