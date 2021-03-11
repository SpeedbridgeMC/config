package io.github.speedbridgemc.config.processor.serialize.gson;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.serialize.api.gson.BaseGsonRWDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonRWContext;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonRWDelegate;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.VariableElement;

@AutoService(GsonRWDelegate.class)
public final class PrimitiveGsonRWDelegate extends BaseGsonRWDelegate {
    private final TypeName STRING_TYPE = TypeName.get(String.class);

    @Override
    public boolean appendRead(@NotNull GsonRWContext ctx, @NotNull VariableElement field, CodeBlock.@NotNull Builder codeBuilder) {
        String name = field.getSimpleName().toString();
        TypeName type = TypeName.get(field.asType());
        if (STRING_TYPE.equals(type)) {
            codeBuilder.addStatement("$L.$L = reader.nextString()", ctx.varName, name);
            return true;
        }
        if (type.isBoxedPrimitive())
            type = type.unbox();
        if (TypeName.BOOLEAN.equals(type)) {
            codeBuilder.addStatement("$L.$L = reader.nextBoolean()", ctx.varName, name);
            return true;
        }
        if (TypeName.INT.equals(type)) {
            codeBuilder.addStatement("$L.$L = reader.nextInt()", ctx.varName, name);
            return true;
        }
        if (TypeName.LONG.equals(type)) {
            codeBuilder.addStatement("$L.$L = reader.nextLong()", ctx.varName, name);
            return true;
        }
        if (TypeName.FLOAT.equals(type)) {
            codeBuilder.addStatement("$L.$L = (float) reader.nextDouble()", ctx.varName, name);
            return true;
        }
        if (TypeName.DOUBLE.equals(type)) {
            codeBuilder.addStatement("$L.$L = reader.nextDouble()", ctx.varName, name);
            return true;
        }
        return false;
    }

    @Override
    public boolean appendWrite(@NotNull GsonRWContext ctx, @NotNull VariableElement field, CodeBlock.@NotNull Builder codeBuilder) {
        String name = field.getSimpleName().toString();
        TypeName type = TypeName.get(field.asType());
        if (STRING_TYPE.equals(type) || type.isBoxedPrimitive() || type.isPrimitive()) {
            codeBuilder.addStatement("writer.value($L.$L)", ctx.varName, name);
            return true;
        }
        return false;
    }
}
