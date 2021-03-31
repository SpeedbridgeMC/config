package io.github.speedbridgemc.config.processor.serialize.gson;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.serialize.SerializerComponentProvider;
import io.github.speedbridgemc.config.processor.serialize.api.gson.BaseGsonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;

public final class EnumGsonDelegate extends BaseGsonDelegate {
    @Override
    public boolean appendRead(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        Element typeElementRaw = types.asElement(type);
        if (typeElementRaw == null || typeElementRaw.getKind() != ElementKind.ENUM)
            return false;
        TypeElement typeElement = (TypeElement) typeElementRaw;
        TypeName typeName = TypeName.get(type);
        String methodName = generateReadMethod(ctx, typeName, typeElement);
        codeBuilder.addStatement("$L = $L($L)", dest, methodName, ctx.readerName);
        if (name != null) {
            String gotFlag = ctx.gotFlags.get(name);
            if (gotFlag != null)
                codeBuilder.addStatement("$L = true", gotFlag);
        }
        return true;
    }

    private @NotNull String generateReadMethod(@NotNull GsonContext ctx, @NotNull TypeName typeName, @NotNull TypeElement typeElement) {
        String typeSimpleName = typeElement.getSimpleName().toString();
        String methodName = "read" + typeSimpleName;
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        ParameterSpec.Builder readerParamBuilder = ParameterSpec.builder(ctx.readerType, ctx.readerName);
        if (ctx.nonNullAnnotation != null)
            readerParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(typeName)
                .addParameter(readerParamBuilder.build())
                .addException(IOException.class);
        if (ctx.nullableAnnotation != null)
            methodBuilder.addAnnotation(ctx.nullableAnnotation);
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        SerializerComponentProvider.EnumKeyType keyType = SerializerComponentProvider.getEnumKeyType(processingEnv, typeElement, ctx.classBuilder);
        if (keyType != null) {
            TypeMirror keyTypeMirror = keyType.type;
            TypeName keyTypeName = TypeName.get(keyType.type);
            if (keyTypeName.isBoxedPrimitive()) {
                keyTypeMirror = types.unboxedType(keyType.type);
                keyTypeName = keyTypeName.unbox();
            }
            String keyDest = "key";
            codeBuilder.addStatement("$T $L", keyTypeName, keyDest);
            ctx.appendRead(keyTypeMirror, null, keyDest, codeBuilder);
            codeBuilder.add("return ")
                    .addStatement(keyType.generateDeserializer(keyDest));
        } else {
            String nameName = "name" + StringUtils.titleCase(typeSimpleName);
            codeBuilder
                    .addStatement("$T $L = $L.nextString()", String.class, nameName, ctx.readerName)
                    .beginControlFlow("try")
                    .addStatement("return $T.valueOf($L)", typeName, nameName)
                    .nextControlFlow("catch ($T e)", IllegalArgumentException.class)
                    .addStatement("throw new $T($S + $L)",
                            IOException.class,
                            "Unknown enum value name! Got ",
                            nameName)
                    .endControlFlow();
        }
        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }

    @Override
    public boolean appendWrite(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, CodeBlock.@NotNull Builder codeBuilder) {
        Element typeElementRaw = types.asElement(type);
        if (typeElementRaw == null || typeElementRaw.getKind() != ElementKind.ENUM)
            return false;
        TypeElement typeElement = (TypeElement) typeElementRaw;
        TypeName typeName = TypeName.get(type);
        String methodName = generateWriteMethod(ctx, typeName, typeElement);
        codeBuilder.addStatement("$L($L, $L)", methodName, ctx.writerName, src);
        return true;
    }

    private @NotNull String generateWriteMethod(@NotNull GsonContext ctx, @NotNull TypeName typeName, @NotNull TypeElement typeElement) {
        String typeSimpleName = typeElement.getSimpleName().toString();
        String methodName = "write" + typeSimpleName;
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        ParameterSpec.Builder writerParamBuilder = ParameterSpec.builder(ctx.writerType, "writer");
        if (ctx.nonNullAnnotation != null)
            writerParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(typeName, "obj");
        if (ctx.nullableAnnotation != null)
            configParamBuilder.addAnnotation(ctx.nullableAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addParameter(writerParamBuilder.build())
                .addParameter(configParamBuilder.build())
                .addException(IOException.class)
                .addCode(CodeBlock.builder()
                        .beginControlFlow("if (obj == null)")
                        .addStatement("$L.nullValue()", ctx.writerName)
                        .addStatement("return")
                        .endControlFlow()
                        .build());
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        SerializerComponentProvider.EnumKeyType keyType = SerializerComponentProvider.getEnumKeyType(processingEnv, typeElement, ctx.classBuilder);
        if (keyType != null) {
            TypeMirror keyTypeMirror = keyType.type;
            TypeName keyTypeName = TypeName.get(keyTypeMirror);
            if (keyTypeName.isBoxedPrimitive()) {
                keyTypeMirror = types.unboxedType(keyTypeMirror);
                keyTypeName = keyTypeName.unbox();
            }
            String keySrc = "key";
            codeBuilder.add("$T $L = ", keyTypeName, keySrc)
                    .addStatement(keyType.generateSerializer("obj"));
            ctx.appendWrite(keyTypeMirror, null, keySrc, codeBuilder);
        } else
            codeBuilder.addStatement("$L.value(obj.name())", ctx.writerName);
        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }

}
