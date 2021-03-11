package io.github.speedbridgemc.config.processor.serialize.jankson;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonContext;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.BaseJanksonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.JanksonContext;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;

// not annotated with @AutoService, JanksonContext manually calls this delegate
public final class NestedJanksonDelegate extends BaseJanksonDelegate {
    @Override
    public boolean appendRead(@NotNull JanksonContext ctx, @NotNull VariableElement field, CodeBlock.@NotNull Builder codeBuilder) {
        String name = field.getSimpleName().toString();
        TypeMirror typeMirror = field.asType();
        Element typeElementRaw = processingEnv.getTypeUtils().asElement(typeMirror);
        if (typeElementRaw == null || typeElementRaw.getKind() != ElementKind.CLASS) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Serializer: Field has non-class type with no special delegate", field);
            return false;
        }
        TypeElement typeElement = (TypeElement) typeElementRaw;
        TypeName typeName = TypeName.get(typeMirror);
        String methodName = generateReadMethod(ctx, typeName, typeElement);
        String nestedObjName = name + "Obj";
        codeBuilder.addStatement("$1T $2L = $3L.get($1T.class, $4S)", ctx.objectType, nestedObjName, ctx.objectName, name)
                .beginControlFlow("if ($L == null)", nestedObjName)
                .addStatement("throw new $T($S)", IOException.class, "Missing field \"" + name + "\"!")
                .endControlFlow()
                .addStatement("$L.$L = $L($L)", ctx.configName, name, methodName, nestedObjName);
        return true;
    }

    private @NotNull String generateReadMethod(@NotNull JanksonContext ctx, @NotNull TypeName typeName, @NotNull TypeElement typeElement) {
        String methodName = "read" + typeElement.getSimpleName().toString();
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        List<VariableElement> fields = TypeUtils.getFieldsIn(typeElement);
        ParameterSpec.Builder objectParamBuilder = ParameterSpec.builder(ctx.objectType, ctx.objectName);
        if (ctx.nonNullAnnotation != null)
            objectParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(typeName)
                .addParameter(objectParamBuilder.build())
                .addException(IOException.class);
        if (ctx.nonNullAnnotation != null)
            methodBuilder.addAnnotation(ctx.nonNullAnnotation);
        methodBuilder.addCode(CodeBlock.builder()
                .addStatement("$1T $2L = new $1T()", typeName, ctx.configName)
                .addStatement("$T $L", ctx.primitiveType, ctx.primitiveName)
                .build());
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        for (VariableElement field : fields)
            ctx.appendRead(field, codeBuilder);
        methodBuilder.addCode(codeBuilder.build());
        methodBuilder.addCode("return $L;\n", ctx.configName);
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }

    @Override
    public boolean appendWrite(@NotNull JanksonContext ctx, @NotNull VariableElement field, CodeBlock.@NotNull Builder codeBuilder) {
        String name = field.getSimpleName().toString();
        TypeMirror typeMirror = field.asType();
        Element typeElementRaw = processingEnv.getTypeUtils().asElement(typeMirror);
        if (typeElementRaw == null || typeElementRaw.getKind() != ElementKind.CLASS) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Serializer: Field has non-class type with no special delegate", field);
            return false;
        }
        TypeElement typeElement = (TypeElement) typeElementRaw;
        TypeName typeName = TypeName.get(typeMirror);
        String methodName = generateWriteMethod(ctx, typeName, typeElement);
        codeBuilder.addStatement("$1L.put($2S, $3L($4L.$2L))", ctx.objectName, name, methodName, ctx.configName);
        return true;
    }

    private @NotNull String generateWriteMethod(@NotNull JanksonContext ctx, @NotNull TypeName typeName, @NotNull TypeElement typeElement) {
        String methodName = "write" + typeElement.getSimpleName().toString();
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        List<VariableElement> fields = TypeUtils.getFieldsIn(typeElement);
        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(typeName, ctx.configName);
        if (ctx.nonNullAnnotation != null)
            configParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(ctx.objectType)
                .addParameter(configParamBuilder.build())
                .addException(IOException.class);
        if (ctx.nonNullAnnotation != null)
            methodBuilder.addAnnotation(ctx.nonNullAnnotation);
        methodBuilder.addCode("$1T $2L = new $1T();\n", ctx.objectType, ctx.objectName);
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        for (VariableElement field : fields)
            ctx.appendWrite(field, codeBuilder);
        methodBuilder.addCode(codeBuilder.build());
        methodBuilder.addCode("return $L;\n", ctx.objectName);
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }
}
