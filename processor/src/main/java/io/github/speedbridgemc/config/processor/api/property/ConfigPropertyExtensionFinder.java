package io.github.speedbridgemc.config.processor.api.property;

import io.github.speedbridgemc.config.processor.api.util.MirrorElementPair;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;

public interface ConfigPropertyExtensionFinder {
    void init(@NotNull ProcessingEnvironment processingEnv);

    interface Callback {
        <T extends ConfigPropertyExtension> void putExtension(@NotNull Class<T> type, @NotNull T extension);
    }

    void findExtensions(@NotNull Callback callback, @NotNull MirrorElementPair @NotNull ... pairs);
}
