package io.github.speedbridgemc.config.processor.api.property;

import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface ConfigProperty {
    @NotNull String name();
    @NotNull ConfigType type();
    @NotNull <E extends ConfigPropertyExtension> Optional<E> extension(@NotNull Class<E> type);

    @NotNull CodeBlock generateGet(@NotNull String object, @NotNull String destination);
    /**
     * Checks if this property can be set. If not, it's likely initialized using the struct's
     * {@linkplain io.github.speedbridgemc.config.processor.api.type.StructInstantiationStrategy instantiation strategy}.
     * @return {@literal true} if property can be set, {@literal false} otherwise
     */
    boolean canSet();
    @NotNull CodeBlock generateSet(@NotNull String object, @NotNull String source);
}
