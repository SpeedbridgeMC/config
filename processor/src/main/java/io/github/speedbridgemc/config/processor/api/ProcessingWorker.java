package io.github.speedbridgemc.config.processor.api;

import javax.annotation.processing.ProcessingEnvironment;

public interface ProcessingWorker {
    void init(ProcessingEnvironment processingEnv);
}
