package io.github.speedbridgemc.config.processor.api.naming;

import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;

public interface NamingStrategy {
    @NotNull String id();
    void init(@NotNull ProcessingEnvironment processingEnv);
    @NotNull String name(@NotNull String variant, @NotNull String originalName);
}
