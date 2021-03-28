package io.github.speedbridgemc.config.processor.validate.api;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.JanksonContext;
import io.github.speedbridgemc.config.processor.validate.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.*;

public final class ValidatorContext {
    private final List<ValidatorDelegate> delegates;
    public final @NotNull TypeName configType;
    public final @NotNull String @NotNull [] options;
    public final @NotNull TypeSpec.Builder classBuilder;
    public final @NotNull Set<@NotNull String> generatedMethods, emptyMethods;
    public final @Nullable ClassName nonNullAnnotation, nullableAnnotation;
    public @NotNull String configName = "config";
    public @Nullable String defaultSrc;
    public @Nullable Element enclosingElement, element;
    public boolean canSet = true;
    public int nestingFactor = 1;

    public ValidatorContext(@NotNull TypeName configType, @NotNull String @NotNull [] options,
                            TypeSpec.@NotNull Builder classBuilder,
                            @Nullable ClassName nonNullAnnotation, @Nullable ClassName nullableAnnotation) {
        this.configType = configType;
        this.options = options;
        this.classBuilder = classBuilder;
        this.nonNullAnnotation = nonNullAnnotation;
        this.nullableAnnotation = nullableAnnotation;
        generatedMethods = new HashSet<>();
        emptyMethods = new HashSet<>();
        ServiceLoader<ValidatorDelegate> delegateLoader = ServiceLoader.load(ValidatorDelegate.class, JanksonContext.class.getClassLoader());
        delegates = new ArrayList<>();
        delegates.add(new PrimitiveValidatorDelegate());
        for (ValidatorDelegate delegate : delegateLoader)
            delegates.add(delegate);
        delegates.add(new ArrayValidatorDelegate());
        delegates.add(new ListValidatorDelegate());
        delegates.add(new NestedValidatorDelegate());
        delegates.add(new ObjectValidatorDelegate());
    }

    public void init(@NotNull ProcessingEnvironment processingEnv) {
        for (ValidatorDelegate delegate : delegates)
            delegate.init(processingEnv);
    }

    public void appendCheck(@NotNull TypeMirror type, @NotNull String src, @NotNull ErrorDelegate errDelegate,
                            @NotNull CodeBlock.Builder codeBuilder) {
        for (ValidatorDelegate delegate : delegates) {
            if (delegate.appendCheck(this, type, src, errDelegate, codeBuilder))
                break;
        }
    }

    public @NotNull Element getEffectiveElement() {
        if (element != null)
            return element;
        else if (enclosingElement != null)
            return enclosingElement;
        else
            throw new IllegalStateException();
    }

    public <T extends Annotation> @Nullable T getAnnotation(@NotNull Class<T> annotationType) {
        T annotation = null;
        if (enclosingElement != null)
            annotation = enclosingElement.getAnnotation(annotationType);
        if (element != null) {
            T annotation2 = element.getAnnotation(annotationType);
            if (annotation2 != null)
                annotation = annotation2;
        }
        return annotation;
    }
}
