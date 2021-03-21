package io.github.speedbridgemc.config.processor.validate.api;

import com.squareup.javapoet.CodeBlock;
import org.jetbrains.annotations.NotNull;

import java.util.function.UnaryOperator;

public interface ErrorDelegate {
    @NotNull String getDescription();
    @NotNull CodeBlock generateThrow(@NotNull CodeBlock details);

    default @NotNull CodeBlock generateThrow(@NotNull String details) {
        return generateThrow(CodeBlock.of("$S", details));
    }
    default @NotNull ErrorDelegate derive(@NotNull UnaryOperator<@NotNull CodeBlock> throwGenerator) {
        return new ErrorDelegate() {
            private final UnaryOperator<CodeBlock> throwFunc = throwGenerator;

            @Override
            public @NotNull String getDescription() {
                return ErrorDelegate.this.getDescription();
            }

            @Override
            public @NotNull CodeBlock generateThrow(@NotNull CodeBlock details) {
                return throwFunc.apply(details);
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
