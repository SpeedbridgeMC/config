package io.github.speedbridgemc.config.processor.serialize.jankson;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.api.BaseSerializerProvider;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerContext;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerMode;
import io.github.speedbridgemc.config.processor.serialize.api.SerializerProvider;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Locale;
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
        String mode = SerializerMode.IMPLICIT;
        if (ctx.mode != null)
            mode = ctx.mode.toLowerCase(Locale.ROOT);
        switch (mode) {
        case SerializerMode.IMPLICIT:
            HashMap<String, Boolean> grammarMap = createGrammarMap(ctx.options);
            TypeName configType = ctx.configType;
            TypeName janksonType = TypeUtils.getTypeName(processingEnv, basePackage + ".Jankson");
            TypeName grammarType = TypeUtils.getTypeName(processingEnv, basePackage + ".JsonGrammar");
            TypeName objectType = TypeUtils.getTypeName(processingEnv, basePackage + ".JsonObject");
            TypeName syntaxErrorType = TypeUtils.getTypeName(processingEnv, basePackage + ".api.SyntaxError");
            classBuilder
                    .addField(FieldSpec.builder(janksonType, "JANKSON", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$T.builder()$L.build()", janksonType, createJanksonCode(grammarMap))
                            .build())
                    .addField(FieldSpec.builder(grammarType, "GRAMMAR", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                            .initializer("$T.builder()$L.build()", grammarType, createGrammarCode(grammarMap))
                            .build());
            ctx.readMethodBuilder.addCode(CodeBlock.builder()
                    .beginControlFlow("try ($T in = $T.newInputStream(path))", InputStream.class, Files.class)
                    .addStatement("$T obj = JANKSON.load(in)", objectType)
                    .addStatement("return JANKSON.fromJson(obj, $T.class)", configType)
                    .nextControlFlow("catch ($T e)", syntaxErrorType)
                    .addStatement("throw new $T($S + path + $S, e)", IOException.class, "Failed to parse config file at \"", "\" to JSON!")
                    .endControlFlow()
                    .build());
            ctx.writeMethodBuilder.addCode(CodeBlock.builder()
                    .addStatement("String json = JANKSON.toJson(config).toJson(GRAMMAR)")
                    .beginControlFlow("try ($3T out = new $3T(new $2T($1T.newOutputStream(path))))",
                            Files.class, OutputStreamWriter.class, BufferedWriter.class)
                    .addStatement("out.write(json)")
                    .endControlFlow()
                    .build());
            break;
        case SerializerMode.EXPLICIT_ADAPTER:   // TODO
        case SerializerMode.EXPLICIT_READWRITE: // TODO
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Serializer: Mode \"" + mode + "\" is NYI", type);
            break;
        default:
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Serializer: Unknown mode \"" + mode + "\"", type);
            break;
        }
    }

    private HashMap<String, Boolean> createGrammarMap(String[] options) {
        HashMap<String, Boolean> grammarMap = new HashMap<>();
        grammarMap.put("withComments", true);
        grammarMap.put("printWhitespace", true);
        grammarMap.put("printCommas", true);
        grammarMap.put("printTrailingCommas", true);
        grammarMap.put("bareSpecialNumerics", true);
        grammarMap.put("bareRootObject", false);
        grammarMap.put("printUnquotedKeys", true);
        for (String option : options) {
            if (option.isEmpty())
                continue;
            char first = option.charAt(0);
            boolean enabled = first != '-';
            if (!enabled || first == '+')
                option = option.substring(1);
            if (!grammarMap.containsKey(option))
                continue;
            grammarMap.put(option, enabled);
        }
        return grammarMap;
    }

    private String createJanksonCode(HashMap<String, Boolean> grammarMap) {
        if (grammarMap.get("bareRootObject"))
            return ".allowBareRootObject()";
        return "";
    }

    private String createGrammarCode(HashMap<String, Boolean> grammarMap) {
        return "." + grammarMap.entrySet().stream()
                .map(entry -> String.format("%s(%b)", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("."));
    }
}
