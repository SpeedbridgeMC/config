package io.github.speedbridgemc.config.processor.api;

import org.jetbrains.annotations.NotNull;

public abstract class BaseIdentifiedProvider extends BaseProvider implements IdentifiedProvider {
    protected final String id;

    public BaseIdentifiedProvider(String id) {
        this.id = id;
    }

    @Override
    public @NotNull String getId() {
        return id;
    }
}
