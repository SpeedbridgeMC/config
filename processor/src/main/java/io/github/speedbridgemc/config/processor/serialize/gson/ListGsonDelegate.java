package io.github.speedbridgemc.config.processor.serialize.gson;

import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.serialize.api.gson.BaseGsonDelegate;
import io.github.speedbridgemc.config.processor.serialize.api.gson.GsonContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ListGsonDelegate extends BaseGsonDelegate {
    private static final ClassName ARRAY_LIST_NAME = ClassName.get(ArrayList.class),
            ITERABLE_NAME = ClassName.get(Iterable.class);
    private TypeMirror listTM;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        listTM = TypeUtils.getTypeMirror(processingEnv, List.class.getCanonicalName());
        if (listTM == null)
            return;
        listTM = types.erasure(listTM);
    }

    @Override
    public boolean appendRead(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String dest, CodeBlock.@NotNull Builder codeBuilder) {
        TypeMirror componentType = null;
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            if (listTM == null)
                return false;
            TypeMirror erasedType = types.erasure(declaredType);
            if (types.isSameType(erasedType, listTM)) {
                List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                if (typeArguments.size() == 0) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Serializer: Raw lists are unsupported", ctx.getEffectiveElement());
                    return false;
                }
                componentType = typeArguments.get(0);
            }
        }
        if (componentType == null)
            return false;

        TypeName componentTypeName = TypeName.get(componentType);
        if (componentTypeName.isPrimitive())
            componentTypeName = componentTypeName.box();

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
        String typeSimpleName = StringUtils.titleCase(TypeUtils.getSimpleIdSafeName(componentType));
        String methodName = "read" + typeSimpleName + "List";
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        TypeName listTypeName = ParameterizedTypeName.get(ARRAY_LIST_NAME, componentTypeName);
        ParameterSpec.Builder readerParamBuilder = ParameterSpec.builder(ctx.readerType, ctx.readerName);
        if (ctx.nonNullAnnotation != null)
            readerParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(listTypeName)
                .addParameter(readerParamBuilder.build())
                .addException(IOException.class);
        if (ctx.nullableAnnotation != null)
            methodBuilder.addAnnotation(ctx.nullableAnnotation);
        String listName = "list" + typeSimpleName;
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

        Element elementBackup = ctx.element, enclosingElementBackup = ctx.enclosingElement;
        ctx.enclosingElement = elementBackup;
        ctx.element = null;
        ctx.appendRead(componentType, null, compDest, codeBuilder);
        ctx.element = elementBackup;
        ctx.enclosingElement = enclosingElementBackup;

        codeBuilder
                .addStatement("$L.add($L)", listName, compDest)
                .endControlFlow()
                .addStatement("reader.endArray()")
                .addStatement("return $L", listName);
        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }

    @Override
    public boolean appendWrite(@NotNull GsonContext ctx, @NotNull TypeMirror type, @Nullable String name, @NotNull String src, CodeBlock.@NotNull Builder codeBuilder) {
        TypeMirror componentType = null;
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            TypeMirror listTM = TypeUtils.getTypeMirror(processingEnv, List.class.getCanonicalName());
            if (listTM == null)
                return false;
            listTM = types.erasure(listTM);
            TypeMirror erasedType = types.erasure(declaredType);
            if (types.isSameType(erasedType, listTM)) {
                List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                if (typeArguments.size() == 0) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Serializer: Raw lists are unsupported", ctx.getEffectiveElement());
                    return false;
                }
                componentType = declaredType.getTypeArguments().get(0);
            }
        }
        if (componentType == null)
            return false;
        TypeName componentTypeName = TypeName.get(componentType);

        String methodName = generateWriteMethod(ctx, componentTypeName, componentType);
        codeBuilder.addStatement("$L($L, $L)", methodName, ctx.writerName, src);

        return true;
    }

    private @NotNull String generateWriteMethod(@NotNull GsonContext ctx, @NotNull TypeName componentTypeName, @NotNull TypeMirror componentType) {
        String typeSimpleName = StringUtils.titleCase(TypeUtils.getSimpleIdSafeName(componentType));
        String methodName = "write" + typeSimpleName + "List";
        if (ctx.generatedMethods.contains(methodName))
            return methodName;
        ctx.generatedMethods.add(methodName);
        TypeName iterableTypeName = ParameterizedTypeName.get(ITERABLE_NAME, componentTypeName.box());
        ParameterSpec.Builder writerParamBuilder = ParameterSpec.builder(ctx.writerType, "writer");
        if (ctx.nonNullAnnotation != null)
            writerParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        String src = "obj";
        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(iterableTypeName, src);
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

        Element elementBackup = ctx.element, enclosingElementBackup = ctx.enclosingElement;
        ctx.enclosingElement = elementBackup;
        ctx.element = null;

        codeBuilder.addStatement("$L.beginArray()", ctx.writerName)
                .beginControlFlow("for ($T comp : $L)", componentTypeName, src);
        ctx.appendWrite(componentType, null, "comp", codeBuilder);
        codeBuilder.endControlFlow()
                .addStatement("$L.endArray()", ctx.writerName);

        ctx.element = elementBackup;
        ctx.enclosingElement = enclosingElementBackup;

        methodBuilder.addCode(codeBuilder.build());
        ctx.classBuilder.addMethod(methodBuilder.build());
        return methodName;
    }
}
