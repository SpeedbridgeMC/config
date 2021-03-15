package io.github.speedbridgemc.config.processor.serialize.api.jankson;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.speedbridgemc.config.processor.serialize.jankson.ListArrayJanksonDelegate;
import io.github.speedbridgemc.config.processor.serialize.jankson.MapJanksonDelegate;
import io.github.speedbridgemc.config.processor.serialize.jankson.NestedJanksonDelegate;
import io.github.speedbridgemc.config.processor.serialize.jankson.PrimitiveJanksonDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.*;

public final class JanksonContext {
    private final List<JanksonDelegate> delegates;
    private final NestedJanksonDelegate nestedDelegate; // separated from the standard delegates since its a "last resort" measure
    public final @NotNull TypeSpec.Builder classBuilder;
    public final @NotNull Set<@NotNull String> generatedMethods;
    public final @NotNull TypeName elementType, objectType, primitiveType, arrayType, nullType;
    public final @Nullable ClassName nonNullAnnotation, nullableAnnotation;
    public @NotNull String elementName = "jElem", objectName = "jObj", primitiveName = "jPrim", arrayName = "jArr";
    public @Nullable VariableElement fieldElement;

    @SuppressWarnings("RedundantSuppression")
    public JanksonContext(TypeSpec.@NotNull Builder classBuilder,
                          @NotNull TypeName elementType, @NotNull TypeName objectType,
                          @NotNull TypeName primitiveType, @NotNull TypeName arrayType,
                          @NotNull TypeName nullType,
                          @Nullable ClassName nonNullAnnotation, @Nullable ClassName nullableAnnotation) {
        this.classBuilder = classBuilder;
        this.elementType = elementType;
        this.objectType = objectType;
        this.primitiveType = primitiveType;
        this.arrayType = arrayType;
        this.nullType = nullType;
        this.nonNullAnnotation = nonNullAnnotation;
        this.nullableAnnotation = nullableAnnotation;
        generatedMethods = new HashSet<>();
        ServiceLoader<JanksonDelegate> delegateLoader = ServiceLoader.load(JanksonDelegate.class, JanksonContext.class.getClassLoader());
        delegates = new ArrayList<>();
        delegates.add(new PrimitiveJanksonDelegate());
        for (JanksonDelegate delegate : delegateLoader)
            delegates.add(delegate);
        delegates.add(new ListArrayJanksonDelegate());
        delegates.add(new MapJanksonDelegate());
        nestedDelegate = new NestedJanksonDelegate();
    }

    public void init(@NotNull ProcessingEnvironment processingEnv) {
        for (JanksonDelegate delegate : delegates)
            delegate.init(processingEnv);
        nestedDelegate.init(processingEnv);
    }

    public boolean appendRead(@NotNull TypeMirror type, @Nullable String name, @NotNull String dest, @NotNull CodeBlock.Builder codeBuilder, boolean useNested) {
        for (JanksonDelegate delegate : delegates) {
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
        for (JanksonDelegate delegate : delegates) {
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
