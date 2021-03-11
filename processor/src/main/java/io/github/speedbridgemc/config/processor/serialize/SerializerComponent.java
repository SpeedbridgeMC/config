package io.github.speedbridgemc.config.processor.serialize;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.processor.api.BaseComponentProvider;
import io.github.speedbridgemc.config.processor.api.ComponentContext;
import io.github.speedbridgemc.config.processor.api.ComponentProvider;
import io.github.speedbridgemc.config.processor.api.ParamUtils;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerContext;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.ServiceLoader;

@ApiStatus.Internal
@AutoService(ComponentProvider.class)
public final class SerializerComponent extends BaseComponentProvider {
    private HashMap<String, SerializerProvider> serializerProviders;

    public SerializerComponent() {
        super("speedbridge-config:serializer");
    }

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        serializerProviders = new HashMap<>();
        ServiceLoader<SerializerProvider> spLoader = ServiceLoader.load(SerializerProvider.class, SerializerComponent.class.getClassLoader());
        for (SerializerProvider provider : spLoader)
            serializerProviders.put(provider.getId(), provider);

        for (SerializerProvider componentProvider : serializerProviders.values())
            componentProvider.init(processingEnv);
    }

    @Override
    public void process(@NotNull String name, @NotNull TypeElement type,
                        @NotNull ImmutableList<VariableElement> fields,
                        @NotNull ComponentContext ctx, TypeSpec.@NotNull Builder classBuilder) {
        String providerId = ParamUtils.allOrNothing(ctx.params, "provider");
        if (providerId == null)
            providerId = "speedbridge-config:jankson";
        SerializerProvider provider = serializerProviders.get(providerId);
        if (provider == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Serializer: Unknown provider \"" + providerId + "\"", type);
            return;
        }
        // TODO properly require resolvePath/log methods
        classBuilder.addField(FieldSpec.builder(Path.class, "path", Modifier.PRIVATE, Modifier.FINAL)
                .initializer("resolvePath($S)", name)
                .build());
        String basePackage = ParamUtils.allOrNothing(ctx.params, "base_package");
        String[] options = ctx.params.get("options").toArray(new String[0]);
        TypeName configType = ctx.configType;
        ParameterSpec.Builder pathParamBuilder = ParameterSpec.builder(Path.class, "path");
        if (ctx.nonNullAnnotation != null)
            pathParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder readMethodBuilder = MethodSpec.methodBuilder("read")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(pathParamBuilder.build())
                .returns(configType)
                .addException(IOException.class);
        if (ctx.nonNullAnnotation != null)
            readMethodBuilder.addAnnotation(ctx.nonNullAnnotation);
        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(configType, "config");
        if (ctx.nonNullAnnotation != null)
            configParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder writeMethodBuilder = MethodSpec.methodBuilder("write")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(configParamBuilder.build())
                .addParameter(pathParamBuilder.build())
                .addException(IOException.class);
        SerializerContext sCtx = new SerializerContext(configType, basePackage, options,
                readMethodBuilder, writeMethodBuilder,
                ctx.nonNullAnnotation, ctx.nullableAnnotation);
        provider.process(name, type, fields, sCtx, classBuilder);
        classBuilder.addMethod(readMethodBuilder.build()).addMethod(writeMethodBuilder.build());
        ctx.loadMethodBuilder.addCode(CodeBlock.builder()
                .beginControlFlow("try")
                .addStatement("config = read(path)")
                .nextControlFlow("catch ($T e)", NoSuchFileException.class)
                .nextControlFlow("catch ($T e)", IOException.class)
                .addStatement("log($S + path + $S, e)", "Failed to read from config file at \"", "\"!")
                // TODO backup
                .endControlFlow()
                .beginControlFlow("if (config == null)")
                .addStatement("config = new $T()", configType)
                .addStatement("save()")
                .endControlFlow()
                .build());
        ctx.saveMethodBuilder.addCode(CodeBlock.builder()
                .beginControlFlow("try")
                .addStatement("write(config, path)")
                .nextControlFlow("catch ($T e)", IOException.class)
                .addStatement("log($S + path + $S, e)", "Failed to read from config file at \"", "\"!")
                .endControlFlow()
                .build());
    }
}
