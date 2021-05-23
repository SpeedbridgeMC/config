package io.github.speedbridgemc.config.processor.api.scan;

import io.github.speedbridgemc.config.Config;
import io.github.speedbridgemc.config.processor.api.ConfigValue;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.TypeElement;

public abstract class ConfigValueScanner extends Scanner {
    protected ConfigValueScanner(@NotNull String id) {
        super(id);
    }

    public interface Callback {
        void addField(@NotNull ConfigValue field);
    }

    public abstract void scan(@NotNull TypeElement type, @NotNull Config config, @NotNull Callback callback);
}
