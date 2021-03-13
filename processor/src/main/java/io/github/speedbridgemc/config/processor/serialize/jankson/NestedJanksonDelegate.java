package io.github.speedbridgemc.config.processor.serialize.jankson;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.SerializerComponent;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.BaseJanksonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.JanksonContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

// not annotated with @AutoService, JanksonContext manually calls this delegate
public final class NestedJanksonDelegate extends BaseJanksonDelegate {
    @Override
    public boolean appendRead(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        Element typeElementRaw = processingEnv.getTypeUtils().asElement(type);
        if (typeElementRaw == null || typeElementRaw.getKind() != ElementKind.CLASS) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Serializer: Field has non-class type with no special delegate", ctx.fieldElement);
            return false;
        }
        TypeElement typeElement = (TypeElement) typeElementRaw;
        TypeName typeName = TypeName.get(type);
        String methodName = generateReadMethod(ctx, typeName, typeElement);
        String objDest;
        String unqDest = dest;
        int dotI = dest.lastIndexOf('.');
        if (dotI > 0)
            unqDest = dest.substring(dotI + 1);
        objDest = unqDest + "Obj";
        if (name == null) {
            codeBuilder.beginControlFlow("if ($L instanceof $T)", ctx.elementName, ctx.objectType)
                    .addStatement("$1T $2L = ($1T) $3L", ctx.objectType, objDest, ctx.elementName)
                    .addStatement("$L = $L($L)", dest, methodName, objDest)
                    .nextControlFlow("else")
                    .addStatement("throw new $T($S + $L.getClass().getSimpleName() + $S)",
                            IOException.class, "Type mismatch! Expected \"JsonObject\", got \"", ctx.elementName, "\"!")
                    .endControlFlow();
        } else {
            codeBuilder.addStatement("$1T $2L = $3L.get($1T.class, $4S)", ctx.objectType, objDest, ctx.objectName, name);
            String missingErrorMessage = ctx.missingErrorMessages.get(name);
            if (missingErrorMessage == null)
                codeBuilder.beginControlFlow("if ($L != null)", objDest);
            else {
                codeBuilder.beginControlFlow("if ($L == null)", objDest)
                        .addStatement("throw new $T($S)", IOException.class, String.format(missingErrorMessage, name))
                        .endControlFlow();
            }
            codeBuilder.addStatement("$L = $L($L)", dest, methodName, objDest);
            if (missingErrorMessage == null)
                codeBuilder.endControlFlow();
        }
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
        String configName = "obj" + typeElement.getSimpleName();
        methodBuilder.addCode(CodeBlock.builder()
                .addStatement("$1T $2L = new $1T()", typeName, configName)
                .addStatement("$T $L", ctx.primitiveType, ctx.primitiveName)
                .addStatement("$T $L", ctx.arrayType, ctx.arrayName)
                .addStatement("$T $L", ctx.elementType, ctx.elementName)
                .build());
        HashMap<String, String> missingErrorMessagesBackup = new HashMap<>(ctx.missingErrorMessages);
        ctx.missingErrorMessages.clear();
        String defaultMissingErrorMessage = SerializerComponent.getDefaultMissingErrorMessage(processingEnv, typeElement);
        SerializerComponent.getMissingErrorMessages(processingEnv, fields, defaultMissingErrorMessage, ctx.missingErrorMessages);
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            ctx.fieldElement = field;
            ctx.appendRead(field.asType(), fieldName, configName + "." + fieldName, codeBuilder);
        }
        ctx.missingErrorMessages.clear();
        ctx.missingErrorMessages.putAll(missingErrorMessagesBackup);
        methodBuilder.addCode(codeBuilder
                .addStatement("return $L", configName)
                .build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }

    @Override
    public boolean appendWrite(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, CodeBlock.@NotNull Builder codeBuilder) {
        Element typeElementRaw = processingEnv.getTypeUtils().asElement(type);
        if (typeElementRaw == null || typeElementRaw.getKind() != ElementKind.CLASS) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Serializer: Field has non-class type with no special delegate", ctx.fieldElement);
            return false;
        }
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
        String methodName = "write" + typeElement.getSimpleName().toString();
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        List<VariableElement> fields = TypeUtils.getFieldsIn(typeElement);
        String configName = "obj";
        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(typeName, configName);
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
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            ctx.fieldElement = null;
            ctx.appendWrite(field.asType(), fieldName, configName + "." + fieldName, codeBuilder);
        }
        methodBuilder.addCode(codeBuilder
                .addStatement("return $L", ctx.objectName)
                .build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }
}
