package io.github.speedbridgemc.config.processor.serialize.gson;

import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.api.gson.BaseGsonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.ArrayList;

public final class ArrayGsonDelegate extends BaseGsonDelegate {
    private static final ClassName ARRAY_LIST_NAME = ClassName.get(ArrayList.class);

    @Override
    public boolean appendRead(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        if (type.getKind() != TypeKind.ARRAY)
            return false;
        TypeMirror componentType = ((ArrayType) type).getComponentType();

        TypeName componentTypeName = TypeName.get(componentType);
        String methodName = generateReadMethod(ctx, componentTypeName, componentType);
        codeBuilder.addStatement("$L = $L($L)", dest, methodName, ctx.readerName);

        if (name != null) {
            String gotFlag = ctx.gotFlags.get(name);
            if (gotFlag != null)
                codeBuilder.addStatement("$L = true", gotFlag);
        }

        return true;
    }

    private @NotNull String generateReadMethod(@NotNull GsonContext ctx, @NotNull TypeName componentTypeName, @NotNull TypeMirror componentType) {
        String typeSimpleName = StringUtils.titleCase(TypeUtils.getSimpleName(componentType).replaceAll("\\[]", "Array"));
        String methodName = "read" + typeSimpleName + "Array";
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        TypeName arrayTypeName = ArrayTypeName.of(componentTypeName);
        ParameterSpec.Builder readerParamBuilder = ParameterSpec.builder(ctx.readerType, ctx.readerName);
        if (ctx.nonNullAnnotation != null)
            readerParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(arrayTypeName)
                .addParameter(readerParamBuilder.build())
                .addException(IOException.class);
        if (ctx.nullableAnnotation != null)
            methodBuilder.addAnnotation(ctx.nullableAnnotation);
        String listName = "list" + typeSimpleName;
        TypeName listCompType = componentTypeName;
        if (listCompType.isPrimitive())
            listCompType = listCompType.box();
        TypeName listTypeName = ParameterizedTypeName.get(ARRAY_LIST_NAME, listCompType);
        methodBuilder.addCode(CodeBlock.builder()
                .beginControlFlow("if ($L.peek() == $T.NULL)", ctx.readerName, ctx.tokenType)
                .addStatement("$L.skipValue()", ctx.readerName)
                .addStatement("return null")
                .endControlFlow()
                .addStatement("$1T $2L = new $1T()", listTypeName, listName)
                .build());
        CodeBlock.Builder codeBuilder = CodeBlock.builder();

        String compDest = "comp" + typeSimpleName;
        codeBuilder.addStatement("$L.beginArray()", ctx.readerName)
                .beginControlFlow("while ($L.hasNext())", ctx.readerName)
                .addStatement("$T $L", componentTypeName, compDest);

        VariableElement fieldElementBackup = ctx.fieldElement;
        ctx.fieldElement = null;
        ctx.appendRead(componentType, null, compDest, codeBuilder);
        ctx.fieldElement = fieldElementBackup;

        codeBuilder
                .addStatement("$L.add($L)", listName, compDest)
                .endControlFlow()
                .addStatement("reader.endArray()");
        if (componentTypeName instanceof ArrayTypeName) {
            TypeName tn = componentTypeName;
            StringBuilder ib = new StringBuilder("[0]");
            while (tn instanceof ArrayTypeName) {
                ib.append("[]");
                tn = ((ArrayTypeName) tn).componentType;
            }
            //int[][] a = new int[0][];
            codeBuilder
                    .addStatement("return $L.toArray(new $T" + ib + ")", listName, tn);
        } else if (componentTypeName.isPrimitive()) {
            codeBuilder
                    .addStatement("$1T[] tmp = new $1T[$2L.size()]", componentTypeName, listName)
                    .beginControlFlow("for (int i = 0; i < tmp.length; i++)")
                    .addStatement("tmp[i] = $L.get(i)", listName)
                    .endControlFlow()
                    .addStatement("return tmp");
        } else
            codeBuilder
                    .addStatement("return $L.toArray(new $T[0])", listName, componentTypeName);
        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }

    @Override
    public boolean appendWrite(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, CodeBlock.@NotNull Builder codeBuilder) {
        if (type.getKind() != TypeKind.ARRAY)
            return false;
        TypeMirror componentType = ((ArrayType) type).getComponentType();

        TypeName componentTypeName = TypeName.get(componentType);
        String methodName = generateWriteMethod(ctx, componentTypeName, componentType);
        codeBuilder.addStatement("$L($L, $L)", methodName, ctx.writerName, src);

        return true;
    }

    private @NotNull String generateWriteMethod(@NotNull GsonContext ctx, @NotNull TypeName componentTypeName, @NotNull TypeMirror componentType) {
        String typeSimpleName = StringUtils.titleCase(TypeUtils.getSimpleName(componentType).replaceAll("\\[]", "Array"));
        String methodName = "write" + typeSimpleName + "Array";
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        ParameterSpec.Builder writerParamBuilder = ParameterSpec.builder(ctx.writerType, "writer");
        if (ctx.nonNullAnnotation != null)
            writerParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        String src = "obj";
        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(ArrayTypeName.of(componentTypeName), src);
        if (ctx.nullableAnnotation != null)
            configParamBuilder.addAnnotation(ctx.nullableAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .addParameter(writerParamBuilder.build())
                .addParameter(configParamBuilder.build())
                .addException(IOException.class);

        CodeBlock.Builder codeBuilder = CodeBlock.builder()
                .beginControlFlow("if (obj == null)")
                .addStatement("$L.nullValue()", ctx.writerName)
                .addStatement("return")
                .endControlFlow();

        VariableElement fieldElementBackup = ctx.fieldElement;
        ctx.fieldElement = null;

        codeBuilder.addStatement("$L.beginArray()", ctx.writerName)
                .beginControlFlow("for ($T comp : $L)", componentTypeName, src);
        ctx.appendWrite(componentType, null, "comp", codeBuilder);
        codeBuilder.endControlFlow()
                .addStatement("$L.endArray()", ctx.writerName);

        ctx.fieldElement = fieldElementBackup;

        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }
}
