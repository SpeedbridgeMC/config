package io.github.speedbridgemc.config.processor.api.naming;

import io.github.speedbridgemc.config.processor.api.BaseProcessingWorker;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public abstract class BaseNamingStrategy extends BaseProcessingWorker implements NamingStrategy {
    protected final @NotNull String id;

    protected BaseNamingStrategy(@NotNull String id) {
        this.id = id;
    }

    @Override
    public final @NotNull String id() {
        return id;
    }
}
