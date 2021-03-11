package io.github.speedbridgemc.config.processor.remote;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.*;
import io.github.speedbridgemc.config.processor.api.BaseComponentProvider;
import io.github.speedbridgemc.config.processor.api.ComponentContext;
import io.github.speedbridgemc.config.processor.api.ComponentProvider;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

@AutoService(ComponentProvider.class)
public final class RemoteConfigComponent extends BaseComponentProvider {
    public RemoteConfigComponent() {
        super("speedbridge-config:remote");
    }

    @Override
    public void process(@NotNull String name, @NotNull TypeElement type, @NotNull ImmutableList<VariableElement> fields,
                        @NotNull ComponentContext ctx, TypeSpec.@NotNull Builder classBuilder) {
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
