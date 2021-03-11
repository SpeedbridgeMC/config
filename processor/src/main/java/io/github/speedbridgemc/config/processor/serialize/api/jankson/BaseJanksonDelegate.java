package io.github.speedbridgemc.config.processor.serialize.api.jankson;

import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;

public abstract class BaseJanksonDelegate implements JanksonDelegate {
    protected ProcessingEnvironment processingEnv;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }
}
