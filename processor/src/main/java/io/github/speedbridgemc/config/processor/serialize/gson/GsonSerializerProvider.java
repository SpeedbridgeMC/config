package io.github.speedbridgemc.config.processor.serialize.gson;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.SerializerComponentProvider;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerContext;
import io.github.speedbridgemc.config.processor.serialize.api.BaseSerializerProvider;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerProvider;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

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
        String basePackage = "com.google.gson.stream";
        if (ctx.basePackage != null)
            basePackage = ctx.basePackage;
        TypeName configType = ctx.configType;
        TypeName readerType = TypeUtils.getTypeName(processingEnv, basePackage + ".JsonReader");
        TypeName writerType = TypeUtils.getTypeName(processingEnv, basePackage + ".JsonWriter");
        TypeName tokenType = TypeUtils.getTypeName(processingEnv, basePackage + ".JsonToken");
        TypeName malformedExceptionType = TypeUtils.getTypeName(processingEnv, basePackage + ".MalformedJsonException");
        GsonContext gCtx = new GsonContext(classBuilder, readerType, writerType, tokenType,
                ctx.nonNullAnnotation, ctx.nullableAnnotation);
        gCtx.init(processingEnv);
        SerializerComponentProvider.getMissingErrorMessages(processingEnv, fields, ctx.defaultMissingErrorMessage, gCtx.missingErrorMessages);
        generateGotFlags(gCtx, fields);
        String objName = "config";
        ctx.readMethodBuilder.addCode("$1T $2L = new $1T();\n", configType, objName);
        CodeBlock.Builder codeBuilder = generateGotFlagDecls(gCtx);
        ctx.readMethodBuilder.addCode(codeBuilder.build());
        ctx.readMethodBuilder.addCode(CodeBlock.builder()
                .beginControlFlow("try ($4T $5L = new $4T(new $3T(new $2T($1T.newInputStream(path)))))",
                        Files.class, InputStreamReader.class, BufferedReader.class, readerType, gCtx.readerName)
                .addStatement("$L.beginObject()", gCtx.readerName)
                .beginControlFlow("while ($L.hasNext())", gCtx.readerName)
                .addStatement("$T token = $L.peek()", tokenType, gCtx.readerName)
                .beginControlFlow("if (token == $T.NAME)", tokenType)
                .addStatement("String name = $L.nextName()", gCtx.readerName)
                .build());
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            codeBuilder = CodeBlock.builder()
                    .beginControlFlow("if ($S.equals(name))", fieldName);
            gCtx.fieldElement = field;
            gCtx.appendRead(field.asType(), fieldName, objName + "." + fieldName, codeBuilder);
            ctx.readMethodBuilder.addCode(codeBuilder
                    .addStatement("continue")
                    .endControlFlow()
                    .build());
        }
        ctx.readMethodBuilder.addCode(CodeBlock.builder()
                .endControlFlow()
                .addStatement("$L.skipValue()", gCtx.readerName)
                .endControlFlow()
                .addStatement("$L.endObject()", gCtx.readerName)
                .nextControlFlow("catch ($T e)", malformedExceptionType)
                .addStatement("throw new $T($S + path + $S, e)", IOException.class, "Failed to parse config file at \"", "\" to JSON!")
                .endControlFlow()
                .build());
        ctx.readMethodBuilder.addCode(generateGotFlagChecks(gCtx)
                .addStatement("return $L", objName)
                .build());
        ctx.writeMethodBuilder.addCode(CodeBlock.builder()
                .beginControlFlow("try ($4T $5L = new $4T(new $3T(new $2T($1T.newOutputStream(path)))))",
                        Files.class, OutputStreamWriter.class, BufferedWriter.class, writerType, gCtx.writerName)
                .addStatement("$L.beginObject()", gCtx.writerName)
                .build());
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            codeBuilder = CodeBlock.builder()
                    .addStatement("$L.name($S)", gCtx.writerName, fieldName);
            gCtx.fieldElement = field;
            gCtx.appendWrite(field.asType(), fieldName, objName + "." + fieldName, codeBuilder);
            ctx.writeMethodBuilder.addCode(codeBuilder.build());
        }
        ctx.writeMethodBuilder.addCode(CodeBlock.builder()
                .addStatement("$L.endObject()", gCtx.writerName)
                .endControlFlow()
                .build());
    }

    public static void generateGotFlags(@NotNull GsonContext gCtx, @NotNull List<@NotNull VariableElement> fields) {
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            if (gCtx.missingErrorMessages.get(fieldName) != null)
                gCtx.gotFlags.put(fieldName, "got" + StringUtils.titleCase(fieldName));
        }
    }

    public static @NotNull CodeBlock.Builder generateGotFlagDecls(@NotNull GsonContext gCtx) {
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        for (String gotFlagVar : gCtx.gotFlags.values())
            codeBuilder.addStatement("boolean $L = false", gotFlagVar);
        return codeBuilder;
    }

    public static @NotNull CodeBlock.Builder generateGotFlagChecks(@NotNull GsonContext gCtx) {
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        for (Map.Entry<String, String> entry : gCtx.gotFlags.entrySet()) {
            String missingErrorMessage = gCtx.missingErrorMessages.get(entry.getKey());
            if (missingErrorMessage == null)
                continue;
            codeBuilder.beginControlFlow("if (!$L)", entry.getValue())
                    .addStatement("throw new $T($S)", IOException.class, String.format(missingErrorMessage, entry.getKey()))
                    .endControlFlow();
        }
        return codeBuilder;
    }
}
