package io.github.speedbridgemc.config.processor.api.property;

import io.github.speedbridgemc.config.processor.impl.ConfigProcessor;
import org.jetbrains.annotations.NotNull;

public enum StandardConfigPropertyFlags implements ConfigPropertyFlag {
    OPTIONAL("optional");

    private final String id;

    StandardConfigPropertyFlags(String path) {
        this.id = ConfigProcessor.id(path);
    }

    @Override
    public @NotNull String id() {
        return id;
    }
}
