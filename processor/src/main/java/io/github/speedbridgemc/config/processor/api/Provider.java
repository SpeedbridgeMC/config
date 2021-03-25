package io.github.speedbridgemc.config.processor.api;

import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;

public interface Provider {
    void init(@NotNull ProcessingEnvironment processingEnv);
}
