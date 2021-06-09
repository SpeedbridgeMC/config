package io.github.speedbridgemc.config.processor.api.component;

import io.github.speedbridgemc.config.processor.api.Identifiable;
import io.github.speedbridgemc.config.processor.api.ProcessingWorker;
import io.github.speedbridgemc.config.processor.api.generation.CodeGenerator;
import io.github.speedbridgemc.config.processor.api.property.ConfigPropertyExtensionFinder;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface Component extends Identifiable, ProcessingWorker {
    @NotNull Collection<? extends ConfigPropertyExtensionFinder> propertyExtensionFinders();
    @NotNull Collection<? extends CodeGenerator> codeGenerators();
}
