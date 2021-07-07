package io.github.speedbridgemc.config.processor.api.property;

import io.github.speedbridgemc.config.processor.api.ProcessingWorker;
import io.github.speedbridgemc.config.processor.api.util.MirrorElementPair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;

public interface ConfigPropertyExtensionFinder extends ProcessingWorker {
    interface Callback {
        <T extends ConfigPropertyExtension> void putExtension(@NotNull Class<T> type, @NotNull T extension);
    }

    void init(@NotNull ProcessingEnvironment processingEnv);
    void findExtensions(@NotNull Callback callback, @NotNull MirrorElementPair @NotNull ... pairs);
}
