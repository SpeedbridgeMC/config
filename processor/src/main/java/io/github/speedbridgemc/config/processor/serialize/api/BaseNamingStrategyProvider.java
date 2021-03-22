package io.github.speedbridgemc.config.processor.serialize.api;

import io.github.speedbridgemc.config.processor.api.BaseProvider;

public abstract class BaseNamingStrategyProvider extends BaseProvider implements NamingStrategyProvider {
    public BaseNamingStrategyProvider(String id) {
        super(id);
    }
}
