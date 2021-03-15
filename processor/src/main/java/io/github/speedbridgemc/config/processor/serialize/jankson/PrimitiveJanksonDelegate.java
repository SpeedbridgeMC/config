package io.github.speedbridgemc.config.processor.serialize.jankson;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.BaseJanksonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.JanksonContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.type.TypeMirror;
import java.io.IOException;

public final class PrimitiveJanksonDelegate extends BaseJanksonDelegate {
    private static final ClassName STRING_TYPE = ClassName.get(String.class);

    @Override
    public boolean appendRead(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        TypeName typeName = TypeName.get(type);
        boolean string = STRING_TYPE.equals(typeName);
        boolean boxed = typeName.isBoxedPrimitive();
        if (boxed)
            typeName = typeName.unbox();
        else if (!string && !typeName.isPrimitive())
            return false;

        if (name == null || boxed || string) {
            if (name != null) {
                codeBuilder
                        .addStatement("$L = $L.get($S)", ctx.elementName, ctx.objectName, name);
            }
            if (boxed || string) {
                // can be null
                codeBuilder
                        .beginControlFlow("if ($L == $T.INSTANCE)", ctx.elementName, ctx.nullType)
                        .addStatement("$L = null", dest)
                        .nextControlFlow("else if ($L instanceof $T)", ctx.elementName, ctx.primitiveType);
            } else
                codeBuilder
                        .beginControlFlow("if ($L instanceof $T)", ctx.elementName, ctx.primitiveType);
            codeBuilder
                    .addStatement("$L = ($T) $L", ctx.primitiveName, ctx.primitiveType, ctx.elementName);
        } else
            codeBuilder
                    .addStatement("$L = $L.get($T.class, $S)", ctx.primitiveName, ctx.objectName, ctx.primitiveType, name)
                    .beginControlFlow("if ($L == null)", ctx.primitiveName)
                    .addStatement("throw new $T($S + $L.get($S).getClass().getSimpleName() + $S)",
                            IOException.class, "Type mismatch! Expected \"JsonPrimitive\", got \"", ctx.objectName, name, "\"!")
                    .endControlFlow();

        String tempDest;
        String unqDest = dest;
        int dotI = dest.lastIndexOf('.');
        if (dotI > 0)
            unqDest = dest.substring(dotI + 1);
        tempDest = unqDest + "Tmp";

        codeBuilder.addStatement("$T $L = $L.getValue()", Object.class, tempDest, ctx.primitiveName);
        if (STRING_TYPE.equals(typeName))
            codeBuilder.addStatement("$L = $T.valueOf($L)", dest, String.class, tempDest);
        if (TypeName.BOOLEAN.equals(typeName))
            codeBuilder.beginControlFlow("if ($L instanceof $T)", tempDest, Boolean.class)
                    .addStatement("$L = (($T) $L).booleanValue()", dest, Boolean.class, tempDest)
                    .nextControlFlow("else")
                    .addStatement("throw new $T($S + $L.getClass().getSimpleName() + $S)",
                            IOException.class, "Type mismatch! Expected \"boolean\", got \"", tempDest, "\"!")
                    .endControlFlow();
        if (TypeName.INT.equals(typeName))
            codeBuilder.beginControlFlow("if ($L instanceof $T)", tempDest, Number.class)
                    .addStatement("$L = (($T) $L).intValue()", dest, Number.class, tempDest)
                    .nextControlFlow("else")
                    .addStatement("throw new $T($S + $L.getClass().getSimpleName() + $S)",
                            IOException.class, "Type mismatch! Expected \"int\", got \"", tempDest, "\"!")
                    .endControlFlow();
        if (TypeName.LONG.equals(typeName))
            codeBuilder.beginControlFlow("if ($L instanceof $T)", tempDest, Number.class)
                    .addStatement("$L = (($T) $L).longValue()", dest, Number.class, tempDest)
                    .nextControlFlow("else")
                    .addStatement("throw new $T($S + $L.getClass().getSimpleName() + $S)",
                            IOException.class, "Type mismatch! Expected \"long\", got \"", tempDest, "\"!")
                    .endControlFlow();
        if (TypeName.FLOAT.equals(typeName))
            codeBuilder.beginControlFlow("if ($L instanceof $T)", tempDest, Number.class)
                    .addStatement("$L = (($T) $L).floatValue()", dest, Number.class, tempDest)
                    .nextControlFlow("else")
                    .addStatement("throw new $T($S + $L.getClass().getSimpleName() + $S)",
                            IOException.class, "Type mismatch! Expected \"float\", got \"", tempDest, "\"!")
                    .endControlFlow();
        if (TypeName.DOUBLE.equals(typeName))
            codeBuilder.beginControlFlow("if ($L instanceof $T)", tempDest, Number.class)
                    .addStatement("$L = (($T) $L).doubleValue()", dest, Number.class, tempDest)
                    .nextControlFlow("else")
                    .addStatement("throw new $T($S + $L.getClass().getSimpleName() + $S)",
                            IOException.class, "Type mismatch! Expected \"double\", got \"", tempDest, "\"!")
                    .endControlFlow();

        if (name == null || boxed || string) {
            codeBuilder
                    .nextControlFlow("else")
                    .addStatement("throw new $T($S + $L.get($S).getClass().getSimpleName() + $S)",
                            IOException.class, "Type mismatch! Expected \"JsonPrimitive\", got \"", ctx.objectName, name, "\"!")
                    .endControlFlow();
        }

        return true;
    }

    @Override
    public boolean appendWrite(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, CodeBlock.@NotNull Builder codeBuilder) {
        TypeName typeName = TypeName.get(type);
        if (STRING_TYPE.equals(typeName) || typeName.isBoxedPrimitive()) {
            if (name == null)
                codeBuilder.addStatement("$1L = $3L == null ? $4T.INSTANCE : new $2T($3L)",
                        ctx.elementName, ctx.primitiveType, src, ctx.nullType);
            else
                codeBuilder.addStatement("$1L.put($2S, $4L == null ? $5T.INSTANCE : new $3T($4L))",
                        ctx.objectName, name, ctx.primitiveType, src, ctx.nullType);
            return true;
        } else if (typeName.isPrimitive()) {
            if (name == null)
                codeBuilder.addStatement("$L = new $T($L)", ctx.elementName, ctx.primitiveType, src);
            else
                codeBuilder.addStatement("$L.put($S, new $T($L))", ctx.objectName, name, ctx.primitiveType, src);
            return true;
        }
        return false;
    }
}
