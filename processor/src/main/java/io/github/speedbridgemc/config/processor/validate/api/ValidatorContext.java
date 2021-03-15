package io.github.speedbridgemc.config.processor.validate.api;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.JanksonContext;
import io.github.speedbridgemc.config.processor.validate.NestedValidatorDelegate;
import io.github.speedbridgemc.config.processor.validate.PrimitiveValidatorDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.*;

public final class ValidatorContext {
    private final List<ValidatorDelegate> delegates;
    public final @NotNull TypeName configType;
    public final @NotNull String @NotNull [] options;
    public final @NotNull TypeSpec.Builder classBuilder;
    public final @NotNull Set<@NotNull String> generatedMethods;
    public final @Nullable ClassName nonNullAnnotation, nullableAnnotation;
    public @NotNull String configName = "config";
    public @Nullable VariableElement enclosingFieldElement, fieldElement;

    public ValidatorContext(@NotNull TypeName configType, @NotNull String @NotNull [] options,
                            TypeSpec.@NotNull Builder classBuilder,
                            @Nullable ClassName nonNullAnnotation, @Nullable ClassName nullableAnnotation) {
        this.configType = configType;
        this.options = options;
        this.classBuilder = classBuilder;
        this.nonNullAnnotation = nonNullAnnotation;
        this.nullableAnnotation = nullableAnnotation;
        generatedMethods = new HashSet<>();
        ServiceLoader<ValidatorDelegate> delegateLoader = ServiceLoader.load(ValidatorDelegate.class, JanksonContext.class.getClassLoader());
        delegates = new ArrayList<>();
        delegates.add(new PrimitiveValidatorDelegate());
        for (ValidatorDelegate delegate : delegateLoader)
            delegates.add(delegate);
        delegates.add(new NestedValidatorDelegate());
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

    public @NotNull VariableElement getEffectiveFieldElement() {
        if (fieldElement != null)
            return fieldElement;
        else if (enclosingFieldElement != null)
            return enclosingFieldElement;
        else
            throw new IllegalStateException();
    }
}
