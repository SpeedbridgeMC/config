package io.github.speedbridgemc.config.processor.serialize.jankson;

import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonContext;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.BaseJanksonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.jankson.JanksonContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ListArrayJanksonDelegate extends BaseJanksonDelegate {
    private static final ClassName ARRAY_LIST_NAME = ClassName.get(ArrayList.class),
            ITERABLE_NAME = ClassName.get(Iterable.class);
    private TypeMirror listTM;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        listTM = TypeUtils.getTypeMirror(processingEnv, List.class.getCanonicalName());
        if (listTM == null)
            return;
        listTM = processingEnv.getTypeUtils().erasure(listTM);
    }

    @Override
    public boolean appendRead(@NotNull JanksonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        boolean array = false;
        TypeMirror componentType = null;
        if (type.getKind() == TypeKind.ARRAY) {
            array = true;
            componentType = ((ArrayType) type).getComponentType();
        } else if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            if (listTM == null)
                return false;
            TypeMirror erasedType = processingEnv.getTypeUtils().erasure(declaredType);
            if (processingEnv.getTypeUtils().isSameType(erasedType, listTM)) {
                List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                if (typeArguments.size() == 0) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Serializer: Raw lists are unsupported", ctx.fieldElement);
                    return false;
                }
                componentType = typeArguments.get(0);
            }
        }
        if (componentType == null)
            return false;

        TypeName componentTypeName = TypeName.get(componentType);
        TypeName oldComponentTypeName = componentTypeName;
        String oldDest = dest;
        if (componentTypeName.isPrimitive())
            componentTypeName = componentTypeName.box();
        if (array) {
            int dotI = dest.lastIndexOf('.');
            if (dotI > 0)
                dest = dest.substring(dotI + 1);
            dest += "Tmp";
            codeBuilder.addStatement("$T<$T> $L", ArrayList.class, componentTypeName, dest);
        }

        if (name != null)
            codeBuilder
                    .addStatement("$L = $L.get($S)", ctx.elementName, ctx.objectName, name);
        String methodName = generateReadMethod(ctx, componentTypeName, componentType);
        codeBuilder.addStatement("$L = $L($L)", dest, methodName, ctx.elementName);
        if (array) {
            if (oldComponentTypeName.isPrimitive()) {
                codeBuilder.addStatement("$L = new $T[$L.size()]", oldDest, oldComponentTypeName, dest)
                        .beginControlFlow("for (int i = 0; i < $L.length; i++)", oldDest)
                        .addStatement("$L[i] = $L.get(i)", oldDest, dest)
                        .endControlFlow();
            } else
                codeBuilder.addStatement("$L = $L.toArray(new $T[0])", oldDest, dest, oldComponentTypeName);
        }

        return true;
    }

    private @NotNull String generateReadMethod(@NotNull JanksonContext ctx, @NotNull TypeName componentTypeName, @NotNull TypeMirror componentType) {
        String typeSimpleName = StringUtils.titleCase(TypeUtils.getSimpleName(componentType).replaceAll("\\[]", "Array"));
        String methodName = "read" + typeSimpleName + "List";
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        TypeName listTypeName = ParameterizedTypeName.get(ARRAY_LIST_NAME, componentTypeName);
        ParameterSpec.Builder elementParamBuilder = ParameterSpec.builder(ctx.elementType, ctx.elementName);
        if (ctx.nonNullAnnotation != null)
            elementParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(listTypeName)
                .addParameter(elementParamBuilder.build())
                .addException(IOException.class);
        if (ctx.nullableAnnotation != null)
            methodBuilder.addAnnotation(ctx.nullableAnnotation);
        String listName = "list" + typeSimpleName;
        String compDest = "comp";
        String elemDest = "elem" + typeSimpleName;
        CodeBlock.Builder codeBuilder = CodeBlock.builder()
                .beginControlFlow("if ($L == $T.INSTANCE)", ctx.elementName, ctx.nullType)
                .addStatement("return null")
                .nextControlFlow("else if ($L instanceof $T)", ctx.elementName, ctx.arrayType)
                .addStatement("$2T $1L = ($2T) $3L", ctx.arrayName, ctx.arrayType, ctx.elementName)
                .addStatement("$1T $2L = new $1T()", listTypeName, listName)
                .addStatement("$T $L", ctx.primitiveType, ctx.primitiveName)
                .addStatement("$T $L", componentType, compDest)
                .beginControlFlow("for ($T $L : $L)", ctx.elementType, elemDest, ctx.arrayName);
        String elementNameBackup = ctx.elementName;
        ctx.elementName = elemDest;
        ctx.appendRead(componentType, null, compDest, codeBuilder);
        ctx.elementName = elementNameBackup;
        codeBuilder
                .addStatement("$L.add($L)", listName, compDest)
                .endControlFlow()
                .addStatement("return $L", listName)
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
        TypeMirror componentType = null;
        if (type.getKind() == TypeKind.ARRAY)
            componentType = ((ArrayType) type).getComponentType();
        else if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            TypeMirror listTM = TypeUtils.getTypeMirror(processingEnv, List.class.getCanonicalName());
            if (listTM == null)
                return false;
            listTM = processingEnv.getTypeUtils().erasure(listTM);
            TypeMirror erasedType = processingEnv.getTypeUtils().erasure(declaredType);
            if (processingEnv.getTypeUtils().isSameType(erasedType, listTM)) {
                List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                if (typeArguments.size() == 0) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Serializer: Raw lists are unsupported", ctx.fieldElement);
                    return false;
                }
                componentType = declaredType.getTypeArguments().get(0);
            }
        }
        if (componentType == null)
            return false;
        TypeName componentTypeName = TypeName.get(componentType);

        String methodName = generateWriteMethod(ctx, componentTypeName, componentType);
        if (name == null)
            codeBuilder.addStatement("$L = $L($L)", ctx.elementName, methodName, src);
        else
            codeBuilder.addStatement("$L.put($S, $L($L))", ctx.objectName, name, methodName, src);

        return true;
    }

    private @NotNull String generateWriteMethod(@NotNull JanksonContext ctx, @NotNull TypeName componentTypeName, @NotNull TypeMirror componentType) {
        String typeSimpleName = StringUtils.titleCase(TypeUtils.getSimpleName(componentType).replaceAll("\\[]", "Array"));
        String methodName = "write" + typeSimpleName + "List";
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        TypeName iterableTypeName = ParameterizedTypeName.get(ITERABLE_NAME, componentTypeName.box());
        String src = "obj";
        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(iterableTypeName, src);
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
                .beginControlFlow("if ($L == null)", src)
                .addStatement("return $T.INSTANCE", ctx.nullType)
                .endControlFlow();

        String compSrc = "comp";
        String arrSrc = "elem";
        String elemSrc = "elem" + typeSimpleName;

        String arrayNameBackup = ctx.arrayName;
        String elementNameBackup = ctx.elementName;
        VariableElement fieldElementBackup = ctx.fieldElement;
        ctx.arrayName = arrSrc;
        ctx.elementName = elemSrc;
        ctx.fieldElement = null;

        codeBuilder
                .beginControlFlow("if ($L == null)", src)
                .addStatement("return $T.INSTANCE", ctx.nullType)
                .nextControlFlow("else")
                .addStatement("$1T $2L = new $1T()", ctx.arrayType, ctx.arrayName)
                .beginControlFlow("for ($T $L : $L)", componentTypeName, compSrc, src)
                .addStatement("$T $L", ctx.elementType, ctx.elementName);
        ctx.appendWrite(componentType, null, compSrc, codeBuilder);
        codeBuilder
                .addStatement("$L.add($L)", ctx.arrayName, ctx.elementName)
                .endControlFlow()
                .addStatement("return $L", arrSrc)
                .endControlFlow();

        ctx.fieldElement = fieldElementBackup;
        ctx.arrayName = arrayNameBackup;
        ctx.elementName = elementNameBackup;

        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());

        if (componentTypeName.isPrimitive() || componentTypeName.isBoxedPrimitive()) {
            ParameterSpec.Builder altParamBuilder = ParameterSpec.builder(ArrayTypeName.of(componentTypeName.unbox()), src);
            if (ctx.nullableAnnotation != null)
                altParamBuilder.addAnnotation(ctx.nullableAnnotation);
            ctx.classBuilder.addMethod(MethodSpec.methodBuilder(methodName)
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                    .returns(ctx.elementType)
                    .addParameter(altParamBuilder.build())
                    .addException(IOException.class)
                    .addCode(codeBuilder.build())
                    .build());
        }
        return methodName;
    }
}
