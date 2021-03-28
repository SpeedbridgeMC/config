package io.github.speedbridgemc.config.processor.serialize.jankson;

import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.SerializerComponentProvider;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.BaseJanksonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.JanksonContext;
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

public final class MapJanksonDelegate extends BaseJanksonDelegate {
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
    public boolean appendRead(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
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
        codeBuilder
                .beginControlFlow("if ($L != null)", ctx.elementName)
                .addStatement("$L = $L($L)", dest, methodName, ctx.elementName)
                .endControlFlow();

        return true;
    }

    private @NotNull String generateReadMethod(@NotNull JanksonContext ctx, @NotNull TypeMirror keyType, @NotNull TypeMirror valueType) {
        String keyTypeSimpleName = StringUtils.titleCase(TypeUtils.getSimpleIdSafeName(keyType));
        String valueTypeSimpleName = StringUtils.titleCase(TypeUtils.getSimpleIdSafeName(valueType));
        String methodName = "read" + keyTypeSimpleName + "2" + valueTypeSimpleName + "Map";
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        TypeName keyTypeName = TypeName.get(keyType);
        TypeName valueTypeName = TypeName.get(valueType);
        TypeName mapTypeName = ParameterizedTypeName.get(HASH_MAP_NAME, keyTypeName, valueTypeName);
        ParameterSpec.Builder elementParamBuilder = ParameterSpec.builder(ctx.elementType, ctx.elementName);
        if (ctx.nonNullAnnotation != null)
            elementParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(mapTypeName)
                .addParameter(elementParamBuilder.build())
                .addException(IOException.class);
        if (ctx.nullableAnnotation != null)
            methodBuilder.addAnnotation(ctx.nullableAnnotation);
        String mapName = "map" + keyTypeSimpleName + "2" + valueTypeSimpleName;

        boolean stringKeys = types.isSameType(keyType, stringTM);
        String objDest, arrDest, elemDest, entryDest, keyDest, valueDest, keyElemDest, valueElemDest;
        objDest = "obj";
        arrDest = "arr";
        elemDest = "elem" + keyTypeSimpleName + "2" + valueTypeSimpleName;
        entryDest = "entry";
        keyDest = "key";
        valueDest = "value";
        keyElemDest = "keyElem";
        valueElemDest = "valueElem";

        Element elementBackup = ctx.element, enclosingElementBackup = ctx.enclosingElement;
        ctx.enclosingElement = elementBackup;
        ctx.element = null;
        String elementNameBackup = ctx.elementName;
        ctx.elementName = elemDest;

        CodeBlock.Builder codeBuilder = CodeBlock.builder()
                .beginControlFlow("if ($L == $T.INSTANCE)", elementNameBackup, ctx.nullType)
                .addStatement("return null");
        if (stringKeys) {
            // object
            codeBuilder
                    .nextControlFlow("else if ($L instanceof $T)", elementNameBackup, ctx.objectType)
                    .addStatement("$1T $2L = ($1T) $3L", ctx.objectType, objDest, elementNameBackup)
                    .addStatement("$1T $2L = new $1T()", mapTypeName, mapName)
                    .addStatement("$T $L", ctx.primitiveType, ctx.primitiveName)
                    .addStatement("$T $L = $L", valueType, valueDest, SerializerComponentProvider.getDefaultValue(valueTypeName))
                    .beginControlFlow("for ($T<$T, $T> $L : $L.entrySet())",
                            entryTM, stringTM, ctx.elementType, entryDest, objDest)
                    .addStatement("$T $L = $L.getValue()", ctx.elementType, ctx.elementName, entryDest);
            ctx.appendRead(valueType, null, valueDest, codeBuilder);
            codeBuilder
                    .addStatement("$L.put($L.getKey(), $L)", mapName, entryDest, valueDest)
                    .endControlFlow()
                    .addStatement("return $L", mapName)
                    .nextControlFlow("else")
                    .addStatement("throw new $T($S + $L.getClass().getSimpleName() + $S)",
                            IOException.class, "Type mismatch! Expected \"JsonObject\", got \"", elementNameBackup, "\"!")
                    .endControlFlow();
        } else {
            // array of key/value pair objects
            codeBuilder
                    .nextControlFlow("else if ($L instanceof $T)", elementNameBackup, ctx.arrayType)
                    .addStatement("$1T $2L = ($1T) $3L", ctx.arrayType, arrDest, elementNameBackup)
                    .addStatement("$1T $2L = new $1T()", mapTypeName, mapName)
                    .addStatement("$T $L", ctx.primitiveType, ctx.primitiveName)
                    .addStatement("$T $L = $L", keyType, keyDest, SerializerComponentProvider.getDefaultValue(keyTypeName))
                    .addStatement("$T $L = $L", valueType, valueDest, SerializerComponentProvider.getDefaultValue(valueTypeName))
                    .beginControlFlow("for ($T $L : $L)", ctx.elementType, elemDest, arrDest)
                    .beginControlFlow("if ($L instanceof $T)", elemDest, ctx.objectType)
                    .addStatement("$1T $2L = ($1T) $3L", ctx.objectType, objDest, elemDest)
                    .addStatement("$T $L = $L.get($S)", ctx.elementType, keyElemDest, objDest, "key")
                    .beginControlFlow("if ($L == null)", keyElemDest)
                    .addStatement("throw new $T($S)", IOException.class, "Missing complex map entry key!")
                    .endControlFlow();
            ctx.elementName = keyElemDest;
            ctx.appendRead(keyType, null, keyDest, codeBuilder);
            codeBuilder.addStatement("$T $L = $L.get($S)", ctx.elementType, valueElemDest, objDest, "value")
                    .beginControlFlow("if ($L == null)", valueElemDest)
                    .addStatement("throw new $T($S)", IOException.class, "Missing complex map entry value!")
                    .endControlFlow();
            ctx.elementName = valueElemDest;
            ctx.appendRead(valueType, null, valueDest, codeBuilder);
            ctx.elementName = elemDest;
            codeBuilder
                    .addStatement("$L.put($L, $L)", mapName, keyDest, valueDest)
                    .endControlFlow()
                    .endControlFlow()
                    .addStatement("return $L", mapName)
                    .nextControlFlow("else")
                    .addStatement("throw new $T($S + $L.getClass().getSimpleName() + $S)",
                            IOException.class, "Type mismatch! Expected \"JsonArray\", got \"", elementNameBackup, "\"!")
                    .endControlFlow();
        }

        ctx.element = elementBackup;
        ctx.enclosingElement = enclosingElementBackup;
        ctx.elementName = elementNameBackup;

        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }

    @Override
    public boolean appendWrite(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, CodeBlock.@NotNull Builder codeBuilder) {
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
        codeBuilder.addStatement("$L = $L($L)", ctx.elementName, methodName, src);

        return true;
    }

    private @NotNull String generateWriteMethod(@NotNull JanksonContext ctx, @NotNull TypeMirror keyType, @NotNull TypeMirror valueType) {
        String keyTypeSimpleName = StringUtils.titleCase(TypeUtils.getSimpleIdSafeName(keyType));
        String valueTypeSimpleName = StringUtils.titleCase(TypeUtils.getSimpleIdSafeName(valueType));
        String methodName = "write" + keyTypeSimpleName + "2" + valueTypeSimpleName + "Map";
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        TypeName keyTypeName = TypeName.get(keyType);
        TypeName valueTypeName = TypeName.get(valueType);
        TypeName mapTypeName = ParameterizedTypeName.get(MAP_NAME, keyTypeName, valueTypeName);
        String src = "obj";
        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(mapTypeName, src);
        if (ctx.nullableAnnotation != null)
            configParamBuilder.addAnnotation(ctx.nullableAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ctx.elementType)
                .addParameter(configParamBuilder.build())
                .addException(IOException.class)
                .addCode(CodeBlock.builder()
                        .beginControlFlow("if ($L == null)", src)
                        .addStatement("return $T.INSTANCE", ctx.nullType)
                        .endControlFlow()
                        .build());
        if (ctx.nonNullAnnotation != null)
            methodBuilder.addAnnotation(ctx.nonNullAnnotation);
        CodeBlock.Builder codeBuilder = CodeBlock.builder();

        boolean stringKeys = types.isSameType(keyType, stringTM);
        String entrySrc, keySrc, valueSrc, objSrc, arrSrc, elemSrc, keyElemSrc, valueElemSrc;
        entrySrc = "entry" + keyTypeSimpleName + "2" + valueTypeSimpleName;
        keySrc = "key";
        valueSrc = "value";
        objSrc = "obj" + keyTypeSimpleName + "2" + valueTypeSimpleName;
        arrSrc = "arr";
        elemSrc = "elem";
        keyElemSrc = "keyElem";
        valueElemSrc = "valueElem";

        Element elementBackup = ctx.element, enclosingElementBackup = ctx.enclosingElement;
        ctx.enclosingElement = elementBackup;
        ctx.element = null;
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
                    .endControlFlow()
                    .addStatement("return $L", objSrc);
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
                    .endControlFlow()
                    .addStatement("return $L", arrSrc);
        }

        ctx.elementName = elementNameBackup;
        ctx.element = elementBackup;
        ctx.enclosingElement = enclosingElementBackup;

        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }
}
