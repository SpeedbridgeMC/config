package io.github.speedbridgemc.config.processor.validate;

import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.EnforceMode;
import io.github.speedbridgemc.config.EnforceNotNull;
import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.validate.api.BaseValidatorDelegate;
import io.github.speedbridgemc.config.processor.validate.api.ErrorDelegate;
import io.github.speedbridgemc.config.processor.validate.api.ValidatorContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Locale;

public final class NestedValidatorDelegate extends BaseValidatorDelegate {
    @Override
    public boolean appendCheck(@NotNull ValidatorContext ctx, @NotNull TypeMirror type, @NotNull String src, @NotNull ErrorDelegate errDelegate, CodeBlock.@NotNull Builder codeBuilder) {
        Element typeElementRaw = types.asElement(type);
        if (typeElementRaw == null || typeElementRaw.getKind() != ElementKind.CLASS)
            return false;
        TypeElement typeElement = (TypeElement) typeElementRaw;
        TypeName typeName = TypeName.get(type);
        String methodName = generateCheckMethod(ctx, typeName, typeElement);
        EnforceNotNull enforceNotNull = ctx.getAnnotation(EnforceNotNull.class);
        if (enforceNotNull != null && enforceNotNull.value() != EnforceMode.IGNORE) {
            codeBuilder.beginControlFlow("if ($L == null)", src);
            switch (enforceNotNull.value()) {
            case TRY_FIX:
                if (ctx.canSet) {
                    if (TypeUtils.hasDefaultConstructor(typeElement)) {
                        codeBuilder.addStatement("$L = new $T()", src, typeName);
                        break;
                    } else
                        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                                "Validator: Can't fix since class has no 0-parameter constructor, using default value instead",
                                ctx.getEffectiveElement());
                }
            case USE_DEFAULT:
                if (ctx.canSet) {
                    if (ctx.defaultSrc != null) {
                        codeBuilder.addStatement("$L = $L", src, ctx.defaultSrc);
                        break;
                    }
                }
            case ERROR:
                codeBuilder.addStatement(errDelegate.generateThrow(" is null!"));
                break;
            }
            codeBuilder.endControlFlow();
        }
        if (methodName != null)
            codeBuilder.addStatement("$L($L)", methodName, src);
        return true;
    }

    private @Nullable String generateCheckMethod(@NotNull ValidatorContext ctx,
                                                 @NotNull TypeName typeName, @NotNull TypeElement typeElement) {
        String typeSimpleName = typeElement.getSimpleName().toString();
        String methodName = "check" + typeSimpleName;
        if (ctx.emptyMethods.contains(methodName))
            return null;
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        String defaultsName = "DEFAULTS_" + StringUtils.camelCaseToSnakeCase(typeSimpleName).toUpperCase(Locale.ROOT);
        List<VariableElement> fields = TypeUtils.getFieldsToSerialize(typeElement);
        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(typeName, ctx.configName);
        if (ctx.nonNullAnnotation != null)
            configParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addParameter(configParamBuilder.build())
                .addException(IllegalArgumentException.class);
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        Element elementBackup = ctx.element, enclosingElementBackup = ctx.enclosingElement;
        String defaultSrcBackup = ctx.defaultSrc;
        ctx.enclosingElement = typeElement;
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            ctx.element = field;
            ctx.defaultSrc = defaultsName + "." + fieldName;
            ctx.appendCheck(field.asType(), ctx.configName + "." + fieldName,
                    ErrorDelegate.simple(ctx.configName + "." + fieldName), codeBuilder);
        }
        ctx.defaultSrc = defaultSrcBackup;
        ctx.enclosingElement = enclosingElementBackup;
        ctx.element = elementBackup;
        if (codeBuilder.isEmpty()) {
            ctx.emptyMethods.add(methodName);
            return null;
        } else {
            ctx.generatedMethods.add(methodName);
            ctx.classBuilder.addField(FieldSpec.builder(typeName, defaultsName, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T()", typeName)
                    .build());
            ctx.classBuilder.addMethod(methodBuilder.addCode(codeBuilder.build()).build());
            return methodName;
        }
    }

}
