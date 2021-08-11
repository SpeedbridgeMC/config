package io.github.speedbridgemc.config.processor.api.property;

public abstract class BaseConfigPropertyExtension implements ConfigPropertyExtension {
    protected final String id;

    protected BaseConfigPropertyExtension(String id) {
        this.id = id;
    }

    @Override
    public final String id() {
        return id;
    }
}
