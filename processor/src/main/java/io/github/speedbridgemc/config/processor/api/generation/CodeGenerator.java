package io.github.speedbridgemc.config.processor.api.generation;

import com.squareup.javapoet.ClassName;
import io.github.speedbridgemc.config.processor.api.ProcessingWorker;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;

import javax.lang.model.element.TypeElement;

public interface CodeGenerator extends ProcessingWorker {
    interface Context {
        ClassName generatedAnnotation();
        String date();
        String processorVersion();
    }

    void generateFor(Context ctx, ConfigType rootType, TypeElement rootElement);
}
