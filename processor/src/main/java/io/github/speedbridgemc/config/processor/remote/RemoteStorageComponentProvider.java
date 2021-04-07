package io.github.speedbridgemc.config.processor.remote;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.processor.api.BaseComponentProvider;
import io.github.speedbridgemc.config.processor.api.ComponentContext;
import io.github.speedbridgemc.config.processor.api.ComponentProvider;
import io.github.speedbridgemc.config.processor.api.MethodSignature;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

@AutoService(ComponentProvider.class)
public final class RemoteStorageComponentProvider extends BaseComponentProvider {
    public RemoteStorageComponentProvider() {
        super("speedbridge-config:remote_storage");
    }

    @Override
    public void process(@NotNull String name, @NotNull TypeElement type, @NotNull ImmutableList<VariableElement> fields,
                        @NotNull ComponentContext ctx, TypeSpec.@NotNull Builder classBuilder) {
        if (!ctx.hasMethod(MethodSignature.of(TypeName.VOID, "setRemote", ctx.configType))) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Handler interface is missing required method: void setRemote(" + type.getSimpleName() + ")",
                    ctx.handlerInterfaceTypeElement);
        }

        FieldSpec.Builder remoteFieldBuilder = FieldSpec.builder(ctx.configType, "remoteConfig", Modifier.PRIVATE);
        if (ctx.nullableAnnotation != null)
            remoteFieldBuilder.addAnnotation(ctx.nullableAnnotation);
        ParameterSpec.Builder setRemoteParamBuilder = ParameterSpec.builder(ctx.configType, "remoteConfig");
        if (ctx.nullableAnnotation != null)
            setRemoteParamBuilder.addAnnotation(ctx.nullableAnnotation);
        MethodSpec.Builder setRemoteMethodBuilder = MethodSpec.methodBuilder("setRemote")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(setRemoteParamBuilder.build())
                .addStatement("this.remoteConfig = remoteConfig");
        if (ctx.hasMethod(MethodSignature.of(TypeName.VOID, "notifyChanged", ctx.configType)))
            setRemoteMethodBuilder.addStatement("notifyChanged(remoteConfig)");
        classBuilder.addField(remoteFieldBuilder.build())
                .addMethod(setRemoteMethodBuilder.build());

        ctx.getMethodBuilder
                .beginControlFlow("if (remoteConfig != null)")
                .addStatement("return remoteConfig")
                .endControlFlow();
    }
}
