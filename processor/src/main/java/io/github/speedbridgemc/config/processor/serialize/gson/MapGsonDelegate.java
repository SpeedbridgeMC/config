package io.github.speedbridgemc.config.processor.serialize.gson;

import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.api.gson.BaseGsonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MapGsonDelegate extends BaseGsonDelegate {
    private static final ClassName HASH_MAP_NAME = ClassName.get(HashMap.class),
            MAP_NAME = ClassName.get(Map.class);
    private TypeMirror mapTM, stringTM, entryTM;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mapTM = TypeUtils.getTypeMirror(processingEnv, Map.class.getCanonicalName());
        if (mapTM != null)
            mapTM = types.erasure(mapTM);
        stringTM = TypeUtils.getTypeMirror(processingEnv, String.class.getCanonicalName());
        entryTM = TypeUtils.getTypeMirror(processingEnv, Map.Entry.class.getCanonicalName());
        if (entryTM != null)
            entryTM = types.erasure(entryTM);
    }

    @Override
    public boolean appendRead(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        TypeMirror keyType = null, valueType = null;
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            if (mapTM == null)
                return false;
            TypeMirror erasedType = types.erasure(declaredType);
            if (types.isSameType(erasedType, mapTM)) {
                List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                if (typeArguments.size() == 0) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Serializer: Raw maps are unsupported", ctx.getEffectiveElement());
                    return false;
                }
                keyType = typeArguments.get(0);
                valueType = typeArguments.get(1);
            }
        }
        if (keyType == null || valueType == null)
            return false;

        String methodName = generateReadMethod(ctx, keyType, valueType);
        codeBuilder.addStatement("$L = $L($L)", dest, methodName, ctx.readerName);

        if (name != null) {
            String gotFlag = ctx.gotFlags.get(name);
            if (gotFlag != null)
                codeBuilder.addStatement("$L = true", gotFlag);
        }

        return true;
    }

    private @NotNull String generateReadMethod(@NotNull GsonContext ctx, @NotNull TypeMirror keyType, @NotNull TypeMirror valueType) {
        String keyTypeSimpleName = StringUtils.titleCase(TypeUtils.getSimpleName(keyType)).replaceAll("\\[]", "Array");
        String valueTypeSimpleName = StringUtils.titleCase(TypeUtils.getSimpleName(valueType)).replaceAll("\\[]", "Array");
        String methodName = "read" + keyTypeSimpleName + "2" + valueTypeSimpleName + "Map";
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        TypeName keyTypeName = TypeName.get(keyType);
        TypeName valueTypeName = TypeName.get(valueType);
        TypeName mapTypeName = ParameterizedTypeName.get(HASH_MAP_NAME, keyTypeName, valueTypeName);
        ParameterSpec.Builder readerParamBuilder = ParameterSpec.builder(ctx.readerType, ctx.readerName);
        if (ctx.nonNullAnnotation != null)
            readerParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(mapTypeName)
                .addParameter(readerParamBuilder.build())
                .addException(IOException.class);
        if (ctx.nullableAnnotation != null)
            methodBuilder.addAnnotation(ctx.nullableAnnotation);
        String mapName = "map" + keyTypeSimpleName + "2" + valueTypeSimpleName;
        methodBuilder.addCode(CodeBlock.builder()
                .beginControlFlow("if ($L.peek() == $T.NULL)", ctx.readerName, ctx.tokenType)
                .addStatement("$L.skipValue()", ctx.readerName)
                .addStatement("return null")
                .endControlFlow()
                .addStatement("$1T $2L = new $1T()", mapTypeName, mapName)
                .build());
        CodeBlock.Builder codeBuilder = CodeBlock.builder();

        boolean stringKeys = types.isSameType(keyType, stringTM);
        String tokenDest = "token";
        String nameDest = "name";
        String keyDest = "key";
        String valueDest = "value";
        Element elementBackup = ctx.element, enclosingElementBackup = ctx.enclosingElement;
        ctx.enclosingElement = elementBackup;
        ctx.element = null;

        if (stringKeys) {
            // object
            codeBuilder
                    .addStatement("$T $L", valueType, valueDest)
                    .addStatement("$L.beginObject()", ctx.readerName)
                    .beginControlFlow("while ($L.hasNext())", ctx.readerName)
                    .addStatement("$T $L = $L.peek()", ctx.tokenType, tokenDest, ctx.readerName)
                    .beginControlFlow("if ($L == $T.NAME)", tokenDest, ctx.tokenType)
                    .addStatement("String $L = $L.nextName()", keyDest, ctx.readerName);
            ctx.appendRead(valueType, null, valueDest, codeBuilder);
            codeBuilder.addStatement("$L.put($L, $L)", mapName, keyDest, valueDest)
                    .addStatement("continue")
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
                    .endControlFlow()
                    .addStatement("$L.endObject()", ctx.readerName);
            codeBuilder.add(GsonSerializerProvider.generateGotFlagChecks(ctx).build());
            codeBuilder
                    .addStatement("$L.put($L, $L)", mapName, keyDest, valueDest)
                    .addStatement("$L = null", keyDest)
                    .addStatement("$L = null", valueDest)
                    .endControlFlow()
                    .addStatement("$L.endArray()", ctx.readerName);

            ctx.gotFlags.clear();
            ctx.gotFlags.putAll(gotFlagsBackup);
            ctx.missingErrorMessages.clear();
            ctx.missingErrorMessages.putAll(missingErrorMessagesBackup);
        }

        ctx.element = elementBackup;
        ctx.enclosingElement = enclosingElementBackup;

        codeBuilder.addStatement("return $L", mapName);
        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }

    @Override
    public boolean appendWrite(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, CodeBlock.@NotNull Builder codeBuilder) {
        TypeMirror keyType = null, valueType = null;
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            if (mapTM == null)
                return false;
            TypeMirror erasedType = types.erasure(declaredType);
            if (types.isSameType(erasedType, mapTM)) {
                List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                if (typeArguments.size() == 0) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Serializer: Raw maps are unsupported", ctx.getEffectiveElement());
                    return false;
                }
                keyType = typeArguments.get(0);
                valueType = typeArguments.get(1);
            }
        }
        if (keyType == null || valueType == null)
            return false;

        String methodName = generateWriteMethod(ctx, keyType, valueType);
        codeBuilder.addStatement("$L($L, $L)", methodName, ctx.writerName, src);

        return true;
    }

    private @NotNull String generateWriteMethod(@NotNull GsonContext ctx, @NotNull TypeMirror keyType, @NotNull TypeMirror valueType) {
        String keyTypeSimpleName = StringUtils.titleCase(TypeUtils.getSimpleName(keyType)).replaceAll("\\[]", "Array");
        String valueTypeSimpleName = StringUtils.titleCase(TypeUtils.getSimpleName(valueType)).replaceAll("\\[]", "Array");
        String methodName = "write" + keyTypeSimpleName + "2" + valueTypeSimpleName + "Map";
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        TypeName keyTypeName = TypeName.get(keyType);
        TypeName valueTypeName = TypeName.get(valueType);
        TypeName mapTypeName = ParameterizedTypeName.get(MAP_NAME, keyTypeName, valueTypeName);
        ParameterSpec.Builder writerParamBuilder = ParameterSpec.builder(ctx.writerType, "writer");
        if (ctx.nonNullAnnotation != null)
            writerParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        String src = "obj";
        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(mapTypeName, src);
        if (ctx.nullableAnnotation != null)
            configParamBuilder.addAnnotation(ctx.nullableAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addParameter(writerParamBuilder.build())
                .addParameter(configParamBuilder.build())
                .addException(IOException.class);

        CodeBlock.Builder codeBuilder = CodeBlock.builder()
                .beginControlFlow("if (obj == null)")
                .addStatement("$L.nullValue()", ctx.writerName)
                .addStatement("return")
                .endControlFlow();
        boolean stringKeys = types.isSameType(keyType, stringTM);
        String valueSrc = "value";
        String entrySrc = "entry";
        String keySrc = "key";

        Element elementBackup = ctx.element, enclosingElementBackup = ctx.enclosingElement;
        ctx.enclosingElement = elementBackup;
        ctx.element = null;

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

        ctx.element = elementBackup;
        ctx.enclosingElement = enclosingElementBackup;

        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }
}
