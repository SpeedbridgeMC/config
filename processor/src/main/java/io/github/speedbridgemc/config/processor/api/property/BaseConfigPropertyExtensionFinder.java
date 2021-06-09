package io.github.speedbridgemc.config.processor.api.property;

import io.github.speedbridgemc.config.processor.api.BaseProcessingWorker;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public abstract class BaseConfigPropertyExtensionFinder extends BaseProcessingWorker implements ConfigPropertyExtensionFinder {
}
