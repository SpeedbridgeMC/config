package io.github.speedbridgemc.config.processor.serialize.jankson;

import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.SerializerComponentProvider;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.BaseJanksonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.JanksonContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.ArrayList;

public final class ArrayJanksonDelegate extends BaseJanksonDelegate {
    private static final ClassName ARRAY_LIST_NAME = ClassName.get(ArrayList.class);

    @Override
    public boolean appendRead(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        if (type.getKind() != TypeKind.ARRAY)
            return false;
        TypeMirror componentType = ((ArrayType) type).getComponentType();

        TypeName componentTypeName = TypeName.get(componentType);
        String methodName = generateReadMethod(ctx, componentTypeName, componentType);
        codeBuilder.addStatement("$L = $L($L)", dest, methodName, ctx.elementName);

        return true;
    }

    private @NotNull String generateReadMethod(@NotNull JanksonContext ctx, @NotNull TypeName componentTypeName, @NotNull TypeMirror componentType) {
        String typeSimpleName = StringUtils.titleCase(TypeUtils.getSimpleIdSafeName(componentType));
        String methodName = "read" + typeSimpleName + "Array";
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        TypeName arrayTypeName = ArrayTypeName.of(componentTypeName);
        ParameterSpec.Builder elementParamBuilder = ParameterSpec.builder(ctx.elementType, ctx.elementName);
        if (ctx.nonNullAnnotation != null)
            elementParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(arrayTypeName)
                .addParameter(elementParamBuilder.build())
                .addException(IOException.class);
        if (ctx.nullableAnnotation != null)
            methodBuilder.addAnnotation(ctx.nullableAnnotation);
        String listName = "list" + typeSimpleName;
        TypeName listCompType = componentTypeName;
        if (listCompType.isPrimitive())
            listCompType = listCompType.box();
        TypeName listTypeName = ParameterizedTypeName.get(ARRAY_LIST_NAME, listCompType);
        String compDest = "comp";
        String elemDest = "elem" + typeSimpleName;
        CodeBlock.Builder codeBuilder = CodeBlock.builder()
                .beginControlFlow("if ($L == $T.INSTANCE)", ctx.elementName, ctx.nullType)
                .addStatement("return null")
                .nextControlFlow("else if ($L instanceof $T)", ctx.elementName, ctx.arrayType)
                .addStatement("$2T $1L = ($2T) $3L", ctx.arrayName, ctx.arrayType, ctx.elementName)
                .addStatement("$1T $2L = new $1T()", listTypeName, listName)
                .addStatement("$T $L", ctx.primitiveType, ctx.primitiveName)
                .addStatement("$T $L = $L", componentType, compDest, SerializerComponentProvider.getDefaultValue(componentTypeName))
                .beginControlFlow("for ($T $L : $L)", ctx.elementType, elemDest, ctx.arrayName);

        String elementNameBackup = ctx.elementName;
        ctx.elementName = elemDest;
        Element elementBackup = ctx.element, enclosingElementBackup = ctx.enclosingElement;
        ctx.enclosingElement = elementBackup;
        ctx.element = null;

        ctx.appendRead(componentType, null, compDest, codeBuilder);

        ctx.element = elementBackup;
        ctx.enclosingElement = enclosingElementBackup;
        ctx.elementName = elementNameBackup;

        codeBuilder
                .addStatement("$L.add($L)", listName, compDest)
                .endControlFlow();
        if (componentTypeName instanceof ArrayTypeName) {
            TypeName tn = componentTypeName;
            StringBuilder ib = new StringBuilder("[0]");
            while (tn instanceof ArrayTypeName) {
                ib.append("[]");
                tn = ((ArrayTypeName) tn).componentType;
            }
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
        codeBuilder
                .nextControlFlow("else")
                .addStatement("throw new $T($S + $L.getClass().getSimpleName() + $S)",
                        IOException.class, "Type mismatch! Expected \"JsonArray\", got \"", ctx.elementName, "\"!")
                .endControlFlow();
        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }

    @Override
    public boolean appendWrite(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, CodeBlock.@NotNull Builder codeBuilder) {
        if (type.getKind() != TypeKind.ARRAY)
            return false;
        TypeMirror componentType = ((ArrayType) type).getComponentType();

        TypeName componentTypeName = TypeName.get(componentType);
        String methodName = generateWriteMethod(ctx, componentTypeName, componentType);
        codeBuilder.addStatement("$L = $L($L)", ctx.elementName, methodName, src);

        return true;
    }

    private @NotNull String generateWriteMethod(@NotNull JanksonContext ctx, @NotNull TypeName componentTypeName, @NotNull TypeMirror componentType) {
        String typeSimpleName = StringUtils.titleCase(TypeUtils.getSimpleIdSafeName(componentType));
        String methodName = "write" + typeSimpleName + "Array";
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        String src = "obj";
        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(ArrayTypeName.of(componentTypeName), src);
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

        String compSrc = "comp";
        String arrSrc = "arr";
        String elemSrc = "elem" + typeSimpleName;

        String arrayNameBackup = ctx.arrayName;
        String elementNameBackup = ctx.elementName;
        Element elementBackup = ctx.element, enclosingElementBackup = ctx.enclosingElement;
        ctx.enclosingElement = elementBackup;
        ctx.element = null;
        ctx.arrayName = arrSrc;
        ctx.elementName = elemSrc;

        codeBuilder
                .beginControlFlow("if ($L == null)", src)
                .addStatement("return $T.INSTANCE", ctx.nullType)
                .endControlFlow()
                .addStatement("$1T $2L = new $1T()", ctx.arrayType, ctx.arrayName)
                .beginControlFlow("for ($T $L : $L)", componentTypeName, compSrc, src)
                .addStatement("$T $L", ctx.elementType, ctx.elementName);
        ctx.appendWrite(componentType, null, compSrc, codeBuilder);
        codeBuilder
                .addStatement("$L.add($L)", ctx.arrayName, ctx.elementName)
                .endControlFlow()
                .addStatement("return $L", arrSrc);

        ctx.element = elementBackup;
        ctx.enclosingElement = enclosingElementBackup;
        ctx.arrayName = arrayNameBackup;
        ctx.elementName = elementNameBackup;

        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }
}
