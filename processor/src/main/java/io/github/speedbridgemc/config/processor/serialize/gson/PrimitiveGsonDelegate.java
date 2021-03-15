package io.github.speedbridgemc.config.processor.serialize.gson;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.serialize.api.gson.BaseGsonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.type.TypeMirror;
import java.util.HashMap;

public final class PrimitiveGsonDelegate extends BaseGsonDelegate {
    private static final ClassName STRING_TYPE = ClassName.get(String.class);
    private static final HashMap<TypeName, String> READ_METHODS = new HashMap<>();

    static {
        READ_METHODS.put(STRING_TYPE, "$L = $L.nextString()");
        READ_METHODS.put(TypeName.BOOLEAN, "$L = $L.nextBoolean()");
        READ_METHODS.put(TypeName.INT, "$L = $L.nextInt()");
        READ_METHODS.put(TypeName.LONG, "$L = $L.nextLong()");
        READ_METHODS.put(TypeName.FLOAT, "$L = (float) $L.nextDouble()"); // no nextFloat method?
        READ_METHODS.put(TypeName.DOUBLE, "$L = $L.nextDouble()");
    }

    @Override
    public boolean appendRead(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        TypeName typeName = TypeName.get(type);
        boolean string = STRING_TYPE.equals(typeName);
        boolean boxed = typeName.isBoxedPrimitive();
        if (boxed)
            typeName = typeName.unbox();
        String readMethod = READ_METHODS.get(typeName);
        if (readMethod == null)
            return false;
        if (string || boxed) {
            codeBuilder
                    .beginControlFlow("if ($L.peek() == $T.NULL)", ctx.readerName, ctx.tokenType)
                    .addStatement("$L.skipValue()", ctx.readerName)
                    .addStatement("$L = null", dest)
                    .nextControlFlow("else");
        }
        codeBuilder.addStatement(readMethod, dest, ctx.readerName);
        if (string || boxed)
            codeBuilder.endControlFlow();
        if (name != null) {
            String gotFlag = ctx.gotFlags.get(name);
            if (gotFlag != null)
                codeBuilder.addStatement("$L = true", gotFlag);
        }
        return true;
    }

    @Override
    public boolean appendWrite(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, CodeBlock.@NotNull Builder codeBuilder) {
        TypeName typeName = TypeName.get(type);
        boolean string = STRING_TYPE.equals(typeName);
        boolean boxed = typeName.isBoxedPrimitive();
        if (string || boxed || typeName.isPrimitive()) {
            if (string || boxed) {
                codeBuilder
                        .beginControlFlow("if ($L == null)", src)
                        .addStatement("$L.nullValue()", ctx.writerName)
                        .nextControlFlow("else");
            }
            codeBuilder.addStatement("$L.value($L)", ctx.writerName, src);
            if (string || boxed)
                codeBuilder.endControlFlow();
            return true;
        }
        return false;
    }
}
