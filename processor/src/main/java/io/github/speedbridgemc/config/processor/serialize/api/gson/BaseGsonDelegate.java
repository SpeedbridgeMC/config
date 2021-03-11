package io.github.speedbridgemc.config.processor.serialize.api.gson;

import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;

public abstract class BaseGsonDelegate implements GsonDelegate {
    protected ProcessingEnvironment processingEnv;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }
}
