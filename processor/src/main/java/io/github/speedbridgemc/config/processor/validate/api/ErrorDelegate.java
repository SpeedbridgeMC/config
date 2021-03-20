package io.github.speedbridgemc.config.processor.validate.api;

import com.squareup.javapoet.CodeBlock;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ErrorDelegate {
    @NotNull CodeBlock generateThrow(@NotNull CodeBlock details);

    default @NotNull CodeBlock generateThrow(@NotNull String details) {
        return generateThrow(CodeBlock.of("$S", details));
    }

    static @NotNull ErrorDelegate simple(@NotNull String description) {
        return details -> CodeBlock.builder()
                .add("throw new $T(", IllegalArgumentException.class)
                .add("$S", '"' + description + '"')
                .add(" + ").add(details)
                .add(")")
                .build();
    }
}
