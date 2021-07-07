package io.github.speedbridgemc.config.processor.api.component;

import io.github.speedbridgemc.config.processor.api.BaseProcessingWorker;
import org.jetbrains.annotations.NotNull;

public abstract class BaseComponent extends BaseProcessingWorker implements Component {
    protected final @NotNull String id;

    protected BaseComponent(@NotNull String id) {
        this.id = id;
    }

    @Override
    public final @NotNull String id() {
        return id;
    }
}
