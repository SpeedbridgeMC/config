package io.github.speedbridgemc.config.processor.serialize.gson;

import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.api.gson.BaseGsonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonContext;
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

public final class EnumGsonDelegate extends BaseGsonDelegate {
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
    public boolean appendRead(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        Element typeElementRaw = processingEnv.getTypeUtils().asElement(type);
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
        String methodName = "read" + typeSimpleName + "Enum";
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
        CodeBlock.Builder codeBuilder = CodeBlock.builder()
                .beginControlFlow("if ($L.peek() == $T.NULL)", ctx.readerName, ctx.tokenType)
                .addStatement("$L.skipValue()", ctx.readerName)
                .addStatement("return null")
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
            codeBuilder.addStatement("$T $L", keyTypeName, keyDest);
            ctx.appendRead(keyType, null, keyDest, codeBuilder);
            codeBuilder.addStatement("return $L.get($L)", mapName, keyDest);
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
        Element typeElementRaw = processingEnv.getTypeUtils().asElement(type);
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
        String methodName = "write" + typeSimpleName + "Enum";
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
            if (TypeName.get(keyType).isBoxedPrimitive())
                keyType = processingEnv.getTypeUtils().unboxedType(keyType);
            ctx.appendWrite(keyType, null, "obj.getKey()", codeBuilder);
        } else
            codeBuilder.addStatement("$L.value(obj.name())", ctx.writerName);
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
