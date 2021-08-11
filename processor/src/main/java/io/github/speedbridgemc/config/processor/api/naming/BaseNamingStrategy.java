package io.github.speedbridgemc.config.processor.api.naming;

import io.github.speedbridgemc.config.processor.api.BaseProcessingWorker;

public abstract class BaseNamingStrategy extends BaseProcessingWorker implements NamingStrategy {
    protected final String id;

    protected BaseNamingStrategy(String id) {
        this.id = id;
    }

    @Override
    public final String id() {
        return id;
    }
}
