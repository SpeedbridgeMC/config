package io.github.speedbridgemc.config.processor.api;

import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.Exclude;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    public static @Nullable TypeMirror getTypeMirror(@NotNull ProcessingEnvironment processingEnv, @NotNull String name) {
        TypeElement type = processingEnv.getElementUtils().getTypeElement(name);
        if (type == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Missing class \"" + name + "\"");
            return null;
        }
        return type.asType();
    }

    public static @NotNull List<@NotNull VariableElement> getFieldsIn(@NotNull TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .map(element -> {
                    if (element.getKind() == ElementKind.FIELD)
                        return (VariableElement) element;
                    return null;
                }).filter(Objects::nonNull)
                .filter(variableElement -> variableElement.getAnnotation(Exclude.class) == null)
                .filter(variableElement -> {
                    Set<Modifier> modifiers = variableElement.getModifiers();
                    return !modifiers.contains(Modifier.TRANSIENT)
                            && !modifiers.contains(Modifier.STATIC)
                            && !modifiers.contains(Modifier.FINAL);
                })
                .collect(Collectors.toList());
    }

    public static @NotNull List<@NotNull ExecutableElement> getMethodsIn(@NotNull TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .map(element -> {
                    if (element.getKind() == ElementKind.METHOD)
                        return (ExecutableElement) element;
                    return null;
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
