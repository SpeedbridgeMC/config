package io.github.speedbridgemc.config.processor.api.type;

import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public abstract class BaseStructFactory implements StructFactory {
    private boolean initialized = false;
    protected ProcessingEnvironment processingEnv;
    protected Messager messager;
    protected Types types;
    protected Elements elements;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        if (initialized)
            throw new IllegalStateException("Already initialized!");
        initialized = true;
        this.processingEnv = processingEnv;
        messager = processingEnv.getMessager();
        types = processingEnv.getTypeUtils();
        elements = processingEnv.getElementUtils();
    }
}
