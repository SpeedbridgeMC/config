package io.github.speedbridgemc.config.processor.serialize.gson;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.serialize.api.gson.BaseGsonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonContext;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonDelegate;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.VariableElement;

@AutoService(GsonDelegate.class)
public final class PrimitiveGsonDelegate extends BaseGsonDelegate {
    private final TypeName STRING_TYPE = TypeName.get(String.class);

    @Override
    public boolean appendRead(@NotNull GsonContext ctx, @NotNull VariableElement field, CodeBlock.@NotNull Builder codeBuilder) {
        String name = field.getSimpleName().toString();
        TypeName type = TypeName.get(field.asType());
        if (STRING_TYPE.equals(type)) {
            codeBuilder.addStatement("$L.$L = reader.nextString()", ctx.configName, name);
            return true;
        }
        if (type.isBoxedPrimitive())
            type = type.unbox();
        if (TypeName.BOOLEAN.equals(type)) {
            codeBuilder.addStatement("$L.$L = reader.nextBoolean()", ctx.configName, name);
            return true;
        }
        if (TypeName.INT.equals(type)) {
            codeBuilder.addStatement("$L.$L = reader.nextInt()", ctx.configName, name);
            return true;
        }
        if (TypeName.LONG.equals(type)) {
            codeBuilder.addStatement("$L.$L = reader.nextLong()", ctx.configName, name);
            return true;
        }
        if (TypeName.FLOAT.equals(type)) {
            codeBuilder.addStatement("$L.$L = (float) reader.nextDouble()", ctx.configName, name);
            return true;
        }
        if (TypeName.DOUBLE.equals(type)) {
            codeBuilder.addStatement("$L.$L = reader.nextDouble()", ctx.configName, name);
            return true;
        }
        return false;
    }

    @Override
    public boolean appendWrite(@NotNull GsonContext ctx, @NotNull VariableElement field, CodeBlock.@NotNull Builder codeBuilder) {
        String name = field.getSimpleName().toString();
        TypeName type = TypeName.get(field.asType());
        if (STRING_TYPE.equals(type) || type.isBoxedPrimitive() || type.isPrimitive()) {
            codeBuilder.addStatement("writer.value($L.$L)", ctx.configName, name);
            return true;
        }
        return false;
    }
}
