package io.github.speedbridgemc.config.processor.validate.api;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.JanksonContext;
import io.github.speedbridgemc.config.processor.validate.NestedValidatorDelegate;
import io.github.speedbridgemc.config.processor.validate.ObjectValidatorDelegate;
import io.github.speedbridgemc.config.processor.validate.PrimitiveValidatorDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
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
    public @NotNull String defaultsName = "DEFAULTS";
    public boolean canUseDefaults = true;
    public @Nullable Element enclosingElement, element;

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
        delegates.add(new NestedValidatorDelegate());
        delegates.add(new ObjectValidatorDelegate());
    }

    public void init(@NotNull ProcessingEnvironment processingEnv) {
        for (ValidatorDelegate delegate : delegates)
            delegate.init(processingEnv);
    }

    public void appendCheck(@NotNull TypeMirror type, @NotNull String src, @NotNull String description,
                            @NotNull CodeBlock.Builder codeBuilder) {
        for (ValidatorDelegate delegate : delegates) {
            if (delegate.appendCheck(this, type, src, description, codeBuilder))
                break;
        }
    }

    public @NotNull Element getEffectiveElement() {
        if (enclosingElement != null)
            return enclosingElement;
        else if (element != null)
            return element;
        else
            throw new IllegalStateException();
    }

    public <T extends Annotation> @Nullable T getAnnotation(@NotNull Class<T> annotationType) {
        T annotation = null;
        if (enclosingElement != null)
            annotation = enclosingElement.getAnnotation(annotationType);
        if (annotation == null && element != null)
            annotation = element.getAnnotation(annotationType);
        return annotation;
    }
}
