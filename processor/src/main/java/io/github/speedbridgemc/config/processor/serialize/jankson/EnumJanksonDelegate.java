package io.github.speedbridgemc.config.processor.serialize.jankson;

import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.api.gson.BaseGsonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonContext;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.BaseJanksonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.JanksonContext;
import io.github.speedbridgemc.config.serialize.KeyedEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class EnumJanksonDelegate extends BaseJanksonDelegate {
    private static final ClassName MAP_NAME = ClassName.get(HashMap.class);
    private TypeMirror keyedEnumTM;
    private ExecutableElement baseGetKeyMethod;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        keyedEnumTM = TypeUtils.getTypeMirror(processingEnv, KeyedEnum.class.getCanonicalName());
        baseGetKeyMethod = null;
        TypeElement keyedEnumElement = processingEnv.getElementUtils().getTypeElement(KeyedEnum.class.getCanonicalName());
        for (ExecutableElement method : ElementFilter.methodsIn(keyedEnumElement.getEnclosedElements())) {
            if (method.getSimpleName().contentEquals("getKey")) {
                baseGetKeyMethod = method;
                break;
            }
        }
        if (baseGetKeyMethod == null)
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Serializer: Failed to find method KeyedEnum.getKey");
    }

    @Override
    public boolean appendRead(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        Element typeElementRaw = processingEnv.getTypeUtils().asElement(type);
        if (typeElementRaw == null || typeElementRaw.getKind() != ElementKind.ENUM)
            return false;
        TypeElement typeElement = (TypeElement) typeElementRaw;
        TypeName typeName = TypeName.get(type);
        String methodName = generateReadMethod(ctx, typeName, typeElement);
        if (name != null)
            codeBuilder.addStatement("$L = $L.get($S)", ctx.elementName, ctx.objectName, name);
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
                .addStatement("return null");
        if (typeElement.getInterfaces().contains(keyedEnumTM)) {
            codeBuilder.endControlFlow();
            ExecutableElement getKeyMethod = null;
            for (ExecutableElement method : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
                if (processingEnv.getElementUtils().overrides(method, baseGetKeyMethod, typeElement)) {
                    getKeyMethod = method;
                    break;
                }
            }
            if (getKeyMethod == null)
                throw new RuntimeException("Somehow got here with no getKey method implemented in " + typeElement);
            TypeMirror keyType = getKeyMethod.getReturnType();
            TypeName keyTypeName = TypeName.get(keyType);
            String mapName = "MAP_" + StringUtils.camelCaseToScreamingSnakeCase(typeSimpleName);
            TypeName mapType = ParameterizedTypeName.get(MAP_NAME, keyTypeName, typeName);
            ctx.classBuilder.addField(FieldSpec.builder(mapType, mapName, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL).build());
            ctx.classBuilder.addStaticBlock(CodeBlock.builder()
                    .addStatement("$L = new $T()", mapName, mapType)
                    .beginControlFlow("for ($1T e : $1T.values())", typeName)
                    .addStatement("$L.put(e.getKey(), e)", mapName)
                    .endControlFlow()
                    .build());
            if (keyTypeName.isBoxedPrimitive()) {
                keyType = processingEnv.getTypeUtils().unboxedType(keyType);
                keyTypeName = keyTypeName.unbox();
            }
            String keyDest = "key" + StringUtils.titleCase(typeSimpleName);
            codeBuilder
                    .addStatement("$T $L", keyTypeName, keyDest)
                    .addStatement("$T $L", ctx.primitiveType, ctx.primitiveName)
                    .addStatement("$T $L", ctx.arrayType, ctx.arrayName);
            ctx.appendRead(keyType, null, keyDest, codeBuilder);
            codeBuilder.addStatement("return $L.get($L)", mapName, keyDest);
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
                    .endControlFlow();
        }
        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }

    @Override
    public boolean appendWrite(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, CodeBlock.@NotNull Builder codeBuilder) {
        Element typeElementRaw = processingEnv.getTypeUtils().asElement(type);
        if (typeElementRaw == null || typeElementRaw.getKind() != ElementKind.ENUM)
            return false;
        TypeElement typeElement = (TypeElement) typeElementRaw;
        TypeName typeName = TypeName.get(type);
        String methodName = generateWriteMethod(ctx, typeName, typeElement);
        if (name == null)
            codeBuilder.addStatement("$L = $L($L)", ctx.elementName, methodName, src);
        else
            codeBuilder.addStatement("$L.put($S, $L($L))", ctx.objectName, name, methodName, src);
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
        if (typeElement.getInterfaces().contains(keyedEnumTM)) {
            ExecutableElement getKeyMethod = null;
            for (ExecutableElement method : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
                if (processingEnv.getElementUtils().overrides(method, baseGetKeyMethod, typeElement)) {
                    getKeyMethod = method;
                    break;
                }
            }
            if (getKeyMethod == null)
                throw new RuntimeException("Somehow got here with no getKey method implemented in " + typeElement);
            TypeMirror keyType = getKeyMethod.getReturnType();
            TypeName keyTypeName = TypeName.get(keyType);
            if (keyTypeName.isBoxedPrimitive()) {
                keyType = processingEnv.getTypeUtils().unboxedType(keyType);
                keyTypeName = keyTypeName.unbox();
            }
            String src = "src" + typeSimpleName;
            codeBuilder
                    .addStatement("$T $L = $L.getKey()", keyTypeName, src, configName)
                    .addStatement("$T $L", ctx.elementType, ctx.elementName);
            ctx.appendWrite(keyType, null, src, codeBuilder);
            codeBuilder.addStatement("return $L", ctx.elementName);
        } else
            codeBuilder.addStatement("return new $T($L.name())", ctx.primitiveName, configName);
        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }

    private static @NotNull List<@NotNull VariableElement> getEnumConstantsIn(@NotNull TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .map(element -> {
                    if (element.getKind() == ElementKind.ENUM_CONSTANT)
                        return (VariableElement) element;
                    return null;
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static @NotNull List<@NotNull VariableElement> getNonEnumConstantFieldsIn(@NotNull TypeElement typeElement) {
        return typeElement.getEnclosedElements().stream()
                .map(element -> {
                    if (element.getKind() == ElementKind.FIELD)
                        return (VariableElement) element;
                    return null;
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
