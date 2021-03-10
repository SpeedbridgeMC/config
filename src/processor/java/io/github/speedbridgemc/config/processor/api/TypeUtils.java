package io.github.speedbridgemc.config.processor.api;

import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;

public final class TypeUtils {
    private TypeUtils() { }

    public static @NotNull TypeName getTypeName(@NotNull ProcessingEnvironment processingEnv, @NotNull String name) {
        return TypeName.get(processingEnv.getElementUtils().getTypeElement(name).asType());
    }
}
