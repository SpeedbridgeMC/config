package io.github.speedbridgemc.config.processor.serialize.api.jankson;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.speedbridgemc.config.processor.serialize.jankson.NestedJanksonDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import java.util.*;

public final class JanksonContext {
    private final List<JanksonDelegate> delegates;
    private final NestedJanksonDelegate nestedDelegate; // separated from the standard delegates since its a "last resort" measure
    public final @NotNull TypeSpec.Builder classBuilder;
    public final @NotNull Set<@NotNull String> generatedMethods;
    public final @NotNull Map<@NotNull String, @NotNull String> missingErrorMessages;
    public final @NotNull TypeName objectType, primitiveType;
    public @NotNull String configName = "config", objectName = "obj", primitiveName = "prim";
    public final @Nullable ClassName nonNullAnnotation, nullableAnnotation;

    @SuppressWarnings("RedundantSuppression")
    public JanksonContext(TypeSpec.@NotNull Builder classBuilder, @NotNull TypeName objectType,
                          @NotNull TypeName primitiveType, @Nullable ClassName nonNullAnnotation, @Nullable ClassName nullableAnnotation) {
        this.classBuilder = classBuilder;
        this.objectType = objectType;
        this.primitiveType = primitiveType;
        this.nonNullAnnotation = nonNullAnnotation;
        this.nullableAnnotation = nullableAnnotation;
        generatedMethods = new HashSet<>();
        ServiceLoader<JanksonDelegate> delegateLoader = ServiceLoader.load(JanksonDelegate.class, JanksonContext.class.getClassLoader());
        delegates = new ArrayList<>();
        missingErrorMessages = new HashMap<>();
        for (JanksonDelegate delegate : delegateLoader)
            delegates.add(delegate);
        nestedDelegate = new NestedJanksonDelegate();
    }

    public void init(@NotNull ProcessingEnvironment processingEnv) {
        for (JanksonDelegate delegate : delegates)
            delegate.init(processingEnv);
        nestedDelegate.init(processingEnv);
    }

    public void appendRead(@NotNull VariableElement field, @NotNull CodeBlock.Builder codeBuilder) {
        for (JanksonDelegate delegate : delegates) {
            if (delegate.appendRead(this, field, codeBuilder))
                return;
        }
        nestedDelegate.appendRead(this, field, codeBuilder);
    }

    public void appendWrite(@NotNull VariableElement field, @NotNull CodeBlock.Builder codeBuilder) {
        for (JanksonDelegate delegate : delegates) {
            if (delegate.appendWrite(this, field, codeBuilder))
                return;
        }
        nestedDelegate.appendWrite(this, field, codeBuilder);
    }
}
