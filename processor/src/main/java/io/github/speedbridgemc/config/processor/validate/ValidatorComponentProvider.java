package io.github.speedbridgemc.config.processor.validate;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.processor.api.BaseComponentProvider;
import io.github.speedbridgemc.config.processor.api.ComponentContext;
import io.github.speedbridgemc.config.processor.api.ComponentProvider;
import io.github.speedbridgemc.config.processor.validate.api.ValidatorContext;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

@AutoService(ComponentProvider.class)
public final class ValidatorComponentProvider extends BaseComponentProvider {
    public ValidatorComponentProvider() {
        super("speedbridge-config:validator");
    }

    @Override
    public void process(@NotNull String name, @NotNull TypeElement type, @NotNull ImmutableList<@NotNull VariableElement> fields, @NotNull ComponentContext ctx, TypeSpec.@NotNull Builder classBuilder) {
        String[] options = ctx.params.get("options").toArray(new String[0]);
        TypeName configType = ctx.configType;
        String configName = "config";

        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(configType, configName);
        if (ctx.nonNullAnnotation != null)
            configParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder checkMethodBuilder = MethodSpec.methodBuilder("validate")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(configParamBuilder.build())
                .addException(IllegalArgumentException.class);
        CodeBlock.Builder codeBuilder = CodeBlock.builder();

        ValidatorContext vCtx = new ValidatorContext(configType, options, classBuilder, ctx.nonNullAnnotation, ctx.nullableAnnotation);
        vCtx.init(processingEnv);
        vCtx.configName = configName;
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            vCtx.fieldElement = field;
            vCtx.appendCheck(field.asType(), configName + "." + fieldName, fieldName, codeBuilder);
        }

        checkMethodBuilder.addCode(codeBuilder.build());
        classBuilder.addMethod(checkMethodBuilder.build());
        ctx.loadMethodBuilder.addCode(CodeBlock.builder()
                .beginControlFlow("try")
                .addStatement("validate(config)")
                .nextControlFlow("catch ($T e)", IllegalArgumentException.class)
                .addStatement("log($S, e)", "Failed to validate config! Loading default values")
                .addStatement("reset()")
                .endControlFlow()
                .build());
    }
}
