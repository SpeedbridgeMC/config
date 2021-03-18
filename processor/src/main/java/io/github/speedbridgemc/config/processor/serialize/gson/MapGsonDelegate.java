package io.github.speedbridgemc.config.processor.serialize.gson;

import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.api.gson.BaseGsonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MapGsonDelegate extends BaseGsonDelegate {
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
    public boolean appendRead(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
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

        codeBuilder
                .beginControlFlow("if ($L.peek() == $T.NULL)", ctx.readerName, ctx.tokenType)
                .addStatement("$L.skipValue()", ctx.readerName)
                .addStatement("$L = null", dest)
                .nextControlFlow("else");

        boolean stringKeys = processingEnv.getTypeUtils().isSameType(keyType, stringTM);
        String tokenDest, nameDest, keyDest, valueDest;
        String unqDest = dest;
        int dotI = dest.lastIndexOf('.');
        if (dotI > 0)
            unqDest = dest.substring(dotI + 1);
        tokenDest = unqDest + "Token";
        nameDest = unqDest + "Name";
        keyDest = unqDest + "Key";
        valueDest = unqDest + "Value";

        VariableElement fieldElementBackup = ctx.fieldElement;
        ctx.fieldElement = null;

        if (stringKeys) {
            // object
            codeBuilder
                    .addStatement("$T $L", valueType, valueDest)
                    .addStatement("$L = new $T<$T, $T>()", dest, HashMap.class, keyType, valueType)
                    .addStatement("$L.beginObject()", ctx.readerName)
                    .beginControlFlow("while ($L.hasNext())", ctx.readerName)
                    .addStatement("$T $L = $L.peek()", ctx.tokenType, tokenDest, ctx.readerName)
                    .beginControlFlow("if ($L == $T.NAME)", tokenDest, ctx.tokenType)
                    .addStatement("String $L = $L.nextName()", keyDest, ctx.readerName);
            ctx.appendRead(valueType, null, valueDest, codeBuilder);
            codeBuilder.addStatement("$L.put($L, $L)", dest, keyDest, valueDest)
                    .endControlFlow()
                    .addStatement("$L.skipValue()", ctx.readerName)
                    .endControlFlow()
                    .addStatement("$L.endObject()", ctx.readerName);
        } else {
            // array of key/value pair objects
            HashMap<String, String> gotFlagsBackup = new HashMap<>(ctx.gotFlags);
            HashMap<String, String> missingErrorMessagesBackup = new HashMap<>(ctx.missingErrorMessages);
            ctx.gotFlags.clear();
            ctx.gotFlags.put(keyDest, "got" + StringUtils.titleCase(keyDest));
            ctx.gotFlags.put(valueDest, "got" + StringUtils.titleCase(valueDest));
            ctx.missingErrorMessages.clear();
            ctx.missingErrorMessages.put(keyDest, "Missing complex map entry key!");
            ctx.missingErrorMessages.put(valueDest, "Missing complex map entry value!");

            codeBuilder.add(GsonSerializerProvider.generateGotFlagDecls(ctx).build());
            codeBuilder
                    .addStatement("$T $L = null", keyType, keyDest)
                    .addStatement("$T $L = null", valueType, valueDest)
                    .addStatement("$L.beginArray()", ctx.readerName)
                    .beginControlFlow("while ($L.hasNext())", ctx.readerName)
                    .addStatement("$L.beginObject()", ctx.readerName)
                    .beginControlFlow("while ($L.hasNext())", ctx.readerName)
                    .addStatement("$T $L = $L.peek()", ctx.tokenType, tokenDest, ctx.readerName)
                    .beginControlFlow("if ($L == $T.NAME)", tokenDest, ctx.tokenType)
                    .addStatement("String $L = $L.nextName()", nameDest, ctx.readerName)
                    .beginControlFlow("if ($S.equals($L))", "key", nameDest);
            ctx.appendRead(keyType, keyDest, keyDest, codeBuilder);
            codeBuilder
                    .addStatement("continue")
                    .nextControlFlow("else if ($S.equals($L))", "value", nameDest);
            ctx.appendRead(valueType, valueDest, valueDest, codeBuilder);
            codeBuilder
                    .addStatement("continue")
                    .endControlFlow()
                    .endControlFlow()
                    .addStatement("$L.endObject()", ctx.readerName)
                    .endControlFlow();
            codeBuilder.add(GsonSerializerProvider.generateGotFlagChecks(ctx).build());
            codeBuilder
                    .addStatement("$L.put($L, $L)", dest, keyDest, valueDest)
                    .addStatement("$L = null", keyDest)
                    .addStatement("$L = null", valueDest)
                    .endControlFlow()
                    .addStatement("$L.endArray()", ctx.readerName);

            ctx.gotFlags.clear();
            ctx.gotFlags.putAll(gotFlagsBackup);
            ctx.missingErrorMessages.clear();
            ctx.missingErrorMessages.putAll(missingErrorMessagesBackup);
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

        codeBuilder
                .beginControlFlow("if ($L == null)", src)
                .addStatement("$L.nullValue()", ctx.writerName)
                .nextControlFlow("else");

        boolean stringKeys = processingEnv.getTypeUtils().isSameType(keyType, stringTM);
        String entrySrc, keySrc, valueSrc;
        String unqSrc = src;
        int dotI = src.lastIndexOf('.');
        if (dotI > 0)
            unqSrc = src.substring(dotI + 1);
        entrySrc = unqSrc + "Entry";
        keySrc = unqSrc + "Key";
        valueSrc = unqSrc + "Value";

        VariableElement fieldElementBackup = ctx.fieldElement;
        ctx.fieldElement = null;

        if (stringKeys) {
            // object
            codeBuilder
                    .addStatement("$T $L", valueType, valueSrc)
                    .addStatement("$L.beginObject()", ctx.writerName)
                    .beginControlFlow("for ($T<$T, $T> $L : $L.entrySet())", entryTM, keyType, valueType, entrySrc, src)
                    .addStatement("$L.name($L.getKey())", ctx.writerName, entrySrc)
                    .addStatement("$L = $L.getValue()", valueSrc, entrySrc);
            ctx.appendWrite(valueType, null, valueSrc, codeBuilder);
            codeBuilder
                    .endControlFlow()
                    .addStatement("$L.endObject()", ctx.writerName);
        } else {
            // array of key/value pair objects
            codeBuilder
                    .addStatement("$T $L", keyType, keySrc)
                    .addStatement("$T $L", valueType, valueSrc)
                    .addStatement("$L.beginArray()", ctx.writerName)
                    .beginControlFlow("for ($T<$T, $T> $L : $L.entrySet())", entryTM, keyType, valueType, entrySrc, src)
                    .addStatement("$L = $L.getKey()", keySrc, entrySrc)
                    .addStatement("$L = $L.getValue()", valueSrc, entrySrc)
                    .addStatement("$L.beginObject()", ctx.writerName)
                    .addStatement("$L.name($S)", ctx.writerName, "key");
            ctx.appendWrite(keyType, null, keySrc, codeBuilder);
            codeBuilder.addStatement("$L.name($S)", ctx.writerName, "value");
            ctx.appendWrite(valueType, null, valueSrc, codeBuilder);
            codeBuilder
                    .addStatement("$L.endObject()", ctx.writerName)
                    .endControlFlow()
                    .addStatement("$L.endArray()", ctx.writerName);
        }

        ctx.fieldElement = fieldElementBackup;

        codeBuilder.endControlFlow();

        return true;
    }
}
