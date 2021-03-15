package io.github.speedbridgemc.config.processor.remote;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.processor.api.BaseComponentProvider;
import io.github.speedbridgemc.config.processor.api.ComponentContext;
import io.github.speedbridgemc.config.processor.api.ComponentProvider;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.List;

@AutoService(ComponentProvider.class)
public final class RemoteStorageComponentProvider extends BaseComponentProvider {
    public RemoteStorageComponentProvider() {
        super("speedbridge-config:remote-storage");
    }

    @Override
    public void process(@NotNull String name, @NotNull TypeElement type, @NotNull ImmutableList<VariableElement> fields,
                        @NotNull ComponentContext ctx, TypeSpec.@NotNull Builder classBuilder) {
        boolean gotSetRemote = false;
        TypeMirror configTM = type.asType();
        for (ExecutableElement method : ctx.handlerInterfaceMethods) {
            String methodName = method.getSimpleName().toString();
            List<? extends VariableElement> params = method.getParameters();
            TypeMirror returnType = method.getReturnType();
            if ("setRemote".equals(methodName)) {
                if (returnType.getKind() == TypeKind.VOID
                        && params.size() == 1
                        && processingEnv.getTypeUtils().isSameType(params.get(0).asType(), configTM))
                    gotSetRemote = true;
            }
        }
        if (!gotSetRemote)
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Handler interface is missing required method: void setRemote(" + type.getSimpleName() + ")", ctx.handlerInterfaceTypeElement);
        if (!gotSetRemote)
            return;

        FieldSpec.Builder remoteFieldBuilder = FieldSpec.builder(ctx.configType, "remoteConfig", Modifier.PRIVATE);
        if (ctx.nullableAnnotation != null)
            remoteFieldBuilder.addAnnotation(ctx.nullableAnnotation);
        ParameterSpec.Builder setRemoteParamBuilder = ParameterSpec.builder(ctx.configType, "remoteConfig");
        if (ctx.nullableAnnotation != null)
            setRemoteParamBuilder.addAnnotation(ctx.nullableAnnotation);
        classBuilder.addField(remoteFieldBuilder.build())
                .addMethod(MethodSpec.methodBuilder("setRemote")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(setRemoteParamBuilder.build())
                        .addCode("this.remoteConfig = remoteConfig;")
                        .build());
        ctx.getMethodBuilder.addCode(CodeBlock.builder()
                .beginControlFlow("if (remoteConfig != null)")
                .addStatement("return remoteConfig")
                .endControlFlow()
                .build());
    }
}
