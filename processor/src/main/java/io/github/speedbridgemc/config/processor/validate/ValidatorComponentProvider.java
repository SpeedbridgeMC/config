package io.github.speedbridgemc.config.processor.validate;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.LogLevel;
import io.github.speedbridgemc.config.processor.api.BaseComponentProvider;
import io.github.speedbridgemc.config.processor.api.ComponentContext;
import io.github.speedbridgemc.config.processor.api.ComponentProvider;
import io.github.speedbridgemc.config.processor.validate.api.ErrorDelegate;
import io.github.speedbridgemc.config.processor.validate.api.ValidatorContext;
import io.github.speedbridgemc.config.serialize.PathUtils;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

@AutoService(ComponentProvider.class)
public final class ValidatorComponentProvider extends BaseComponentProvider {
    public ValidatorComponentProvider() {
        super("speedbridge-config:validator");
    }

    @Override
    public void process(@NotNull String name, @NotNull TypeElement type, @NotNull ImmutableList<@NotNull VariableElement> fields, @NotNull ComponentContext ctx, TypeSpec.@NotNull Builder classBuilder) {
        HashMap<String, Boolean> options = new HashMap<>();
        parseOptions(ctx.params.get("options").toArray(new String[0]), options);
        TypeName configType = ctx.configName;
        String configName = "config";

        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(configType, configName);
        if (ctx.nonNullAnnotation != null)
            configParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder checkMethodBuilder = MethodSpec.methodBuilder("validate")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(configParamBuilder.build())
                .addException(IllegalArgumentException.class);
        CodeBlock.Builder codeBuilder = CodeBlock.builder();

        ValidatorContext vCtx = new ValidatorContext(ctx, configType, options, classBuilder, ctx.nonNullAnnotation, ctx.nullableAnnotation);
        vCtx.init(processingEnv);
        vCtx.configName = configName;

        classBuilder.addField(FieldSpec.builder(ctx.configName, "DEFAULTS",
                Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("new $T()", ctx.configName)
                .build());

        vCtx.enclosingElement = type;
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            vCtx.element = field;
            vCtx.defaultSrc = "DEFAULTS." + fieldName;
            vCtx.appendCheck(field.asType(), configName + "." + fieldName, ErrorDelegate.simple(fieldName), codeBuilder);
        }

        checkMethodBuilder.addCode(codeBuilder.build());
        classBuilder.addMethod(checkMethodBuilder.build());

        CodeBlock.Builder loadCodeBuilder = CodeBlock.builder()
                .beginControlFlow("try")
                .addStatement("validate(config)")
                .nextControlFlow("catch ($T e)", IllegalArgumentException.class)
                .addStatement("log($T.ERROR, $S + path + $S, e)",
                        LogLevel.class, "Config file at \"", "\" is invalid!");
        boolean crashOnFail = options.getOrDefault("crashOnFail", false);
        if (options.getOrDefault("backupOnFail", true)) {
            loadCodeBuilder
                    .addStatement("$T backupPath = $T.resolveTimestampedSibling(path, $S)",
                            Path.class, PathUtils.class, "BACKUP")
                    .addStatement("boolean backupSuccess = true");
            loadCodeBuilder
                    .beginControlFlow("try")
                    .addStatement("$T.move(path, backupPath, $T.REPLACE_EXISTING)",
                            Files.class, StandardCopyOption.class)
                    .nextControlFlow("catch ($T be)", IOException.class)
                    .addStatement("log($T.ERROR, $S + backupPath + $S, be)",
                            LogLevel.class, "Failed to back up config file to \"", "\"!")
                    .addStatement("backupSuccess = false")
                    .endControlFlow();
            if (crashOnFail) {
                loadCodeBuilder
                        .beginControlFlow("if (backupSuccess)")
                        .addStatement("reset()")
                        .addStatement("save()")
                        .addStatement("throw new $T($S + path + $S + backupPath + $S, e)",
                                RuntimeException.class, "Config file \"",
                                "\" is invalid! File has been replaced with default values, with the original backed up at \"", "\"")
                        .nextControlFlow("else")
                        .addStatement("throw new $T($S + path + $S, e)",
                                RuntimeException.class, "Config file \"", "\" is invalid!")
                        .endControlFlow();
            } else
                loadCodeBuilder
                        .beginControlFlow("if (backupSuccess)")
                        .addStatement("log($T.INFO, $S + backupPath + $S, null)",
                                LogLevel.class, "Backed up config file to \"", "\"")
                        .endControlFlow()
                        .addStatement("log($T.WARN, $S, null)",
                                LogLevel.class, "Resetting to default config values")
                        .addStatement("reset()");
        } else if (crashOnFail) {
            loadCodeBuilder.addStatement("throw new $T($S + path + $S, e)",
                    RuntimeException.class, "Config file \"", "\" is invalid!");
        }
        loadCodeBuilder.endControlFlow();
        ctx.loadMethodBuilder.addCode(loadCodeBuilder.build());
    }
}
