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
    @NotNull CodeBlock generateSet(@NotNull String object, @NotNull String source);
}
