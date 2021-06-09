package io.github.speedbridgemc.config.processor.api.generation;

import com.squareup.javapoet.ClassName;
import io.github.speedbridgemc.config.processor.api.ProcessingWorker;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.TypeElement;

public interface CodeGenerator extends ProcessingWorker {
    interface Context {
        @NotNull ClassName generatedAnnotation();
        @NotNull String date();
        @NotNull String processorVersion();
    }

    void generateFor(@NotNull Context ctx, @NotNull ConfigType rootType, @NotNull TypeElement rootElement);
}
