package io.github.speedbridgemc.config.processor.serialize;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerContext;
import io.github.speedbridgemc.config.processor.serialize.api.BaseSerializerProvider;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.io.*;
import java.nio.file.Files;
import java.util.Locale;

@ApiStatus.Internal
@AutoService(SerializerProvider.class)
public final class GsonSerializerProvider extends BaseSerializerProvider {
    public GsonSerializerProvider() {
        super("speedbridge-config:gson");
    }

    @Override
    public void process(@NotNull String name, @NotNull TypeElement type,
                        @NotNull ImmutableSet<VariableElement> fields,
                        @NotNull SerializerContext ctx, TypeSpec.@NotNull Builder classBuilder) {
        String basePackage = "com.google.gson";
        if (ctx.basePackage != null)
            basePackage = ctx.basePackage;
        String mode = "simple";
        if (ctx.mode != null)
            mode = ctx.mode.toLowerCase(Locale.ROOT);
        if ("simple".equals(mode)) {
            TypeName configType = ctx.configType;
            TypeName gsonType = TypeUtils.getTypeName(processingEnv, basePackage + ".Gson");
            TypeName syntaxExceptionType = TypeUtils.getTypeName(processingEnv, basePackage + ".SyntaxException");
            classBuilder
                    .addField(FieldSpec.builder(gsonType, "GSON", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                            .initializer("new $T()", gsonType)
                            .build());
            ctx.readMethodBuilder.addCode(CodeBlock.builder()
                    .beginControlFlow("try ($3T in = new $3T(new $2T($1T.newInputStream(path))))",
                            Files.class, InputStreamReader.class, BufferedReader.class)
                    .addStatement("return GSON.fromJson(in, $T.class)", configType)
                    .nextControlFlow("catch ($T e)", syntaxExceptionType)
                    .addStatement("throw new $T($S + path + $S, e)", IOException.class, "Failed to parse config file at \"", "\" to JSON!")
                    .endControlFlow()
                    .build());
            ctx.writeMethodBuilder.addCode(CodeBlock.builder()
                    .beginControlFlow("try ($T out = $T.newOutputStream(path))",
                            OutputStream.class, Files.class)
                    .addStatement("GSON.toJson(config, out)")
                    .endControlFlow()
                    .build());
        } else
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Serializer: Unknown mode \"" + mode + "\"", type);
    }
}
