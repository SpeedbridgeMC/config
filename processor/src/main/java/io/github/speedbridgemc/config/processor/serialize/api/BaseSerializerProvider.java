package io.github.speedbridgemc.config.processor.serialize.api;

import io.github.speedbridgemc.config.processor.api.BaseIdentifiedProvider;

public abstract class BaseSerializerProvider extends BaseIdentifiedProvider implements SerializerProvider {
    public BaseSerializerProvider(String id) {
        super(id);
    }
}
