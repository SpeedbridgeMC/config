package io.github.speedbridgemc.config.processor.validate.api;

import com.squareup.javapoet.CodeBlock;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public interface ErrorDelegate {
    @NotNull String getDescription();
    @NotNull CodeBlock generateThrow(@NotNull CodeBlock details);
    default @NotNull CodeBlock generateThrow(@NotNull String details) {
        return generateThrow(CodeBlock.of("$S", details));
    }

    default @NotNull ErrorDelegate derive(@NotNull BiFunction<@NotNull CodeBlock, @NotNull String, @NotNull CodeBlock> generator) {
        return new ErrorDelegate() {
            private final BiFunction<@NotNull CodeBlock, @NotNull String, @NotNull CodeBlock> gen = generator;

            @Override
            public @NotNull String getDescription() {
                return ErrorDelegate.this.getDescription();
            }

            @Override
            public @NotNull CodeBlock generateThrow(@NotNull CodeBlock details) {
                return gen.apply(details, getDescription());
            }
        };
    }

    static @NotNull ErrorDelegate simple(@NotNull String description) {
        return new ErrorDelegate() {
            private final String desc = description;

            @Override
            public @NotNull String getDescription() {
                return desc;
            }

            @Override
            public @NotNull CodeBlock generateThrow(@NotNull CodeBlock details) {
                return CodeBlock.builder()
                        .add("throw new $T(", IllegalArgumentException.class)
                        .add("$S", '"' + desc + '"')
                        .add(" + ").add(details)
                        .add(")")
                        .build();
            }
        };
    }
}
