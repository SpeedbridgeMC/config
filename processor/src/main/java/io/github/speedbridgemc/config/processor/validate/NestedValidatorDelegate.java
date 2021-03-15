package io.github.speedbridgemc.config.processor.validate;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.EnforceMode;
import io.github.speedbridgemc.config.EnforceNotNull;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.validate.api.BaseValidatorDelegate;
import io.github.speedbridgemc.config.processor.validate.api.ValidatorContext;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.List;

public final class NestedValidatorDelegate extends BaseValidatorDelegate {
    @Override
    public boolean appendCheck(@NotNull ValidatorContext ctx, @NotNull TypeMirror type, @NotNull String src, @NotNull String description, CodeBlock.@NotNull Builder codeBuilder) {
        Element typeElementRaw = processingEnv.getTypeUtils().asElement(type);
        if (typeElementRaw == null || typeElementRaw.getKind() != ElementKind.CLASS)
            return false;
        TypeElement typeElement = (TypeElement) typeElementRaw;
        TypeName typeName = TypeName.get(type);
        String methodName = generateCheckMethod(ctx, typeName, typeElement);
        VariableElement effectiveFieldElement = ctx.getEffectiveFieldElement();
        EnforceNotNull enforceNotNull = effectiveFieldElement.getAnnotation(EnforceNotNull.class);
        if (enforceNotNull != null && enforceNotNull.value() != EnforceMode.IGNORE) {
            codeBuilder
                    .beginControlFlow("if ($L == null)", src)
                    .addStatement("throw new $T($S)",
                            IllegalArgumentException.class, String.format("\"%s\" is null!", description))
                    .endControlFlow();
        }
        codeBuilder.addStatement("$L($L)", methodName, src);
        return true;
    }

    private @NotNull String generateCheckMethod(@NotNull ValidatorContext ctx, @NotNull TypeName typeName, @NotNull TypeElement typeElement) {
        String methodName = "check" + typeElement.getSimpleName().toString();
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        List<VariableElement> fields = TypeUtils.getFieldsIn(typeElement);
        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(typeName, ctx.configName);
        if (ctx.nonNullAnnotation != null)
            configParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addParameter(configParamBuilder.build())
                .addException(IllegalArgumentException.class);
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            ctx.fieldElement = field;
            ctx.appendCheck(field.asType(), ctx.configName + "." + fieldName, ctx.configName + "." + fieldName, codeBuilder);
        }
        ctx.classBuilder.addMethod(methodBuilder.addCode(codeBuilder.build()).build());
        return methodName;
    }
}
