package io.github.speedbridgemc.config.processor.serialize.jankson;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.BaseJanksonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.JanksonContext;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.JanksonDelegate;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.VariableElement;
import java.io.IOException;

@AutoService(JanksonDelegate.class)
public final class PrimitiveJanksonDelegate extends BaseJanksonDelegate {
    private final TypeName STRING_TYPE = TypeName.get(String.class);

    @Override
    public boolean appendRead(@NotNull JanksonContext ctx, @NotNull VariableElement field, CodeBlock.@NotNull Builder codeBuilder) {
        TypeName type = TypeName.get(field.asType());
        if (type.isBoxedPrimitive())
            type = type.unbox();
        if (!STRING_TYPE.equals(type) && !type.isPrimitive())
            return false;
        String name = field.getSimpleName().toString();
        codeBuilder.addStatement("$1L = $2L.get($3T.class, $4S)", ctx.primitiveName, ctx.objectName, ctx.primitiveType, name);
        String missingErrorMessage = ctx.missingErrorMessages.get(name);
        if (missingErrorMessage != null)
            codeBuilder.beginControlFlow("if ($L == null)", ctx.primitiveName)
                    .addStatement("throw new $T($S)", IOException.class, String.format(missingErrorMessage, name))
                    .endControlFlow();
        else
            codeBuilder.beginControlFlow("if ($L != null)", ctx.primitiveName);
        if (STRING_TYPE.equals(type))
            codeBuilder.addStatement("$L.$L = $L.asString()", ctx.configName, name, ctx.primitiveName);
        if (TypeName.BOOLEAN.equals(type))
            codeBuilder.addStatement("$1L.$2L = $3L.asBoolean($1L.$2L)", ctx.configName, name, ctx.primitiveName);
        if (TypeName.INT.equals(type))
            codeBuilder.addStatement("$1L.$2L = $3L.asInt($1L.$2L)", ctx.configName, name, ctx.primitiveName);
        if (TypeName.LONG.equals(type))
            codeBuilder.addStatement("$1L.$2L = $3L.asLong($1L.$2L)", ctx.configName, name, ctx.primitiveName);
        if (TypeName.FLOAT.equals(type))
            codeBuilder.addStatement("$1L.$2L = $3L.asFloat($1L.$2L)", ctx.configName, name, ctx.primitiveName);
        if (TypeName.DOUBLE.equals(type))
            codeBuilder.addStatement("$1L.$2L = $3L.asDouble($1L.$2L)", ctx.configName, name, ctx.primitiveName);
        if (missingErrorMessage == null)
            codeBuilder.endControlFlow();
        return true;
    }

    @Override
    public boolean appendWrite(@NotNull JanksonContext ctx, @NotNull VariableElement field, CodeBlock.@NotNull Builder codeBuilder) {
        String name = field.getSimpleName().toString();
        TypeName type = TypeName.get(field.asType());
        if (STRING_TYPE.equals(type) || type.isBoxedPrimitive() || type.isPrimitive()) {
            codeBuilder.addStatement("$1L.put($2S, new $3T($4L.$2L))", ctx.objectName, name, ctx.primitiveType, ctx.configName);
            return true;
        }
        return false;
    }
}
