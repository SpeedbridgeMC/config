package io.github.speedbridgemc.config.processor.api.scan;

import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.ConfigValueExtension;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Collection;

public abstract class ConfigValueExtensionScanner extends ScanPhaseWorker {
    protected ConfigValueExtensionScanner(@NotNull String id) {
        super(id);
    }

    public interface Callback {
        <T extends ConfigValueExtension> void putExtension(@NotNull Class<T> type, @NotNull T ext);
    }

    public abstract void findExtensions(@NotNull TypeElement type, @NotNull Config config, @NotNull Collection<? extends Element> elements, @NotNull Callback callback);
}
