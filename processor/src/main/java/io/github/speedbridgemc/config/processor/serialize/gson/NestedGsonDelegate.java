package io.github.speedbridgemc.config.processor.serialize.gson;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.SerializerComponent;
import io.github.speedbridgemc.config.processor.serialize.api.gson.BaseGsonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonContext;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

// not annotated with @AutoService, GsonContext manually calls this delegate
public final class NestedGsonDelegate extends BaseGsonDelegate {
    @Override
    public boolean appendRead(@NotNull GsonContext ctx, @NotNull VariableElement field, CodeBlock.@NotNull Builder codeBuilder) {
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
        codeBuilder.addStatement("$L.$L = $L(reader)", ctx.configName, name, methodName);
        return true;
    }

    private @NotNull String generateReadMethod(@NotNull GsonContext ctx, @NotNull TypeName typeName, @NotNull TypeElement typeElement) {
        String methodName = "read" + typeElement.getSimpleName().toString();
        if (ctx.generatedMethods.contains(methodName))
             return methodName;
        ctx.generatedMethods.add(methodName);
        List<VariableElement> fields = TypeUtils.getFieldsIn(typeElement);
        ParameterSpec.Builder readerParamBuilder = ParameterSpec.builder(ctx.readerType, "reader");
        if (ctx.nonNullAnnotation != null)
            readerParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(typeName)
                .addParameter(readerParamBuilder.build())
                .addException(IOException.class);
        if (ctx.nonNullAnnotation != null)
            methodBuilder.addAnnotation(ctx.nonNullAnnotation);
        methodBuilder.addCode(CodeBlock.builder()
                .addStatement("$1T $2L = new $1T()", typeName, ctx.configName)
                .addStatement("reader.beginObject()")
                .beginControlFlow("while (reader.hasNext())")
                .addStatement("$T token = reader.peek()", ctx.tokenType)
                .beginControlFlow("if (token == $T.NAME)", ctx.tokenType)
                .addStatement("String name = reader.nextName()")
                .build());
        HashMap<String, String> missingErrorMessagesBackup = new HashMap<>(ctx.missingErrorMessages);
        HashMap<String, String> gotFlagsBackup = new HashMap<>(ctx.gotFlags);
        ctx.missingErrorMessages.clear();
        ctx.gotFlags.clear();
        String defaultMissingErrorMessage = SerializerComponent.getDefaultMissingErrorMessage(processingEnv, typeElement);
        SerializerComponent.getMissingErrorMessages(processingEnv, fields, defaultMissingErrorMessage, ctx.missingErrorMessages);
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            String missingErrorMessage = ctx.missingErrorMessages.get(fieldName);
            if (missingErrorMessage == null)
                continue;
            ctx.gotFlags.put(fieldName, "got_" + fieldName);
        }
        CodeBlock.Builder codeBuilder = GsonSerializerProvider.generateGotFlags(ctx);
        methodBuilder.addCode(codeBuilder.build());
        for (VariableElement field : fields) {
            codeBuilder = CodeBlock.builder()
                    .beginControlFlow("if ($S.equals(name))", field.getSimpleName().toString());
            ctx.appendRead(field, codeBuilder);
            methodBuilder.addCode(codeBuilder
                    .addStatement("continue")
                    .endControlFlow()
                    .build());
        }
        methodBuilder.addCode(GsonSerializerProvider.generateGotFlagChecks(ctx).build());
        ctx.missingErrorMessages.clear();
        ctx.missingErrorMessages.putAll(missingErrorMessagesBackup);
        ctx.gotFlags.clear();
        ctx.gotFlags.putAll(gotFlagsBackup);
        methodBuilder.addCode(CodeBlock.builder()
                .endControlFlow()
                .addStatement("reader.skipValue()")
                .endControlFlow()
                .addStatement("reader.endObject()")
                .addStatement("return $L", ctx.configName)
                .build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }

    @Override
    public boolean appendWrite(@NotNull GsonContext ctx, @NotNull VariableElement field, CodeBlock.@NotNull Builder codeBuilder) {
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
        codeBuilder.addStatement("$L($L.$L, writer)", methodName, ctx.configName, name);
        return true;
    }

    private @NotNull String generateWriteMethod(@NotNull GsonContext ctx, @NotNull TypeName typeName, @NotNull TypeElement typeElement) {
        String methodName = "write" + typeElement.getSimpleName().toString();
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        List<VariableElement> fields = TypeUtils.getFieldsIn(typeElement);
        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(typeName, ctx.configName);
        if (ctx.nonNullAnnotation != null)
            configParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        ParameterSpec.Builder writerParamBuilder = ParameterSpec.builder(ctx.writerType, "writer");
        if (ctx.nonNullAnnotation != null)
            writerParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addParameter(configParamBuilder.build())
                .addParameter(writerParamBuilder.build())
                .addException(IOException.class)
                .addCode("writer.beginObject();\n");
        for (VariableElement field : fields) {
            CodeBlock.Builder codeBuilder = CodeBlock.builder()
                    .addStatement("writer.name($S)", field.getSimpleName().toString());
            ctx.appendWrite(field, codeBuilder);
            methodBuilder.addCode(codeBuilder.build());
        }
        methodBuilder.addCode("writer.endObject();\n");
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }
}
