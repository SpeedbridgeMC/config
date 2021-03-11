package io.github.speedbridgemc.config.processor.serialize.api;

import io.github.speedbridgemc.config.processor.api.BaseProvider;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;

public abstract class BaseSerializerProvider extends BaseProvider implements SerializerProvider {
    public BaseSerializerProvider(String id) {
        super(id);
    }
}
