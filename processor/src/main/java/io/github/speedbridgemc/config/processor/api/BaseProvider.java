package io.github.speedbridgemc.config.processor.api;

import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;

public abstract class BaseProvider implements Provider {
    protected final String id;
    protected ProcessingEnvironment processingEnv;

    public BaseProvider(String id) {
        this.id = id;
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }
}
