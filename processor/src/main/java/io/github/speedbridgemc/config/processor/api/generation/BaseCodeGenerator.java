package io.github.speedbridgemc.config.processor.api.generation;

import io.github.speedbridgemc.config.processor.api.BaseProcessingWorker;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;

public abstract class BaseCodeGenerator extends BaseProcessingWorker implements CodeGenerator {
    protected Filer filer;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
    }
}
