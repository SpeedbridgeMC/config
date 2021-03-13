package io.github.speedbridgemc.config.processor.serialize.api.gson;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.speedbridgemc.config.processor.serialize.gson.ListArrayGsonDelegate;
import io.github.speedbridgemc.config.processor.serialize.gson.MapGsonDelegate;
import io.github.speedbridgemc.config.processor.serialize.gson.NestedGsonDelegate;
import io.github.speedbridgemc.config.processor.serialize.gson.PrimitiveGsonDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.*;

public final class GsonContext {
    private final List<GsonDelegate> delegates;
    private final NestedGsonDelegate nestedDelegate; // separated from the standard delegates since its a "last resort" measure
    public final @NotNull TypeSpec.Builder classBuilder;
    public final @NotNull Set<@NotNull String> generatedMethods;
    public final @NotNull Map<@NotNull String, @NotNull String> gotFlags;
    public final @NotNull Map<@NotNull String, @Nullable String> missingErrorMessages;
    public final @NotNull TypeName readerType, writerType, tokenType;
    public final @Nullable ClassName nonNullAnnotation, nullableAnnotation;
    public @NotNull String readerName = "reader", writerName = "writer";
    public @Nullable VariableElement fieldElement;

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
        delegates.add(new PrimitiveGsonDelegate());
        for (GsonDelegate delegate : delegateLoader)
            delegates.add(delegate);
        delegates.add(new ListArrayGsonDelegate());
        delegates.add(new MapGsonDelegate());
        nestedDelegate = new NestedGsonDelegate();
    }

    public void init(@NotNull ProcessingEnvironment processingEnv) {
        for (GsonDelegate delegate : delegates)
            delegate.init(processingEnv);
        nestedDelegate.init(processingEnv);
    }

    public boolean appendRead(@NotNull TypeMirror type, @Nullable String name, @NotNull String dest, @NotNull CodeBlock.Builder codeBuilder, boolean useNested) {
        for (GsonDelegate delegate : delegates) {
            if (delegate.appendRead(this, type, name, dest, codeBuilder))
                return true;
        }
        return useNested && appendReadNested(type, name, dest, codeBuilder);
    }

    public boolean appendRead(@NotNull TypeMirror type, @Nullable String name, @NotNull String dest, @NotNull CodeBlock.Builder codeBuilder) {
        return appendRead(type, name, dest, codeBuilder, true);
    }

    public boolean appendReadNested(@NotNull TypeMirror type, @Nullable String name, @NotNull String dest, @NotNull CodeBlock.Builder codeBuilder) {
        return nestedDelegate.appendRead(this, type, name, dest, codeBuilder);
    }

    public boolean appendWrite(@NotNull TypeMirror type, @Nullable String name, @NotNull String src, @NotNull CodeBlock.Builder codeBuilder, boolean useNested) {
        for (GsonDelegate delegate : delegates) {
            if (delegate.appendWrite(this, type, name, src, codeBuilder))
                return true;
        }
        return useNested && appendWriteNested(type, name, src, codeBuilder);
    }

    public boolean appendWrite(@NotNull TypeMirror type, @Nullable String name, @NotNull String src, @NotNull CodeBlock.Builder codeBuilder) {
        return appendWrite(type, name, src, codeBuilder, true);
    }

    public boolean appendWriteNested(@NotNull TypeMirror type, @Nullable String name, @NotNull String src, @NotNull CodeBlock.Builder codeBuilder) {
        return nestedDelegate.appendWrite(this, type, name, src, codeBuilder);
    }
}
