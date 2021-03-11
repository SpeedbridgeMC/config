package io.github.speedbridgemc.config.processor.serialize.gson;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerContext;
import io.github.speedbridgemc.config.processor.serialize.api.BaseSerializerProvider;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerMode;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerProvider;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonRWContext;
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
                        @NotNull ImmutableList<VariableElement> fields,
                        @NotNull SerializerContext ctx, TypeSpec.@NotNull Builder classBuilder) {
        String basePackage = null;
        if (ctx.basePackage != null)
            basePackage = ctx.basePackage;
        String mode = SerializerMode.EXPLICIT_READWRITE;
        if (ctx.mode != null)
            mode = ctx.mode.toLowerCase(Locale.ROOT);
        TypeName configType = ctx.configType;
        switch (mode) {
        case SerializerMode.IMPLICIT:
            if (basePackage == null)
                basePackage = "com.google.gson";
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
            break;
        case SerializerMode.EXPLICIT_ADAPTER: // TODO
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Serializer: Mode \"" + mode + "\" is NYI", type);
            break;
        case SerializerMode.EXPLICIT_READWRITE:
            if (basePackage == null)
                basePackage = "com.google.gson.stream";
            TypeName readerType = TypeUtils.getTypeName(processingEnv, basePackage + ".JsonReader");
            TypeName writerType = TypeUtils.getTypeName(processingEnv, basePackage + ".JsonWriter");
            TypeName tokenType = TypeUtils.getTypeName(processingEnv, basePackage + ".JsonToken");
            GsonRWContext rwCtx = new GsonRWContext(classBuilder, readerType, writerType, tokenType,
                    ctx.nonNullAnnotation, ctx.nullableAnnotation);
            rwCtx.init(processingEnv);
            TypeName malformedExceptionType = TypeUtils.getTypeName(processingEnv, basePackage + ".MalformedJsonException");
            ctx.readMethodBuilder.addCode(CodeBlock.builder()
                    .addStatement("$1T config = new $1T()", configType)
                    .beginControlFlow("try ($4T reader = new $4T(new $3T(new $2T($1T.newInputStream(path)))))",
                            Files.class, InputStreamReader.class, BufferedReader.class, readerType)
                    .addStatement("reader.beginObject()")
                    .beginControlFlow("while (reader.hasNext())")
                    .addStatement("$T token = reader.peek()", tokenType)
                    .beginControlFlow("if (token == $T.NAME)", tokenType)
                    .addStatement("String name = reader.nextName()")
                    .build());
            for (VariableElement field : fields) {
                CodeBlock.Builder codeBuilder = CodeBlock.builder()
                        .beginControlFlow("if ($S.equals(name))", field.getSimpleName().toString());
                rwCtx.appendRead(field, codeBuilder);
                ctx.readMethodBuilder.addCode(codeBuilder
                        .addStatement("continue")
                        .endControlFlow()
                        .build());
            }
            ctx.readMethodBuilder.addCode(CodeBlock.builder()
                    .endControlFlow()
                    .addStatement("reader.skipValue()")
                    .endControlFlow()
                    .addStatement("reader.endObject()")
                    .nextControlFlow("catch ($T e)", malformedExceptionType)
                    .addStatement("throw new $T($S + path + $S, e)", IOException.class, "Failed to parse config file at \"", "\" to JSON!")
                    .endControlFlow()
                    .addStatement("return config")
                    .build());
            break;
        default:
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Serializer: Unknown mode \"" + mode + "\"", type);
            break;
        }
    }
}
