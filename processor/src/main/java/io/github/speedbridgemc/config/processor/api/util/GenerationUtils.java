package io.github.speedbridgemc.config.processor.api.util;

import com.squareup.javapoet.ClassName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

public final class GenerationUtils {
    private GenerationUtils() { }

    private static ClassName generatedAnnotation;

    /**
     * Gets the {@code @Generated} annotation's {@link ClassName}.
     * @param processingEnv processing environment
     * @return class name of {@code @Generated} annotation
     */
    public static ClassName getGeneratedAnnotation(ProcessingEnvironment processingEnv) {
        if (generatedAnnotation == null) {
            TypeElement elem = processingEnv.getElementUtils().getTypeElement("javax.annotation.processing.Generated");
            if (elem == null)
                elem = processingEnv.getElementUtils().getTypeElement("javax.annotation.Generated");
            if (elem == null) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Missing @Generated annotation!");
                generatedAnnotation = ClassName.get(Deprecated.class);
                return generatedAnnotation;
            }
            generatedAnnotation = ClassName.get(elem);
        }
        return generatedAnnotation;
    }

}
