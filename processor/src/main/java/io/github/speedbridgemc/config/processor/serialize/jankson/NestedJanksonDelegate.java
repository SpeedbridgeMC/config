package io.github.speedbridgemc.config.processor.serialize.jankson;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.SerializerComponentProvider;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.BaseJanksonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.JanksonContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;

// not annotated with @AutoService, JanksonContext manually calls this delegate
public final class NestedJanksonDelegate extends BaseJanksonDelegate {
    @Override
    public boolean appendRead(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        Element typeElementRaw = types.asElement(type);
        if (typeElementRaw == null || typeElementRaw.getKind() != ElementKind.CLASS) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Serializer: Field has non-class type with no special delegate", ctx.getEffectiveElement());
            return false;
        }
        TypeElement typeElement = (TypeElement) typeElementRaw;
        if (!TypeUtils.hasDefaultConstructor(typeElement)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Serializer: Field has class type with no 0-parameter constructor or special delegate", ctx.getEffectiveElement());
            return false;
        }
        TypeName typeName = TypeName.get(type);
        String methodName = generateReadMethod(ctx, typeName, typeElement);
        codeBuilder
                .beginControlFlow("if ($L != null)", ctx.elementName)
                .addStatement("$L = $L($L)", dest, methodName, ctx.elementName)
                .endControlFlow();
        return true;
    }

    private @NotNull String generateReadMethod(@NotNull JanksonContext ctx, @NotNull TypeName typeName, @NotNull TypeElement typeElement) {
        String methodName = "read" + typeElement.getSimpleName().toString();
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        List<VariableElement> fields = TypeUtils.getFieldsToSerialize(typeElement);
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
        String configName = "obj" + typeElement.getSimpleName();
        String defaultMissingErrorMessage = SerializerComponentProvider.getDefaultMissingErrorMessage(processingEnv, typeElement);
        methodBuilder.addCode(CodeBlock.builder()
                .beginControlFlow("if ($L == $T.INSTANCE)", ctx.elementName, ctx.nullType)
                .addStatement("return null")
                .nextControlFlow("else if ($L instanceof $T)", ctx.elementName, ctx.objectType)
                .addStatement("$1T $2L = ($1T) $3L", ctx.objectType, ctx.objectName, ctx.elementName)
                .build());
        methodBuilder.addCode(JanksonSerializerProvider.generateFieldChecks(processingEnv, defaultMissingErrorMessage, fields, ctx.objectName)
                .build());
        methodBuilder.addCode(CodeBlock.builder()
                .addStatement("$1T $2L = new $1T()", typeName, configName)
                .addStatement("$T $L", ctx.primitiveType, ctx.primitiveName)
                .addStatement("$T $L", ctx.arrayType, ctx.arrayName)
                .build());
        CodeBlock.Builder codeBuilder = CodeBlock.builder();

        Element elementBackup = ctx.element, enclosingElementBackup = ctx.enclosingElement;
        ctx.enclosingElement = elementBackup;

        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            String serializedName = SerializerComponentProvider.getSerializedName(field);
            ctx.element = field;
            codeBuilder.add(JanksonSerializerProvider.generateGet(ctx, field).build());
            ctx.appendRead(field.asType(), serializedName, configName + "." + fieldName, codeBuilder);
        }

        ctx.element = elementBackup;
        ctx.enclosingElement = enclosingElementBackup;

        methodBuilder.addCode(codeBuilder
                .addStatement("return $L", configName)
                .nextControlFlow("else")
                .addStatement("throw new $T($S + $L.getClass().getSimpleName() + $S)",
                        IOException.class, "Type mismatch! Expected \"JsonObject\", got \"", ctx.elementName, "\"!")
                .endControlFlow()
                .build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }

    @Override
    public boolean appendWrite(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, CodeBlock.@NotNull Builder codeBuilder) {
        Element typeElementRaw = types.asElement(type);
        if (typeElementRaw == null || typeElementRaw.getKind() != ElementKind.CLASS) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Serializer: Field has non-class type with no special delegate", ctx.getEffectiveElement());
            return false;
        }
        TypeElement typeElement = (TypeElement) typeElementRaw;
        TypeName typeName = TypeName.get(type);
        String methodName = generateWriteMethod(ctx, typeName, typeElement);
        codeBuilder.addStatement("$L = $L($L)", ctx.elementName, methodName, src);
        return true;
    }

    private @NotNull String generateWriteMethod(@NotNull JanksonContext ctx, @NotNull TypeName typeName, @NotNull TypeElement typeElement) {
        String methodName = "write" + typeElement.getSimpleName().toString();
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        List<VariableElement> fields = TypeUtils.getFieldsToSerialize(typeElement);
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
        methodBuilder.addCode(CodeBlock.builder()
                .beginControlFlow("if ($L == null)", configName)
                .addStatement("return $T.INSTANCE", ctx.nullType)
                .endControlFlow()
                .addStatement("$1T $2L = new $1T()", ctx.objectType, ctx.objectName)
                .addStatement("$T $L", ctx.elementType, ctx.elementName)
                .build());
        CodeBlock.Builder codeBuilder = CodeBlock.builder();

        Element elementBackup = ctx.element, enclosingElementBackup = ctx.enclosingElement;
        ctx.enclosingElement = elementBackup;

        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            ctx.element = null;
            ctx.appendWrite(field.asType(), fieldName, configName + "." + fieldName, codeBuilder);
            codeBuilder.addStatement(JanksonSerializerProvider.generatePut(ctx, field).build());
        }

        ctx.element = elementBackup;
        ctx.enclosingElement = enclosingElementBackup;

        methodBuilder.addCode(codeBuilder
                .addStatement("return $L", ctx.objectName)
                .build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }
}
