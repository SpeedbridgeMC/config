package io.github.speedbridgemc.config.processor.serialize.api;

import io.github.speedbridgemc.config.processor.api.BaseIdentifiedProvider;

public abstract class BaseNamingStrategyProvider extends BaseIdentifiedProvider implements NamingStrategyProvider {
    public BaseNamingStrategyProvider(String id) {
        super(id);
    }
}
