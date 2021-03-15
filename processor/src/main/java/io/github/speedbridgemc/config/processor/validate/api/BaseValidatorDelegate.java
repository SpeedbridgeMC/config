package io.github.speedbridgemc.config.processor.validate.api;

import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;

public abstract class BaseValidatorDelegate implements ValidatorDelegate {
    protected ProcessingEnvironment processingEnv;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }
}
