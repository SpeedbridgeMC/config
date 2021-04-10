package io.github.speedbridgemc.config.processor.listener;

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
import java.util.ArrayList;
import java.util.function.Consumer;

@AutoService(ComponentProvider.class)
public final class ListenerComponentProvider extends BaseComponentProvider {
    public ListenerComponentProvider() {
        super("speedbridge-config:listener");
    }

    @Override
    public void process(@NotNull String name, @NotNull TypeElement type, @NotNull ImmutableList<@NotNull VariableElement> fields,
                        @NotNull ComponentContext ctx, TypeSpec.@NotNull Builder classBuilder) {
        TypeName listenerName = ParameterizedTypeName.get(ClassName.get(Consumer.class), ctx.configName);
        if (!ctx.hasMethod(MethodSignature.of(TypeName.VOID, "addListener", listenerName))) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Handler interface is missing required method: void addListener(Consumer<" + type.getSimpleName() + ">)",
                    ctx.handlerInterfaceTypeElement);
        }
        if (!ctx.hasMethod(MethodSignature.of(TypeName.VOID, "removeListener", listenerName))) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Handler interface is missing required method: void removeListener(Consumer<" + type.getSimpleName() + ">)",
                    ctx.handlerInterfaceTypeElement);
        }
        if (!ctx.hasMethod(MethodSignature.of(TypeName.VOID, "notifyChanged", ctx.configName))) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "Handler interface is missing required method: void notifyChanged(" + type.getSimpleName() + ")",
                    ctx.handlerInterfaceTypeElement);
        }
        TypeName listName = ParameterizedTypeName.get(ClassName.get(ArrayList.class), listenerName);
        ParameterSpec.Builder listenerParamBuilder = ParameterSpec.builder(listenerName, "listener");
        if (ctx.nonNullAnnotation != null)
            listenerParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        ParameterSpec.Builder configParamBuilder = ParameterSpec.builder(ctx.configName, "config");
        if (ctx.nonNullAnnotation != null)
            configParamBuilder.addAnnotation(ctx.nonNullAnnotation);
        classBuilder
                .addField(FieldSpec.builder(listName, "listeners")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .initializer("new $T()", listName)
                        .build())
                .addField(FieldSpec.builder(listName, "listenersToAdd")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .initializer("new $T()", listName)
                        .build())
                .addField(FieldSpec.builder(listName, "listenersToRemove")
                        .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                        .initializer("new $T()", listName)
                        .build())
                .addMethod(MethodSpec.methodBuilder("addListener")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(listenerParamBuilder.build())
                        .addStatement("listenersToAdd.add(listener)")
                        .build())
                .addMethod(MethodSpec.methodBuilder("removeListener")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(listenerParamBuilder.build())
                        .addStatement("listenersToRemove.add(listener)")
                        .build())
                .addMethod(MethodSpec.methodBuilder("notifyChanged")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(configParamBuilder.build())
                        .addStatement("listeners.addAll(listenersToAdd)")
                        .addStatement("listeners.removeAll(listenersToRemove)")
                        .addStatement("listenersToAdd.clear()")
                        .addStatement("listenersToRemove.clear()")
                        .beginControlFlow("for ($T listener : listeners)", listenerName)
                        .addStatement("listener.accept(config)")
                        .endControlFlow()
                        .build());
        CodeBlock loadBlock = CodeBlock.builder()
                .addStatement("notifyChanged(config)")
                .build();
        ctx.postLoadBuilder.add(loadBlock);
        if (ctx.setMethodBuilder != null)
            ctx.setMethodBuilder.addCode(loadBlock);
        ctx.postSaveBuilder.add(loadBlock);
    }
}
