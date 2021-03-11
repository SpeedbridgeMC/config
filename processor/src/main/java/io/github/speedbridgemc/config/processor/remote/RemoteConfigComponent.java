package io.github.speedbridgemc.config.processor.remote;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
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
    public void process(@NotNull String name, @NotNull TypeElement type, @NotNull ImmutableSet<VariableElement> fields,
                        @NotNull ComponentContext ctx, TypeSpec.@NotNull Builder classBuilder) {
        classBuilder.addField(ctx.configType, "remoteConfig", Modifier.PRIVATE, Modifier.STATIC)
                .addMethod(MethodSpec.methodBuilder("setRemote")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameter(ctx.configType, "remoteConfig")
                        .addCode(CodeBlock.of("$L.remoteConfig = remoteConfig;", ctx.handlerName))
                        .build());
        ctx.getMethodBuilder.addCode(CodeBlock.builder()
                .beginControlFlow("if (remoteConfig != null)")
                .addStatement("return remoteConfig")
                .endControlFlow()
                .build());
    }
}
