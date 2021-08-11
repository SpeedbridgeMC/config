package io.github.speedbridgemc.config.processor.api;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public abstract class BaseProcessingWorker implements ProcessingWorker {
    private boolean initialized = false;
    protected ProcessingEnvironment processingEnv;
    protected Messager messager;
    protected Types types;
    protected Elements elements;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        if (initialized)
            throw new IllegalStateException("Already initialized!");
        initialized = true;
        this.processingEnv = processingEnv;
        messager = processingEnv.getMessager();
        types = processingEnv.getTypeUtils();
        elements = processingEnv.getElementUtils();
    }
}
