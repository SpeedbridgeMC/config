package io.github.speedbridgemc.config.processor.serialize.jankson;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.SerializerComponentProvider;
import io.github.speedbridgemc.config.processor.serialize.api.BaseSerializerProvider;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerContext;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerProvider;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.JanksonContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApiStatus.Internal
@AutoService(SerializerProvider.class)
public final class JanksonSerializerProvider extends BaseSerializerProvider {
    public JanksonSerializerProvider() {
        super("speedbridge-config:jankson");
    }

    @Override
    public void process(@NotNull String name, @NotNull TypeElement type, @NotNull ImmutableList<VariableElement> fields,
                        @NotNull SerializerContext ctx, TypeSpec.@NotNull Builder classBuilder) {
        String basePackage = "blue.endless.jankson";
        if (ctx.basePackage != null)
            basePackage = ctx.basePackage;
        HashMap<String, Boolean> grammarMap = createGrammarMap(ctx.options);
        TypeName configType = ctx.configType;
        TypeName janksonType = TypeUtils.getTypeName(processingEnv, basePackage + ".Jankson");
        TypeName grammarType = TypeUtils.getTypeName(processingEnv, basePackage + ".JsonGrammar");
        TypeName elementType = TypeUtils.getTypeName(processingEnv, basePackage + ".JsonElement");
        TypeName objectType = TypeUtils.getTypeName(processingEnv, basePackage + ".JsonObject");
        TypeName arrayType = TypeUtils.getTypeName(processingEnv, basePackage + ".JsonArray");
        TypeName primitiveType = TypeUtils.getTypeName(processingEnv, basePackage + ".JsonPrimitive");
        TypeName nullType = TypeUtils.getTypeName(processingEnv, basePackage + ".JsonNull");
        TypeName syntaxErrorType = TypeUtils.getTypeName(processingEnv, basePackage + ".api.SyntaxError");
        classBuilder
                .addField(FieldSpec.builder(janksonType, "JANKSON", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.builder().build()", janksonType)
                        .build())
                .addField(FieldSpec.builder(grammarType, "GRAMMAR", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.builder()$L.build()", grammarType, createGrammarCode(grammarMap))
                        .build());
        JanksonContext jCtx = new JanksonContext(classBuilder, elementType, objectType, primitiveType, arrayType, nullType, ctx.nonNullAnnotation, ctx.nullableAnnotation);
        jCtx.init(processingEnv);
        jCtx.enclosingElement = type;
        String configName = "config";
        ctx.readMethodBuilder.addCode(CodeBlock.builder()
                .addStatement("$1T $2L = new $1T()", configType, configName)
                .beginControlFlow("try ($T in = $T.newInputStream(path))", InputStream.class, Files.class)
                .addStatement("$T $L = JANKSON.load(in)", objectType, jCtx.objectName)
                .build());
        ctx.readMethodBuilder.addCode(generateFieldChecks(processingEnv, ctx.defaultMissingErrorMessage, fields, jCtx.objectName)
                .build());
        ctx.readMethodBuilder.addCode(CodeBlock.builder()
                .addStatement("$T $L", primitiveType, jCtx.primitiveName)
                .addStatement("$T $L", arrayType, jCtx.arrayName)
                .addStatement("$T $L", elementType, jCtx.elementName)
                .build());
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            String serializedName = SerializerComponentProvider.getSerializedName(field);
            jCtx.element = field;
            codeBuilder.add(generateGet(jCtx, field).build());
            jCtx.appendRead(field.asType(), serializedName, configName + "." + fieldName, codeBuilder);
        }
        ctx.readMethodBuilder.addCode(codeBuilder.build());
        ctx.readMethodBuilder.addCode(CodeBlock.builder()
                .nextControlFlow("catch ($T e)", syntaxErrorType)
                .addStatement("throw new $T($S + path + $S, e)", IOException.class, "Failed to parse config file at \"", "\" to JSON!")
                .endControlFlow()
                .addStatement("return $L", configName)
                .build());
        ctx.writeMethodBuilder.addCode(CodeBlock.builder()
                .addStatement("$1T $2L = new $1T()", objectType, jCtx.objectName)
                .addStatement("$T $L", elementType, jCtx.elementName)
                .build());
        codeBuilder = CodeBlock.builder();
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            String serializedName = SerializerComponentProvider.getSerializedName(field);
            jCtx.element = field;
            jCtx.appendWrite(field.asType(), serializedName, configName + "." + fieldName, codeBuilder);
            codeBuilder.addStatement(generatePut(jCtx, field).build());
        }
        ctx.writeMethodBuilder.addCode(codeBuilder.build());
        ctx.writeMethodBuilder.addCode(CodeBlock.builder()
                .addStatement("String json = $L.toJson(GRAMMAR)", jCtx.objectName)
                .beginControlFlow("try ($3T out = new $3T(new $2T($1T.newOutputStream(path))))",
                        Files.class, OutputStreamWriter.class, BufferedWriter.class)
                .addStatement("out.write(json)")
                .endControlFlow()
                .build());
    }

    private HashMap<String, Boolean> createGrammarMap(Map<String, Boolean> src) {
        HashMap<String, Boolean> grammarMap = new HashMap<>();
        grammarMap.put("withComments", true);
        grammarMap.put("printWhitespace", true);
        grammarMap.put("printCommas", true);
        grammarMap.put("printTrailingCommas", true);
        grammarMap.put("bareSpecialNumerics", true);
        grammarMap.put("bareRootObject", false);
        grammarMap.put("printUnquotedKeys", true);
        for (Map.Entry<String, Boolean> entry : src.entrySet()) {
            if (grammarMap.containsKey(entry.getKey()))
                grammarMap.put(entry.getKey(), entry.getValue());
        }
        return grammarMap;
    }

    private String createGrammarCode(HashMap<String, Boolean> grammarMap) {
        return "." + grammarMap.entrySet().stream()
                .map(entry -> String.format("%s(%b)", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("."));
    }

    public static @NotNull CodeBlock.Builder generateFieldChecks(@NotNull ProcessingEnvironment processingEnv,
                                                                 @Nullable String defaultMissingErrorMessage,
                                                                 @NotNull List<VariableElement> fields,
                                                                 @NotNull String objectName) {
        HashMap<String, String> missingErrorMessages = new HashMap<>();
        SerializerComponentProvider.getMissingErrorMessages(processingEnv, fields, defaultMissingErrorMessage, missingErrorMessages);
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        for (VariableElement field : fields) {
            String serializedName = SerializerComponentProvider.getSerializedName(field);
            String missingErrorMessage = missingErrorMessages.get(serializedName);
            if (missingErrorMessage == null)
                continue;
            codeBuilder
                    .add("if (!$L.containsKey($S)", objectName, serializedName);
            for (String alias : SerializerComponentProvider.getSerializedAliases(field))
                codeBuilder
                        .add(" && !$L.containsKey($S)", objectName, alias);
            codeBuilder
                    .beginControlFlow(")")
                    .addStatement("throw new $T($S)", IOException.class, String.format(missingErrorMessage, serializedName))
                    .endControlFlow();
        }
        return codeBuilder;
    }

    public static @NotNull CodeBlock.Builder generateGet(@NotNull JanksonContext ctx, @NotNull VariableElement field) {
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        String serializedName = SerializerComponentProvider.getSerializedName(field);
        codeBuilder.addStatement("$L = $L.get($S)", ctx.elementName, ctx.objectName, serializedName);
        for (String alias : SerializerComponentProvider.getSerializedAliases(field))
            codeBuilder
                    .beginControlFlow("if ($L == null)", ctx.elementName)
                    .addStatement("$L = $L.get($S)", ctx.elementName, ctx.objectName, alias)
                    .endControlFlow();
        return codeBuilder;
    }

    public static @NotNull CodeBlock.Builder generatePut(@NotNull JanksonContext ctx, @NotNull VariableElement field) {
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        String serializedName = SerializerComponentProvider.getSerializedName(field);
        codeBuilder.add("$L.put($S, $L)", ctx.objectName, serializedName, ctx.elementName);
        return codeBuilder;
    }
}
