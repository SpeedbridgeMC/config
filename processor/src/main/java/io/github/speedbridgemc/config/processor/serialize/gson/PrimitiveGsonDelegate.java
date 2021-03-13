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
    public boolean appendRead(@NotNull GsonContext ctx, @NotNull VariableElement field, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        boolean ok = false;
        TypeName type = TypeName.get(field.asType());
        if (type.isBoxedPrimitive())
            type = type.unbox();
        if (STRING_TYPE.equals(type)) {
            codeBuilder.addStatement("$L = reader.nextString()", dest);
            ok = true;
        } else if (TypeName.BOOLEAN.equals(type)) {
            codeBuilder.addStatement("$L = reader.nextBoolean()", dest);
            ok = true;
        } else if (TypeName.INT.equals(type)) {
            codeBuilder.addStatement("$L = reader.nextInt()", dest);
            ok = true;
        } else if (TypeName.LONG.equals(type)) {
            codeBuilder.addStatement("$L = reader.nextLong()", dest);
            ok = true;
        } else if (TypeName.FLOAT.equals(type)) {
            codeBuilder.addStatement("$L = (float) reader.nextDouble()", dest);
            ok = true;
        } else if (TypeName.DOUBLE.equals(type)) {
            codeBuilder.addStatement("$L = reader.nextDouble()", dest);
            ok = true;
        }
        if (ok) {
            String gotFlag = ctx.gotFlags.get(field.getSimpleName().toString());
            if (gotFlag != null)
                codeBuilder.addStatement("$L = true", gotFlag);
        }
        return ok;
    }

    @Override
    public boolean appendWrite(@NotNull GsonContext ctx, @NotNull VariableElement field, @NotNull String src, CodeBlock.@NotNull Builder codeBuilder) {
        TypeName type = TypeName.get(field.asType());
        if (STRING_TYPE.equals(type) || type.isBoxedPrimitive() || type.isPrimitive()) {
            codeBuilder.addStatement("writer.value($L)", src);
            return true;
        }
        return false;
    }
}
