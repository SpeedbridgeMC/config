package io.github.speedbridgemc.config.processor.api.component;

import io.github.speedbridgemc.config.processor.api.BaseProcessingWorker;

public abstract class BaseComponent extends BaseProcessingWorker implements Component {
    protected final String id;

    protected BaseComponent(String id) {
        this.id = id;
    }

    @Override
    public final String id() {
        return id;
    }
}
