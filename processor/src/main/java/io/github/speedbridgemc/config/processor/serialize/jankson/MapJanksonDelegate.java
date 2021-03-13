package io.github.speedbridgemc.config.processor.serialize.jankson;

import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.BaseJanksonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.JanksonContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MapJanksonDelegate extends BaseJanksonDelegate {
    private TypeMirror mapTM, stringTM, entryTM;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mapTM = TypeUtils.getTypeMirror(processingEnv, Map.class.getCanonicalName());
        if (mapTM != null)
            mapTM = processingEnv.getTypeUtils().erasure(mapTM);
        stringTM = TypeUtils.getTypeMirror(processingEnv, String.class.getCanonicalName());
        entryTM = TypeUtils.getTypeMirror(processingEnv, Map.Entry.class.getCanonicalName());
        if (entryTM != null)
            entryTM = processingEnv.getTypeUtils().erasure(entryTM);
    }

    @Override
    public boolean appendRead(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        TypeMirror keyType = null, valueType = null;
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            if (mapTM == null)
                return false;
            TypeMirror erasedType = processingEnv.getTypeUtils().erasure(declaredType);
            if (processingEnv.getTypeUtils().isSameType(erasedType, mapTM)) {
                List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                if (typeArguments.size() == 0) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Serializer: Raw maps are unsupported", ctx.fieldElement);
                    return false;
                }
                keyType = typeArguments.get(0);
                valueType = typeArguments.get(1);
            }
        }
        if (keyType == null || valueType == null)
            return false;

        boolean stringKeys = processingEnv.getTypeUtils().isSameType(keyType, stringTM);
        String objDest, arrDest, elemDest, entryDest, keyDest, valueDest, keyElemDest, valueElemDest;
        String unqDest = dest;
        int dotI = dest.lastIndexOf('.');
        if (dotI > 0)
            unqDest = dest.substring(dotI + 1);
        objDest = unqDest + "Obj";
        arrDest = unqDest + "Arr";
        elemDest = unqDest + "Elem";
        entryDest = unqDest + "Entry";
        keyDest = unqDest + "Key";
        valueDest = unqDest + "Value";
        keyElemDest = unqDest + "KeyElem";
        valueElemDest = unqDest + "ValueElem";

        String elementNameBackup = ctx.elementName;
        ctx.elementName = elemDest;

        if (stringKeys) {
            // object
            codeBuilder
                    .addStatement("$T $L", valueType, valueDest)
                    .addStatement("$L = new $T<$T, $T>()", dest, HashMap.class, keyType, valueType);
            if (name == null) {
                codeBuilder
                        .beginControlFlow("if ($L instanceof $T)", elementNameBackup, ctx.objectType)
                        .addStatement("$1T $2L = ($1T) $3L", ctx.objectType, objDest, elementNameBackup)
                        .beginControlFlow("for ($T<$T, $T> $L : $L.entrySet())",
                                entryTM, stringTM, ctx.elementType, entryDest, objDest)
                        .addStatement("$T $L = $L.getValue()", ctx.elementType, ctx.elementName, entryDest);
                ctx.appendRead(valueType, null, valueDest, codeBuilder);
                codeBuilder
                        .addStatement("$L.put($L.getKey(), $L)", dest, entryDest, valueDest)
                        .endControlFlow()
                        .endControlFlow();
            } else {
                String missingErrorMessage = ctx.missingErrorMessages.get(name);
                codeBuilder
                        .addStatement("$1T $2L = $3L.get($1T.class, $4S)", ctx.objectType, objDest, ctx.objectName, name);
                if (missingErrorMessage == null)
                    codeBuilder.beginControlFlow("if ($L != null)", objDest);
                else
                    codeBuilder.beginControlFlow("if ($L == null)", objDest)
                            .addStatement("throw new $T($S)", IOException.class, String.format(missingErrorMessage, name))
                            .endControlFlow();
                codeBuilder.beginControlFlow("for ($T<$T, $T> $L : $L.entrySet())",
                        entryTM, stringTM, ctx.elementType, entryDest, objDest)
                        .addStatement("$T $L = $L.getValue()", ctx.elementType, ctx.elementName, entryDest);
                ctx.appendRead(valueType, null, valueDest, codeBuilder);
                codeBuilder
                        .addStatement("$L.put($L.getKey(), $L)", dest, entryDest, valueDest)
                        .endControlFlow();
                if (missingErrorMessage == null)
                    codeBuilder.endControlFlow();
            }
        } else {
            // array of key/value pair objects
            codeBuilder
                    .addStatement("$T $L", keyType, keyDest)
                    .addStatement("$T $L", valueType, valueDest)
                    .addStatement("$L = new $T<$T, $T>()", dest, HashMap.class, keyType, valueType);
            if (name == null) {
                codeBuilder
                        .beginControlFlow("if ($L instanceof $T)", ctx.elementName, ctx.arrayType)
                        .addStatement("$1T $2L = ($1T) $3L", ctx.arrayType, arrDest, ctx.elementName)
                        .beginControlFlow("for ($T $L : $L)",
                                ctx.elementType, elemDest, arrDest)
                        .beginControlFlow("if ($L instanceof $T)", elemDest, ctx.objectType)
                        .addStatement("$1T $2L = ($1T) $3L", ctx.objectType, objDest, elemDest);
                codeBuilder
                        .endControlFlow()
                        .endControlFlow();
            } else {
                String missingErrorMessage = ctx.missingErrorMessages.get(name);
                codeBuilder
                        .addStatement("$1T $2L = $3L.get($1T.class, $4S)", ctx.arrayType, arrDest, ctx.objectName, name);
                if (missingErrorMessage == null)
                    codeBuilder.beginControlFlow("if ($L != null)", arrDest);
                else
                    codeBuilder.beginControlFlow("if ($L == null)", arrDest)
                            .addStatement("throw new $T($S)", IOException.class, String.format(missingErrorMessage, name))
                            .endControlFlow();
                codeBuilder.beginControlFlow("for ($T $L : $L)",
                        ctx.elementType, elemDest, arrDest)
                        .beginControlFlow("if ($L instanceof $T)", elemDest, ctx.objectType)
                        .addStatement("$1T $2L = ($1T) $3L", ctx.objectType, objDest, elemDest)
                        .addStatement("$1T $2L = $3L.get($1T.class, $4S)", ctx.elementType, keyElemDest, objDest, "key")
                        .beginControlFlow("if ($L == null)", keyElemDest)
                        .addStatement("throw new $T($S)", IOException.class, "Missing field \"key\"!")
                        .endControlFlow();
                ctx.elementName = keyElemDest;
                ctx.appendRead(keyType, null, keyDest, codeBuilder);
                codeBuilder.addStatement("$1T $2L = $3L.get($1T.class, $4S)", ctx.elementType, valueElemDest, objDest, "value")
                        .beginControlFlow("if ($L == null)", valueElemDest)
                        .addStatement("throw new $T($S)", IOException.class, "Missing field \"value\"!")
                        .endControlFlow();
                ctx.elementName = valueElemDest;
                ctx.appendRead(valueType, null, valueDest, codeBuilder);
                ctx.elementName = elemDest;
                codeBuilder
                        .addStatement("$L.put($L, $L)", dest, keyDest, valueDest)
                        .endControlFlow()
                        .endControlFlow();
                if (missingErrorMessage == null)
                    codeBuilder.endControlFlow();
            }
        }

        ctx.elementName = elementNameBackup;

        return true;
    }

    @Override
    public boolean appendWrite(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, CodeBlock.@NotNull Builder codeBuilder) {
        TypeMirror keyType = null, valueType = null;
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            if (mapTM == null)
                return false;
            TypeMirror erasedType = processingEnv.getTypeUtils().erasure(declaredType);
            if (processingEnv.getTypeUtils().isSameType(erasedType, mapTM)) {
                List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                if (typeArguments.size() == 0) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Serializer: Raw maps are unsupported", ctx.fieldElement);
                    return false;
                }
                keyType = typeArguments.get(0);
                valueType = typeArguments.get(1);
            }
        }
        if (keyType == null || valueType == null)
            return false;

        boolean stringKeys = processingEnv.getTypeUtils().isSameType(keyType, stringTM);
        String entrySrc, keySrc, valueSrc, objSrc, arrSrc, elemSrc, keyElemSrc, valueElemSrc;
        String unqSrc = src;
        int dotI = src.lastIndexOf('.');
        if (dotI > 0)
            unqSrc = src.substring(dotI + 1);
        entrySrc = unqSrc + "Entry";
        keySrc = unqSrc + "Key";
        valueSrc = unqSrc + "Value";
        objSrc = unqSrc + "Obj";
        arrSrc = unqSrc + "Arr";
        elemSrc = unqSrc + "Elem";
        keyElemSrc = unqSrc + "KeyElem";
        valueElemSrc = unqSrc + "ValueElem";

        String elementNameBackup = ctx.elementName;
        ctx.elementName = elemSrc;

        if (stringKeys) {
            // object
            codeBuilder
                    .addStatement("$1T $2L = new $1T()", ctx.objectType, objSrc)
                    .beginControlFlow("for ($T<$T, $T> $L : $L.entrySet())",
                            entryTM, keyType, valueType, entrySrc, src)
                    .addStatement("$T $L = $L.getValue()", valueType, valueSrc, entrySrc)
                    .addStatement("$T $L", ctx.elementType, ctx.elementName);
            ctx.appendWrite(valueType, null, valueSrc, codeBuilder);
            codeBuilder
                    .addStatement("$L.put($L.getKey(), $L)", objSrc, entrySrc, elemSrc)
                    .endControlFlow();
            if (name == null)
                codeBuilder.addStatement("$L = $L", elementNameBackup, objSrc);
            else
                codeBuilder.addStatement("$L.put($S, $L)", ctx.objectName, name, objSrc);
        } else {
            // array of key/value objects
            codeBuilder
                    .addStatement("$1T $2L = new $1T()", ctx.arrayType, arrSrc)
                    .beginControlFlow("for ($T<$T, $T> $L : $L.entrySet())",
                            entryTM, keyType, valueType, entrySrc, src);
            ctx.elementName = keyElemSrc;
            codeBuilder
                    .addStatement("$T $L", ctx.elementType, keyElemSrc)
                    .addStatement("$T $L = $L.getKey()", keyType, keySrc, entrySrc);
            ctx.appendWrite(keyType, null, keySrc, codeBuilder);
            ctx.elementName = valueElemSrc;
            codeBuilder
                    .addStatement("$T $L", ctx.elementType, valueElemSrc)
                    .addStatement("$T $L = $L.getValue()", valueType, valueSrc, entrySrc);
            ctx.appendWrite(valueType, null, valueSrc, codeBuilder);
            codeBuilder
                    .addStatement("$1T $2L = new $1T()", ctx.objectType, objSrc)
                    .addStatement("$L.put($S, $L)", objSrc, "key", keyElemSrc)
                    .addStatement("$L.put($S, $L)", objSrc, "value", valueElemSrc)
                    .addStatement("$L.add($L)", arrSrc, objSrc)
                    .endControlFlow();
            if (name == null)
                codeBuilder.addStatement("$L = $L", elementNameBackup, arrSrc);
            else
                codeBuilder.addStatement("$L.put($S, $L)", ctx.objectName, name, arrSrc);
        }

        ctx.elementName = elementNameBackup;

        return true;
    }
}
