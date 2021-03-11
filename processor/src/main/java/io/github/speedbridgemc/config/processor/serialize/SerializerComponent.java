package io.github.speedbridgemc.config.processor.serialize;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
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
                        @NotNull ImmutableSet<VariableElement> fields,
                        @NotNull ComponentContext ctx, TypeSpec.@NotNull Builder classBuilder) {
        String pathTemplate = ParamUtils.allOrNothing(ctx.params, "pathTemplate");
        if (pathTemplate == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Serializer: Missing required parameter \"pathTemplate\"", type);
            return;
        }
        classBuilder.addField(FieldSpec.builder(Path.class, "path", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer(pathTemplate, name)
                .build());
        String logTemplate = ParamUtils.allOrNothing(ctx.params, "logTemplate");
        if (logTemplate == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Serializer: Missing required parameter \"logTemplate\"", type);
            return;
        }
        String serializer = ParamUtils.allOrNothing(ctx.params, "serializer");
        if (serializer == null)
            serializer = "speedbridge-config:jankson";
        SerializerProvider provider = serializerProviders.get(serializer);
        if (provider == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Serializer: Unknown provider \"" + serializer + "\"", type);
            return;
        }
        String basePackage = ParamUtils.allOrNothing(ctx.params, "basePackage");
        String mode = ParamUtils.allOrNothing(ctx.params, "mode");
        String[] options = ctx.params.get("options").toArray(new String[0]);
        TypeName configType = ctx.configType;
        MethodSpec.Builder readMethodBuilder = MethodSpec.methodBuilder("read")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(Path.class, "path")
                .returns(configType)
                .addException(IOException.class);
        MethodSpec.Builder writeMethodBuilder = MethodSpec.methodBuilder("write")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(configType, "config")
                .addParameter(Path.class, "path")
                .addException(IOException.class);
        SerializerContext ctx2 = new SerializerContext(configType, basePackage, mode, options, readMethodBuilder, writeMethodBuilder);
        provider.process(name, type, fields, ctx2, classBuilder);
        classBuilder.addMethod(readMethodBuilder.build()).addMethod(writeMethodBuilder.build());
        ctx.loadMethodBuilder.addCode(CodeBlock.builder()
                .beginControlFlow("try")
                .addStatement("config = read(path)")
                .nextControlFlow("catch ($T e)", NoSuchFileException.class)
                .nextControlFlow("catch ($T e)", IOException.class)
                .addStatement(logTemplate, "Failed to read from config file!", "e")
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
                .addStatement(logTemplate, "Failed to write to config file!", "e")
                .endControlFlow()
                .build());
    }
}
