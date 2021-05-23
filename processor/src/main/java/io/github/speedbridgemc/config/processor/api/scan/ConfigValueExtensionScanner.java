package io.github.speedbridgemc.config.processor.api.scan;

import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.ConfigValueExtension;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Collection;

public abstract class ConfigValueExtensionScanner extends Scanner {
    protected ConfigValueExtensionScanner(@NotNull String id) {
        super(id);
    }

    public interface Callback {
        void addExtension(@NotNull ConfigValueExtension ext);
    }

    public abstract void scan(@NotNull TypeElement type, @NotNull Config config, @NotNull Collection<? extends Element> elements, @NotNull Callback callback);
}
