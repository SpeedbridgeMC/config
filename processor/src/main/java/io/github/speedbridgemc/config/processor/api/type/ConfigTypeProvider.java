package io.github.speedbridgemc.config.processor.api.type;

import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

/**
 * Provides {@link ConfigType} instances.
 */
public interface ConfigTypeProvider {
    /**
     * Initializes the provider.
     * @param processingEnv annotation processing environment
     */
    void init(@NotNull ProcessingEnvironment processingEnv);

    /**
     * Gets a {@code ConfigType} that represents a primitive.
     * @param kind primitive kind
     * @return type of specified kind
     */
    @NotNull ConfigType primitiveOf(@NotNull ConfigTypeKind kind);

    /**
     * Gets a {@code ConfigType} that represents an array, with the specified type as its component type.
     * @param componentType component type
     * @return array type
     */
    @NotNull ConfigType arrayOf(@NotNull ConfigType componentType);

    /**
     * Gets a {@code ConfigType} that represents a map, with the specified types as its key and value types.
     * @param keyType key type
     * @param valueType value type
     * @return map type
     */
    @NotNull ConfigType mapOf(@NotNull ConfigType keyType, @NotNull ConfigType valueType);

    /**
     * Gets a {@code ConfigType} from a {@link TypeMirror}.
     * @param mirror type mirror
     * @return matching type
     */
    @NotNull ConfigType fromMirror(@NotNull TypeMirror mirror);
}
