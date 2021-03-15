package io.github.speedbridgemc.config.processor.serialize.jankson;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.BaseJanksonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.JanksonContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ListArrayJanksonDelegate extends BaseJanksonDelegate {
    private TypeMirror listTM;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        listTM = TypeUtils.getTypeMirror(processingEnv, List.class.getCanonicalName());
        if (listTM == null)
            return;
        listTM = processingEnv.getTypeUtils().erasure(listTM);
    }

    @Override
    public boolean appendRead(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        boolean array = false;
        TypeMirror componentType = null;
        if (type.getKind() == TypeKind.ARRAY) {
            array = true;
            componentType = ((ArrayType) type).getComponentType();
        } else if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            if (listTM == null)
                return false;
            TypeMirror erasedType = processingEnv.getTypeUtils().erasure(declaredType);
            if (processingEnv.getTypeUtils().isSameType(erasedType, listTM)) {
                List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                if (typeArguments.size() == 0) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Serializer: Raw lists are unsupported", ctx.fieldElement);
                    return false;
                }
                componentType = typeArguments.get(0);
            }
        }
        if (componentType == null)
            return false;

        TypeName componentTypeName = TypeName.get(componentType);
        TypeName oldComponentTypeName = componentTypeName;
        String oldDest = dest;
        if (componentTypeName.isPrimitive())
            componentTypeName = componentTypeName.box();
        String compDest, elemDest;
        if (array) {
            int dotI = dest.lastIndexOf('.');
            if (dotI > 0)
                dest = dest.substring(dotI + 1);
            dest += "Tmp";
            compDest = dest + "Comp";
            elemDest = dest + "Elem";
        } else {
            String unqDest = dest;
            int dotI = dest.lastIndexOf('.');
            if (dotI > 0)
                unqDest = dest.substring(dotI + 1);
            compDest = unqDest + "Comp";
            elemDest = unqDest + "Elem";
        }

        VariableElement fieldElementBackup = ctx.fieldElement;
        String elementNameBackup = ctx.elementName;
        ctx.fieldElement = null;
        ctx.elementName = elemDest;

        if (name != null)
            codeBuilder
                    .addStatement("$L = $L.get($S)", elementNameBackup, ctx.objectName, name);
        codeBuilder
                .beginControlFlow("if ($L == $T.INSTANCE)", elementNameBackup, ctx.nullType)
                .addStatement("$L = null", oldDest)
                .nextControlFlow("else if ($L instanceof $T)", elementNameBackup, ctx.arrayType)
                .addStatement("$L = ($T) $L", ctx.arrayName, ctx.arrayType, elementNameBackup);
        if (array)
            codeBuilder
                    .addStatement("$2T<$3T> $1L = new $2T<$3T>()", dest, ArrayList.class, componentTypeName);
        else
            codeBuilder
                    .addStatement("$L = new $T<$T>()", dest, ArrayList.class, componentTypeName);

        codeBuilder
                .addStatement("$T $L", componentType, compDest)
                .beginControlFlow("for ($T $L : $L)", ctx.elementType, ctx.elementName, ctx.arrayName);
        ctx.appendRead(componentType, null, compDest, codeBuilder);
        codeBuilder
                .addStatement("$L.add($L)", dest, compDest)
                .endControlFlow();
        if (array) {
            if (oldComponentTypeName.isPrimitive()) {
                codeBuilder.addStatement("$L = new $T[$L.size()]", oldDest, oldComponentTypeName, dest)
                        .beginControlFlow("for (int i = 0; i < $L.length; i++)", oldDest)
                        .addStatement("$L[i] = $L.get(i)", oldDest, dest)
                        .endControlFlow();
            } else
                codeBuilder.addStatement("$L = $L.toArray(new $T[0])", oldDest, dest, oldComponentTypeName);
        }
        codeBuilder
                .nextControlFlow("else")
                .addStatement("throw new $T($S + $L.getClass().getSimpleName() + $S)",
                        IOException.class, "Type mismatch! Expected \"JsonArray\", got \"", elementNameBackup, "\"!")
                .endControlFlow();

        ctx.fieldElement = fieldElementBackup;
        ctx.elementName = elementNameBackup;

        return true;
    }

    @Override
    public boolean appendWrite(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, CodeBlock.@NotNull Builder codeBuilder) {
        TypeMirror componentType = null;
        if (type.getKind() == TypeKind.ARRAY)
            componentType = ((ArrayType) type).getComponentType();
        else if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            TypeMirror listTM = TypeUtils.getTypeMirror(processingEnv, List.class.getCanonicalName());
            if (listTM == null)
                return false;
            listTM = processingEnv.getTypeUtils().erasure(listTM);
            TypeMirror erasedType = processingEnv.getTypeUtils().erasure(declaredType);
            if (processingEnv.getTypeUtils().isSameType(erasedType, listTM)) {
                List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                if (typeArguments.size() == 0) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Serializer: Raw lists are unsupported", ctx.fieldElement);
                    return false;
                }
                componentType = declaredType.getTypeArguments().get(0);
            }
        }
        if (componentType == null)
            return false;
        TypeName componentTypeName = TypeName.get(componentType);
        String compSrc, arrSrc, elemSrc;
        String unqSrc = src;
        int dotI = src.lastIndexOf('.');
        if (dotI > 0)
            unqSrc = src.substring(dotI + 1);
        compSrc = unqSrc + "Comp";
        arrSrc = unqSrc + "Arr";
        elemSrc = unqSrc + "Elem";

        String arrayNameBackup = ctx.arrayName;
        String elementNameBackup = ctx.elementName;
        VariableElement fieldElementBackup = ctx.fieldElement;
        ctx.arrayName = arrSrc;
        ctx.elementName = elemSrc;
        ctx.fieldElement = null;

        codeBuilder
                .beginControlFlow("if ($L == null)", src);
        if (name == null)
            codeBuilder.addStatement("$L = $T.INSTANCE", elementNameBackup, ctx.nullType);
        else
            codeBuilder.addStatement("$L.put($S, $T.INSTANCE)", ctx.objectName, name, ctx.nullType);
        codeBuilder
                .nextControlFlow("else")
                .addStatement("$1T $2L = new $1T()", ctx.arrayType, ctx.arrayName)
                .beginControlFlow("for ($T $L : $L)", componentTypeName, compSrc, src)
                .addStatement("$T $L", ctx.elementType, ctx.elementName);
        ctx.appendWrite(componentType, null, compSrc, codeBuilder);
        codeBuilder
                .addStatement("$L.add($L)", ctx.arrayName, ctx.elementName)
                .endControlFlow();
        if (name == null)
            codeBuilder.addStatement("$L = $L", elementNameBackup, arrSrc);
        else
            codeBuilder.addStatement("$L.put($S, $L)", ctx.objectName, name, ctx.arrayName);
        codeBuilder.endControlFlow();

        ctx.arrayName = arrayNameBackup;
        ctx.elementName = elementNameBackup;
        ctx.fieldElement = fieldElementBackup;

        return true;
    }
}
