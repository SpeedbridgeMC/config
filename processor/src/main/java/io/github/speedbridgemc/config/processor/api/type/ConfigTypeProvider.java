package io.github.speedbridgemc.config.processor.api.type;

import com.google.common.collect.ImmutableClassToInstanceMap;
import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.naming.NamingStrategy;
import io.github.speedbridgemc.config.processor.api.property.ConfigPropertyExtension;
import io.github.speedbridgemc.config.processor.api.property.ConfigPropertyExtensionFinder;
import io.github.speedbridgemc.config.processor.api.util.MirrorElementPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
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
     * @param nullable if the type should be nullable
     * @return type of specified kind
     */
    @NotNull ConfigType primitiveOf(@NotNull ConfigTypeKind kind, boolean nullable);

    /**
     * Gets a {@code ConfigType} that represents a primitive.<p>
     * Equivalent to {@code primitiveOf(kind, false)}.
     * @param kind primitive kind
     * @return type of specified kind
     */
    default @NotNull ConfigType primitiveOf(@NotNull ConfigTypeKind kind) {
        return primitiveOf(kind, false);
    }

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
     * Adds a preconfigured {@code ConfigType} of kind {@link ConfigTypeKind#STRUCT STRUCT}.
     * @param type matching type
     */
    void addStruct(@NotNull ConfigType type);

    /**
     * Adds a {@link ConfigPropertyExtensionFinder} to use when creating struct types.
     * @param extensionFinder finder to add
     */
    void addExtensionFinder(@NotNull ConfigPropertyExtensionFinder extensionFinder);

    /**
     * Finds {@link ConfigPropertyExtension}s using the specified mirror-element pairs.
     * @param callback callback
     * @param pairs mirror-element pairs
     */
    void findExtensions(@NotNull ConfigPropertyExtensionFinder.Callback callback,
                        @NotNull MirrorElementPair @NotNull ... pairs);

    /**
     * Sets the naming strategy to use when creating struct types.
     * @param strategy naming strategy to use
     * @param variant strategy variant to use
     */
    void setNamingStrategy(@NotNull NamingStrategy strategy, @NotNull String variant);

    /**
     * Converts a naming using the {@linkplain #setNamingStrategy(NamingStrategy, String) current naming strategy}.
     * @param originalName original name
     * @return converted name
     */
    @NotNull String name(@NotNull String originalName);

    /**
     * Sets a {@link io.github.speedbridgemc.config.Config.StructOverride StructOverride} for creating a {@code ConfigType}
     * from a specific {@link DeclaredType}.
     * @param mirror type mirror
     * @param structOverride override for struct annotation<br>
     *                       (only used for {@code DeclaredType}s that don't have a {@code ConfigType} yet)
     */
    void setStructOverride(@NotNull DeclaredType mirror, @Nullable Config.StructOverride structOverride);

    /**
     * Adds a {@link StructFactory} to use for creating a {@code ConfigType} from a specific {@link DeclaredType}.
     * @param structFactory struct factory to add
     */
    void addStructFactory(@NotNull StructFactory structFactory);

    /**
     * Gets a {@code ConfigType} from a {@link TypeMirror}.
     * @param mirror type mirror
     * @return matching type
     */
    @NotNull ConfigType fromMirror(@NotNull TypeMirror mirror);
}
