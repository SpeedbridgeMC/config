package io.github.speedbridgemc.config.processor.serialize.custom;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeSpec;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.api.BaseSerializerProvider;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerContext;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerProvider;
import io.github.speedbridgemc.config.serialize.Deserializer;
import io.github.speedbridgemc.config.serialize.Serializer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

@AutoService(SerializerProvider.class)
public final class CustomSerializerProvider extends BaseSerializerProvider {
    private TypeMirror inputStreamTM, outputStreamTM;

    public CustomSerializerProvider() {
        super("speedbridge-config:custom");
    }

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        inputStreamTM = TypeUtils.getTypeMirror(processingEnv, InputStream.class.getCanonicalName());
        outputStreamTM = TypeUtils.getTypeMirror(processingEnv, OutputStream.class.getCanonicalName());
    }

    @Override
    public void process(@NotNull String name, @NotNull TypeElement type, @NotNull ImmutableList<VariableElement> fields, @NotNull SerializerContext ctx, TypeSpec.@NotNull Builder classBuilder) {
        ExecutableElement serializeMethod = null, deserializeMethod = null;
        for (ExecutableElement method : ElementFilter.methodsIn(type.getEnclosedElements())) {
            if (method.getAnnotation(Serializer.class) != null) {
                Set<Modifier> mods = method.getModifiers();
                List<? extends VariableElement> params = method.getParameters();
                if (mods.contains(Modifier.PUBLIC)
                        && mods.contains(Modifier.STATIC)
                        && params.size() == 2
                        && types.isSameType(type.asType(), params.get(0).asType())
                        && types.isSameType(outputStreamTM, params.get(1).asType()))
                    serializeMethod = method;
                else
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Method marked with @Serialize is invalid: should be public and static " +
                                    "and take (" + TypeUtils.getSimpleName(ctx.configType) + ", OutputStream) as only parameters",
                            method);
            }
            if (method.getAnnotation(Deserializer.class) != null) {
                Set<Modifier> mods = method.getModifiers();
                List<? extends VariableElement> params = method.getParameters();
                if (mods.contains(Modifier.PUBLIC)
                        && mods.contains(Modifier.STATIC)
                        && types.isSameType(type.asType(), method.getReturnType())
                        && params.size() == 1
                        && types.isSameType(inputStreamTM, params.get(0).asType()))
                    deserializeMethod = method;
                else
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Method marked with @Deserialize is invalid: should be public and static, " +
                                    "take InputStream as only parameter and return " + TypeUtils.getSimpleName(ctx.configType),
                            method);
            }
            if (serializeMethod != null && deserializeMethod != null)
                break;
        }
        if (serializeMethod == null)
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Missing @Serialize method for custom serializer",
                    type);
        if (deserializeMethod == null)
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Missing @Deserialize method for custom serializer",
                    type);
        if (serializeMethod == null || deserializeMethod == null)
            return;
        ctx.readMethodBuilder
                .beginControlFlow("try ($T in = $T.newInputStream(path))", InputStream.class, Files.class)
                .addStatement("return $T.$L(in)", ctx.configType, deserializeMethod.getSimpleName().toString())
                .endControlFlow();
        ctx.writeMethodBuilder
                .beginControlFlow("try ($T out = $T.newOutputStream(path))", OutputStream.class, Files.class)
                .addStatement("$T.$L(config, out)", ctx.configType, serializeMethod.getSimpleName().toString())
                .endControlFlow();
    }
}
