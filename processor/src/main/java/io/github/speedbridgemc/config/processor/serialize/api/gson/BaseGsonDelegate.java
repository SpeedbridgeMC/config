package io.github.speedbridgemc.config.processor.serialize.api.gson;

import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public abstract class BaseGsonDelegate implements GsonDelegate {
    protected boolean initialized;
    protected ProcessingEnvironment processingEnv;
    protected Messager messager;
    protected Elements elements;
    protected Types types;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        if (initialized)
            throw new IllegalStateException("Already initialized!");
        this.processingEnv = processingEnv;
        messager = processingEnv.getMessager();
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
        initialized = true;
    }
}
