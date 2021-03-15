package io.github.speedbridgemc.config.processor.serialize.gson;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.api.gson.BaseGsonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

public final class ListArrayGsonDelegate extends BaseGsonDelegate {
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
    public boolean appendRead(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
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

        codeBuilder
                .beginControlFlow("if ($L.peek() == $T.NULL)", ctx.readerName, ctx.tokenType)
                .addStatement("$L.skipValue()", ctx.readerName)
                .addStatement("$L = null", dest)
                .nextControlFlow("else");

        TypeName componentTypeName = TypeName.get(componentType);
        TypeName oldComponentTypeName = componentTypeName;
        String oldDest = dest;
        if (componentTypeName.isPrimitive())
            componentTypeName = componentTypeName.box();
        String compDest;
        if (array) {
            int dotI = dest.lastIndexOf('.');
            if (dotI > 0)
                dest = dest.substring(dotI + 1);
            dest += "Tmp";
            compDest = dest + "Comp";
            codeBuilder.addStatement("$2T<$3T> $1L = new $2T<$3T>()", dest, ArrayList.class, componentTypeName);
        } else {
            String unqDest = dest;
            int dotI = dest.lastIndexOf('.');
            if (dotI > 0)
                unqDest = dest.substring(dotI + 1);
            compDest = unqDest + "Comp";
            codeBuilder.addStatement("$L = new $T<$T>()", dest, ArrayList.class, componentTypeName);
        }

        VariableElement fieldElementBackup = ctx.fieldElement;
        ctx.fieldElement = null;

        codeBuilder.addStatement("reader.beginArray()")
                .beginControlFlow("while (reader.hasNext())")
                .addStatement("$T $L", componentTypeName, compDest);
        ctx.appendRead(componentType, null, compDest, codeBuilder);
        codeBuilder
                .addStatement("$L.add($L)", dest, compDest)
                .endControlFlow()
                .addStatement("reader.endArray()");
        if (array) {
            if (oldComponentTypeName.isPrimitive()) {
                codeBuilder.addStatement("$L = new $T[$L.size()]", oldDest, oldComponentTypeName, dest)
                    .beginControlFlow("for (int i = 0; i < $L.length; i++)", oldDest)
                    .addStatement("$L[i] = $L.get(i)", oldDest, dest)
                    .endControlFlow();
            } else
                codeBuilder.addStatement("$L = $L.toArray(new $T[0])", oldDest, dest, oldComponentTypeName);
        }

        ctx.fieldElement = fieldElementBackup;

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
        String compSrc;
        String unqSrc = src;
        int dotI = src.lastIndexOf('.');
        if (dotI > 0)
            unqSrc = src.substring(dotI + 1);
        compSrc = unqSrc + "Comp";

        codeBuilder
                .beginControlFlow("if ($L == null)", src)
                .addStatement("$L.nullValue()", ctx.writerName)
                .nextControlFlow("else");

        VariableElement fieldElementBackup = ctx.fieldElement;
        ctx.fieldElement = null;

        codeBuilder.addStatement("$L.beginArray()", ctx.writerName)
                .beginControlFlow("for ($T $L : $L)", componentTypeName, compSrc, src);
        ctx.appendWrite(componentType, null, compSrc, codeBuilder);
        codeBuilder.endControlFlow()
                .addStatement("$L.endArray()", ctx.writerName);

        ctx.fieldElement = fieldElementBackup;

        codeBuilder.endControlFlow();

        return true;
    }
}
