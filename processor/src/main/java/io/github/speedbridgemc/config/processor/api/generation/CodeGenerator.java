package io.github.speedbridgemc.config.processor.api.generation;

import io.github.speedbridgemc.config.processor.api.ProcessingWorker;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.TypeElement;

public interface CodeGenerator extends ProcessingWorker {
    void generateFor(@NotNull ConfigType rootType, @NotNull TypeElement rootElement);
}
