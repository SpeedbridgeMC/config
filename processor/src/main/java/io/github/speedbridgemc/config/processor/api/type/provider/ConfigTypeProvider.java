package io.github.speedbridgemc.config.processor.api.type.provider;

import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.naming.NamingStrategy;
import io.github.speedbridgemc.config.processor.api.property.ConfigPropertyExtensionFinder;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import io.github.speedbridgemc.config.processor.api.type.ConfigTypeKind;
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
    void init(ProcessingEnvironment processingEnv);

    /**
     * Gets a {@code ConfigType} that represents a primitive.
     * @param kind primitive kind
     * @return type of specified kind
     */
    ConfigType primitiveOf(ConfigTypeKind kind);

    /**
     * Gets a {@code ConfigType} that represents an array, with the specified type as its component type.
     * @param componentType component type
     * @return array type
     */
    ConfigType arrayOf(ConfigType componentType);

    /**
     * Gets a {@code ConfigType} that represents a map, with the specified types as its key and value types.
     * @param keyType key type
     * @param valueType value type
     * @return map type
     */
    ConfigType mapOf(ConfigType keyType, ConfigType valueType);

    /**
     * Adds a preconfigured {@code ConfigType} of kind {@link ConfigTypeKind#STRUCT STRUCT}.
     * @param type matching type
     */
    void addStruct(ConfigType type);

    /**
     * Adds a {@link ConfigPropertyExtensionFinder} to use when creating struct types.
     * @param extensionFinder finder to add
     */
    void addExtensionFinder(ConfigPropertyExtensionFinder extensionFinder);

    /**
     * Sets the naming strategy to use when creating struct types.
     * @param strategy naming strategy to use
     * @param variant strategy variant to use
     */
    void setNamingStrategy(NamingStrategy strategy, String variant);

    /**
     * Sets a {@link io.github.speedbridgemc.config.Config.StructOverride StructOverride} for creating a {@code ConfigType}
     * from a specific {@link DeclaredType}.
     * @param mirror type mirror
     * @param structOverride override for struct annotation<br>
     *                       (only used for {@code DeclaredType}s that don't have a {@code ConfigType} yet)
     */
    void setStructOverride(DeclaredType mirror, @Nullable Config.StructOverride structOverride);

    /**
     * Adds a {@link StructFactory} to use for creating a {@code ConfigType} from a specific {@link DeclaredType}.
     * @param structFactory struct factory to add
     */
    void addStructFactory(StructFactory structFactory);

    /**
     * Gets a {@code ConfigType} from a {@link TypeMirror}.
     * @param mirror type mirror
     * @return matching type
     */
    ConfigType fromMirror(TypeMirror mirror);
}
