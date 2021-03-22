package io.github.speedbridgemc.config.processor.serialize.jankson;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.serialize.SerializerComponentProvider;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.BaseJanksonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.JanksonContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;

public final class EnumJanksonDelegate extends BaseJanksonDelegate {
    @Override
    public boolean appendRead(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        Element typeElementRaw = types.asElement(type);
        if (typeElementRaw == null || typeElementRaw.getKind() != ElementKind.ENUM)
            return false;
        TypeElement typeElement = (TypeElement) typeElementRaw;
        TypeName typeName = TypeName.get(type);
        String methodName = generateReadMethod(ctx, typeName, typeElement);
        codeBuilder.addStatement("$L = $L($L)", dest, methodName, ctx.elementName);
        return true;
    }

    private @NotNull String generateReadMethod(@NotNull JanksonContext ctx, @NotNull TypeName typeName, @NotNull TypeElement typeElement) {
        String typeSimpleName = typeElement.getSimpleName().toString();
        String methodName = "read" + typeSimpleName;
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        ParameterSpec.Builder elementParamBuilder = ParameterSpec.builder(ctx.elementType, ctx.elementName);
        if (ctx.nonNullAnnotation != null)
            elementParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(typeName)
                .addParameter(elementParamBuilder.build())
                .addException(IOException.class);
        if (ctx.nullableAnnotation != null)
            methodBuilder.addAnnotation(ctx.nullableAnnotation);
        CodeBlock.Builder codeBuilder = CodeBlock.builder()
                .beginControlFlow("if ($L == $T.INSTANCE)", ctx.elementName, ctx.nullType)
                .addStatement("return null")
                .endControlFlow();
        SerializerComponentProvider.EnumKeyType keyType = SerializerComponentProvider.getEnumKeyType(processingEnv, typeElement, ctx.classBuilder);
        if (keyType != null) {
            TypeMirror keyTypeMirror = keyType.type;
            TypeName keyTypeName = TypeName.get(keyTypeMirror);
            if (keyTypeName.isBoxedPrimitive()) {
                keyTypeMirror = types.unboxedType(keyTypeMirror);
                keyTypeName = keyTypeName.unbox();
            }
            String keyDest = "key";
            codeBuilder
                    .addStatement("$T $L", keyTypeName, keyDest)
                    .addStatement("$T $L", ctx.primitiveType, ctx.primitiveName)
                    .addStatement("$T $L", ctx.arrayType, ctx.arrayName);
            ctx.appendRead(keyTypeMirror, null, keyDest, codeBuilder);
            codeBuilder.add("return ")
                    .addStatement(keyType.generateDeserializer(keyDest));
        } else {
            String nameName = "name" + StringUtils.titleCase(typeSimpleName);
            codeBuilder
                    .nextControlFlow("else if ($L instanceof $T)", ctx.elementName, ctx.primitiveType)
                    .addStatement("$1T $2L = ($1T) $3L", ctx.primitiveType, ctx.primitiveName, ctx.elementName)
                    .addStatement("$1T $2L = $1T.valueOf($3L)", String.class, nameName, ctx.primitiveName)
                    .beginControlFlow("try")
                    .addStatement("return $T.valueOf($L)", typeName, nameName)
                    .nextControlFlow("catch ($T e)", IllegalArgumentException.class)
                    .addStatement("throw new $T($S + $L)",
                            IOException.class,
                            "Unknown enum value name! Got ",
                            nameName)
                    .endControlFlow()
                    .nextControlFlow("else")
                    .addStatement("throw new $T($S + $L.getClass().getSimpleName() + $S)",
                            IOException.class, "Type mismatch! Expected \"JsonPrimitive\", got \"", ctx.elementName, "\"!")
                    .endControlFlow();
        }
        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }

    @Override
    public boolean appendWrite(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, CodeBlock.@NotNull Builder codeBuilder) {
        Element typeElementRaw = types.asElement(type);
        if (typeElementRaw == null || typeElementRaw.getKind() != ElementKind.ENUM)
            return false;
        TypeElement typeElement = (TypeElement) typeElementRaw;
        TypeName typeName = TypeName.get(type);
        String methodName = generateWriteMethod(ctx, typeName, typeElement);
        codeBuilder.addStatement("$L = $L($L)", ctx.elementName, methodName, src);
        return true;
    }

    private @NotNull String generateWriteMethod(@NotNull JanksonContext ctx, @NotNull TypeName typeName, @NotNull TypeElement typeElement) {
        String typeSimpleName = typeElement.getSimpleName().toString();
        String methodName = "write" + typeSimpleName;
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        String configName = "obj";
        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(typeName, configName);
        if (ctx.nullableAnnotation != null)
            configParamBuilder.addAnnotation(ctx.nullableAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ctx.elementType)
                .addParameter(configParamBuilder.build())
                .addException(IOException.class);
        if (ctx.nonNullAnnotation != null)
            methodBuilder.addAnnotation(ctx.nonNullAnnotation);
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        codeBuilder
                .beginControlFlow("if ($L == null)", configName)
                .addStatement("return $T.INSTANCE", ctx.nullType)
                .endControlFlow();
        SerializerComponentProvider.EnumKeyType keyType = SerializerComponentProvider.getEnumKeyType(processingEnv, typeElement, ctx.classBuilder);
        if (keyType != null) {
            TypeMirror keyTypeMirror = keyType.type;
            TypeName keyTypeName = TypeName.get(keyTypeMirror);
            if (keyTypeName.isBoxedPrimitive()) {
                keyTypeMirror = types.unboxedType(keyTypeMirror);
                keyTypeName = keyTypeName.unbox();
            }
            String src = "src";
            codeBuilder
                    .add("$T $L = ", keyTypeName, src)
                    .addStatement(keyType.generateSerializer(configName))
                    .addStatement("$T $L", ctx.elementType, ctx.elementName);
            ctx.appendWrite(keyTypeMirror, null, src, codeBuilder);
            codeBuilder.addStatement("return $L", ctx.elementName);
        } else
            codeBuilder.addStatement("return new $T($L.name())", ctx.primitiveType, configName);
        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }
}
