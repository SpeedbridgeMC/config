package io.github.speedbridgemc.config.processor.api.generation;

import io.github.speedbridgemc.config.processor.api.BaseProcessingWorker;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;

public abstract class BaseCodeGenerator extends BaseProcessingWorker implements CodeGenerator {
    protected Filer filer;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
    }
}
