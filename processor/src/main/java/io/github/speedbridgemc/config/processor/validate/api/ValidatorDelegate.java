package io.github.speedbridgemc.config.processor.validate.api;

import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.processor.api.Provider;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

public interface ValidatorDelegate extends Provider {
    boolean appendCheck(@NotNull ValidatorContext ctx, @NotNull TypeMirror type, @NotNull String src,
                        @NotNull ErrorDelegate errDelegate, @NotNull CodeBlock.Builder codeBuilder);
}
