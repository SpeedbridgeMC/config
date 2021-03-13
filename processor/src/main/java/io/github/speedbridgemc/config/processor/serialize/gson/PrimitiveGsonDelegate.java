package io.github.speedbridgemc.config.processor.serialize.gson;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.serialize.api.gson.BaseGsonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.type.TypeMirror;

public final class PrimitiveGsonDelegate extends BaseGsonDelegate {
    private final TypeName STRING_TYPE = TypeName.get(String.class);

    @Override
    public boolean appendRead(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        boolean ok = false;
        TypeName typeName = TypeName.get(type);
        if (typeName.isBoxedPrimitive())
            typeName = typeName.unbox();
        if (STRING_TYPE.equals(typeName)) {
            codeBuilder.addStatement("$L = $L.nextString()", dest, ctx.readerName);
            ok = true;
        } else if (TypeName.BOOLEAN.equals(typeName)) {
            codeBuilder.addStatement("$L = $L.nextBoolean()", dest, ctx.readerName);
            ok = true;
        } else if (TypeName.INT.equals(typeName)) {
            codeBuilder.addStatement("$L = $L.nextInt()", dest, ctx.readerName);
            ok = true;
        } else if (TypeName.LONG.equals(typeName)) {
            codeBuilder.addStatement("$L = $L.nextLong()", dest, ctx.readerName);
            ok = true;
        } else if (TypeName.FLOAT.equals(typeName)) {
            codeBuilder.addStatement("$L = (float) $L.nextDouble()", dest, ctx.readerName);
            ok = true;
        } else if (TypeName.DOUBLE.equals(typeName)) {
            codeBuilder.addStatement("$L = $L.nextDouble()", dest, ctx.readerName);
            ok = true;
        }
        if (ok && name != null) {
            String gotFlag = ctx.gotFlags.get(name);
            if (gotFlag != null)
                codeBuilder.addStatement("$L = true", gotFlag);
        }
        return ok;
    }

    @Override
    public boolean appendWrite(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, CodeBlock.@NotNull Builder codeBuilder) {
        TypeName typeName = TypeName.get(type);
        if (STRING_TYPE.equals(typeName) || typeName.isBoxedPrimitive() || typeName.isPrimitive()) {
            codeBuilder.addStatement("$L.value($L)", ctx.writerName, src);
            return true;
        }
        return false;
    }
}
