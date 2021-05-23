package io.github.speedbridgemc.config.processor.api.scan;

import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Collection;

public abstract class ConfigValueNamingStrategy extends ScanPhaseWorker {
    protected ConfigValueNamingStrategy(@NotNull String id) {
        super(id);
    }

    public abstract @NotNull String name(@NotNull TypeElement type, @NotNull Collection<? extends Element> elements, @NotNull String variantId);
}
