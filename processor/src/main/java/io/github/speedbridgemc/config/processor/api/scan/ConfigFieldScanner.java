package io.github.speedbridgemc.config.processor.api.scan;

import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.ConfigField;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.TypeElement;

public abstract class ConfigFieldScanner extends Scanner {
    protected ConfigFieldScanner(@NotNull String id) {
        super(id);
    }

    public interface Callback {
        void addField(@NotNull ConfigField field);
    }

    public abstract void scan(@NotNull TypeElement type, @NotNull Config config, @NotNull Callback callback);
}
