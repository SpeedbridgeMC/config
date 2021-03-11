package io.github.speedbridgemc.config.processor.serialize.api.gson;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.speedbridgemc.config.processor.serialize.gson.NestedGsonRWDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import java.util.*;

public final class GsonRWContext {
    private final List<GsonRWDelegate> delegates;
    private final NestedGsonRWDelegate nestedDelegate; // separated from the standard delegates since its a "last resort" measure
    public final @NotNull TypeSpec.Builder classBuilder;
    public final @NotNull Set<@NotNull String> generatedMethods;
    public final @NotNull TypeName readerType, writerType, tokenType;
    public @NotNull String varName = "config";
    public final @Nullable ClassName nonNullAnnotation, nullableAnnotation;

    @SuppressWarnings("RedundantSuppression")
    public GsonRWContext(TypeSpec.Builder classBuilder, @NotNull TypeName readerType, @NotNull TypeName writerType,
                         @NotNull TypeName tokenType, @Nullable ClassName nonNullAnnotation, @Nullable ClassName nullableAnnotation) {
        this.classBuilder = classBuilder;
        this.readerType = readerType;
        this.writerType = writerType;
        this.tokenType = tokenType;
        this.nonNullAnnotation = nonNullAnnotation;
        this.nullableAnnotation = nullableAnnotation;
        generatedMethods = new HashSet<>();
        ServiceLoader<GsonRWDelegate> delegateLoader = ServiceLoader.load(GsonRWDelegate.class, GsonRWContext.class.getClassLoader());
        delegates = new ArrayList<>();
        for (GsonRWDelegate delegate : delegateLoader)
            delegates.add(delegate);
        nestedDelegate = new NestedGsonRWDelegate();
    }

    public void init(@NotNull ProcessingEnvironment processingEnv) {
        for (GsonRWDelegate delegate : delegates)
            delegate.init(processingEnv);
        nestedDelegate.init(processingEnv);
    }

    public void appendRead(@NotNull VariableElement field, @NotNull CodeBlock.Builder codeBuilder) {
        for (GsonRWDelegate delegate : delegates) {
            if (delegate.appendRead(this, field, codeBuilder))
                return;
        }
        nestedDelegate.appendRead(this, field, codeBuilder);
    }

    public void appendWrite(@NotNull VariableElement field, @NotNull CodeBlock.Builder codeBuilder) {
        for (GsonRWDelegate delegate : delegates) {
            if (delegate.appendWrite(this, field, codeBuilder))
                return;
        }
        nestedDelegate.appendWrite(this, field, codeBuilder);
    }
}
