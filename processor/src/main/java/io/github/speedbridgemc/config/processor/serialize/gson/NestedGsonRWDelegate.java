package io.github.speedbridgemc.config.processor.serialize.gson;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.api.gson.BaseGsonRWDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonRWContext;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public final class NestedGsonRWDelegate extends BaseGsonRWDelegate {
    @Override
    public boolean appendRead(@NotNull GsonRWContext ctx, @NotNull VariableElement field, CodeBlock.@NotNull Builder codeBuilder) {
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
        codeBuilder.addStatement("$L.$L = $L(reader)", ctx.varName, name, methodName);
        return true;
    }

    private @NotNull String generateReadMethod(@NotNull GsonRWContext ctx, @NotNull TypeName typeName, @NotNull TypeElement typeElement) {
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
        CodeBlock.Builder codeBuilder = CodeBlock.builder()
                .addStatement("$1T $2L = new $1T()", typeName, ctx.varName)
                .addStatement("reader.beginObject()")
                .beginControlFlow("while (reader.hasNext())")
                .addStatement("$T token = reader.peek()", ctx.tokenType)
                .beginControlFlow("if (token == $T.NAME)", ctx.tokenType)
                .addStatement("String name = reader.nextName()");
        for (VariableElement field : fields) {
            CodeBlock.Builder codeBuilder2 = CodeBlock.builder()
                    .beginControlFlow("if ($S.equals(name))", field.getSimpleName().toString());
            ctx.appendRead(field, codeBuilder2);
            codeBuilder.add(codeBuilder2
                    .addStatement("continue")
                    .endControlFlow()
                    .build());
        }
        methodBuilder.addCode(codeBuilder
                .endControlFlow()
                .addStatement("reader.skipValue()")
                .endControlFlow()
                .addStatement("reader.endObject()")
                .addStatement("return $L", ctx.varName)
                .build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }

    @Override
    public boolean appendWrite(@NotNull GsonRWContext ctx, @NotNull VariableElement field, CodeBlock.@NotNull Builder codeBuilder) {
        // TODO
        return false;
    }
}
