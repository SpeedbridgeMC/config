package io.github.speedbridgemc.config.processor.serialize.gson;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.SerializerComponentProvider;
import io.github.speedbridgemc.config.processor.serialize.api.gson.BaseGsonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

// not annotated with @AutoService, GsonContext manually calls this delegate
public final class NestedGsonDelegate extends BaseGsonDelegate {
    @Override
    public boolean appendRead(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        Element typeElementRaw = processingEnv.getTypeUtils().asElement(type);
        if (typeElementRaw == null || typeElementRaw.getKind() != ElementKind.CLASS) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Serializer: Field has non-class type with no special delegate", ctx.getEffectiveElement());
            return false;
        }
        TypeElement typeElement = (TypeElement) typeElementRaw;
        if (!TypeUtils.hasDefaultConstructor(typeElement)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Serializer: Field has class type with no 0-parameter constructor or special delegate", ctx.getEffectiveElement());
            return false;
        }
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
        String methodName = "read" + typeElement.getSimpleName().toString();
        if (ctx.generatedMethods.contains(methodName))
             return methodName;
        ctx.generatedMethods.add(methodName);
        List<VariableElement> fields = TypeUtils.getFieldsToSerialize(typeElement);
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

        HashMap<String, String> missingErrorMessagesBackup = new HashMap<>(ctx.missingErrorMessages);
        HashMap<String, String> gotFlagsBackup = new HashMap<>(ctx.gotFlags);
        Element elementBackup = ctx.element, enclosingElementBackup = ctx.enclosingElement;
        ctx.missingErrorMessages.clear();
        ctx.gotFlags.clear();
        ctx.enclosingElement = elementBackup;
        
        String defaultMissingErrorMessage = SerializerComponentProvider.getDefaultMissingErrorMessage(processingEnv, typeElement);
        SerializerComponentProvider.getMissingErrorMessages(processingEnv, fields, defaultMissingErrorMessage, ctx.missingErrorMessages);
        GsonSerializerProvider.generateGotFlags(ctx, fields);
        String objName = "obj" + typeElement.getSimpleName();
        methodBuilder.addCode("$1T $2L = new $1T();\n", typeName, objName);
        methodBuilder.addCode(CodeBlock.builder()
                .add(GsonSerializerProvider.generateGotFlagDecls(ctx).build())
                .addStatement("$L.beginObject()", ctx.readerName)
                .beginControlFlow("while ($L.hasNext())", ctx.readerName)
                .addStatement("$T token = $L.peek()", ctx.tokenType, ctx.readerName)
                .beginControlFlow("if (token == $T.NAME)", ctx.tokenType)
                .addStatement("String name = $L.nextName()", ctx.readerName)
                .beginControlFlow("switch (name)")
                .build());
        CodeBlock.Builder codeBuilder = CodeBlock.builder();
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            String serializedName = SerializerComponentProvider.getSerializedName(field);
            codeBuilder.add("case $S:\n", serializedName);
            for (String alias : SerializerComponentProvider.getSerializedAliases(field))
                codeBuilder.add("case $S:\n", alias);
            codeBuilder.indent();
            ctx.element = field;
            ctx.appendRead(field.asType(), serializedName, objName + "." + fieldName, codeBuilder);
            codeBuilder.addStatement("continue").unindent();
        }
        codeBuilder.add(CodeBlock.builder()
                .endControlFlow()
                .endControlFlow()
                .addStatement("$L.skipValue()", ctx.readerName)
                .endControlFlow()
                .addStatement("$L.endObject()", ctx.readerName)
                .build());
        codeBuilder.add(GsonSerializerProvider.generateGotFlagChecks(ctx)
                .addStatement("return $L", objName)
                .build());

        ctx.element = elementBackup;
        ctx.enclosingElement = enclosingElementBackup;
        ctx.missingErrorMessages.clear();
        ctx.missingErrorMessages.putAll(missingErrorMessagesBackup);
        ctx.gotFlags.clear();
        ctx.gotFlags.putAll(gotFlagsBackup);

        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }

    @Override
    public boolean appendWrite(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, CodeBlock.@NotNull Builder codeBuilder) {
        Element typeElementRaw = processingEnv.getTypeUtils().asElement(type);
        if (typeElementRaw == null || typeElementRaw.getKind() != ElementKind.CLASS) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Serializer: Field has non-class type with no special delegate", ctx.getEffectiveElement());
            return false;
        }
        TypeElement typeElement = (TypeElement) typeElementRaw;
        TypeName typeName = TypeName.get(type);
        String methodName = generateWriteMethod(ctx, typeName, typeElement);
        codeBuilder.addStatement("$L($L, $L)", methodName, ctx.writerName, src);
        return true;
    }

    private @NotNull String generateWriteMethod(@NotNull GsonContext ctx, @NotNull TypeName typeName, @NotNull TypeElement typeElement) {
        String methodName = "write" + typeElement.getSimpleName().toString();
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        List<VariableElement> fields = TypeUtils.getFieldsToSerialize(typeElement);
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
                        .addStatement("$L.beginObject()", ctx.writerName)
                        .build());
        for (VariableElement field : fields) {
            String fieldName = field.getSimpleName().toString();
            String serializedName = SerializerComponentProvider.getSerializedName(field);
            CodeBlock.Builder codeBuilder = CodeBlock.builder()
                    .addStatement("$L.name($S)", ctx.writerName, serializedName);
            ctx.appendWrite(field.asType(), serializedName, "obj." + fieldName, codeBuilder);
            methodBuilder.addCode(codeBuilder.build());
        }
        methodBuilder.addCode("$L.endObject();\n", ctx.writerName);
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }
}
