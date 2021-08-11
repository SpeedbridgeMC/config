package io.github.speedbridgemc.config.processor.api.property;

import io.github.speedbridgemc.config.processor.api.ProcessingWorker;
import io.github.speedbridgemc.config.processor.api.util.MirrorElementPair;

import javax.annotation.processing.ProcessingEnvironment;

public interface ConfigPropertyExtensionFinder extends ProcessingWorker {
    interface Callback {
        <T extends ConfigPropertyExtension> void putExtension(Class<T> type, T extension);
    }

    void init(ProcessingEnvironment processingEnv);
    void findExtensions(Callback callback, MirrorElementPair ... pairs);
}
