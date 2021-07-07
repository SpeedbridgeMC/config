package io.github.speedbridgemc.config.processor.api.property;

import org.jetbrains.annotations.NotNull;

public abstract class BaseConfigPropertyExtension implements ConfigPropertyExtension {
    protected final @NotNull String id;

    protected BaseConfigPropertyExtension(@NotNull String id) {
        this.id = id;
    }

    @Override
    public final @NotNull String id() {
        return id;
    }
}
