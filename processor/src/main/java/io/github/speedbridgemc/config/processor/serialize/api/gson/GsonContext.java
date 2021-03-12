package io.github.speedbridgemc.config.processor.serialize.api.gson;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.speedbridgemc.config.processor.serialize.gson.NestedGsonDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import java.util.*;

public final class GsonContext {
    private final List<GsonDelegate> delegates;
    private final NestedGsonDelegate nestedDelegate; // separated from the standard delegates since its a "last resort" measure
    public final @NotNull TypeSpec.Builder classBuilder;
    public final @NotNull Set<@NotNull String> generatedMethods;
    public final @NotNull Map<@NotNull String, @NotNull String> gotFlags;
    public final @NotNull Map<@NotNull String, @Nullable String> missingErrorMessages;
    public final @NotNull TypeName readerType, writerType, tokenType;
    public @NotNull String configName = "config";
    public final @Nullable ClassName nonNullAnnotation, nullableAnnotation;

    @SuppressWarnings("RedundantSuppression")
    public GsonContext(TypeSpec.@NotNull Builder classBuilder, @NotNull TypeName readerType, @NotNull TypeName writerType,
                       @NotNull TypeName tokenType, @Nullable ClassName nonNullAnnotation, @Nullable ClassName nullableAnnotation) {
        this.classBuilder = classBuilder;
        this.readerType = readerType;
        this.writerType = writerType;
        this.tokenType = tokenType;
        this.nonNullAnnotation = nonNullAnnotation;
        this.nullableAnnotation = nullableAnnotation;
        generatedMethods = new HashSet<>();
        gotFlags = new LinkedHashMap<>();
        missingErrorMessages = new HashMap<>();
        ServiceLoader<GsonDelegate> delegateLoader = ServiceLoader.load(GsonDelegate.class, GsonContext.class.getClassLoader());
        delegates = new ArrayList<>();
        for (GsonDelegate delegate : delegateLoader)
            delegates.add(delegate);
        nestedDelegate = new NestedGsonDelegate();
    }

    public void init(@NotNull ProcessingEnvironment processingEnv) {
        for (GsonDelegate delegate : delegates)
            delegate.init(processingEnv);
        nestedDelegate.init(processingEnv);
    }

    public void appendRead(@NotNull VariableElement field, @NotNull CodeBlock.Builder codeBuilder) {
        for (GsonDelegate delegate : delegates) {
            if (delegate.appendRead(this, field, codeBuilder))
                return;
        }
        nestedDelegate.appendRead(this, field, codeBuilder);
    }

    public void appendWrite(@NotNull VariableElement field, @NotNull CodeBlock.Builder codeBuilder) {
        for (GsonDelegate delegate : delegates) {
            if (delegate.appendWrite(this, field, codeBuilder))
                return;
        }
        nestedDelegate.appendWrite(this, field, codeBuilder);
    }
}
