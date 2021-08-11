package io.github.speedbridgemc.config.processor.api.property;

import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;

import java.util.Optional;
import java.util.Set;

public interface ConfigProperty {
    String name();
    ConfigType type();
    Set<? extends ConfigPropertyFlag> flags();
    default boolean hasFlag(ConfigPropertyFlag flag) {
        return flags().contains(flag);
    }
    <E extends ConfigPropertyExtension> Optional<E> extension(Class<E> type);

    CodeBlock generateGet(String object, String destination);
    /**
     * Checks if this property can be set. If not, it's likely initialized using the struct's
     * {@linkplain io.github.speedbridgemc.config.processor.api.type.StructInstantiationStrategy instantiation strategy}.
     * @return {@literal true} if property can be set, {@literal false} otherwise
     */
    boolean canSet();
    CodeBlock generateSet(String object, String source);
}
