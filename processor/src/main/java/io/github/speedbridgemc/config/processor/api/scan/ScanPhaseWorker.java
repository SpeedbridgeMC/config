package io.github.speedbridgemc.config.processor.api.scan;

import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public abstract class ScanPhaseWorker {
    protected final @NotNull String id;
    protected ProcessingEnvironment processingEnv;
    protected Messager messager;
    protected Elements elements;
    protected Types types;

    protected ScanPhaseWorker(@NotNull String id) {
        this.id = id;
    }

    public final @NotNull String id() {
        return id;
    }

    public void init(@NotNull ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        messager = processingEnv.getMessager();
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
    }
}
