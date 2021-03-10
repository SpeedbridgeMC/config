package io.github.speedbridgemc.config.processor.api;

import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

public final class TypeUtils {
    private TypeUtils() { }

    public static @NotNull TypeName getTypeName(@NotNull ProcessingEnvironment processingEnv, @NotNull String name) {
        TypeElement type = processingEnv.getElementUtils().getTypeElement(name);
        if (type == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Missing class \"" + name + "\"");
            return TypeName.VOID;
        }
        return TypeName.get(type.asType());
    }
}
