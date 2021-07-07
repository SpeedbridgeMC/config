package io.github.speedbridgemc.config.processor.api.naming;

import io.github.speedbridgemc.config.processor.api.Identifiable;
import io.github.speedbridgemc.config.processor.api.ProcessingWorker;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;

public interface NamingStrategy extends Identifiable, ProcessingWorker {
    @NotNull String name(@NotNull String variant, @NotNull String originalName);
}
