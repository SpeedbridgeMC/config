package io.github.speedbridgemc.config.processor.serialize.api;

import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;

public abstract class BaseSerializerProvider implements SerializerProvider {
    protected final String id;
    protected ProcessingEnvironment processingEnv;

    public BaseSerializerProvider(String id) {
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
