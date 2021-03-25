package io.github.speedbridgemc.config.processor.api;

import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

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
