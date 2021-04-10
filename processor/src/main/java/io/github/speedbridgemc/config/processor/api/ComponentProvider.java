package io.github.speedbridgemc.config.processor.api;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeSpec;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * Defines a configuration component.<p>
 * The {@link #process(String, TypeElement, ImmutableList, ComponentContext, TypeSpec.Builder) process} method will be called
 * for every configuration class that specifies this component in its {@link io.github.speedbridgemc.config.Config Config} annotation.
 */
public interface ComponentProvider extends IdentifiedProvider {
    /**
     * Processes a configuration class that has specified this component.
     * @param name configuration name
     * @param type configuration class
     * @param fields configuration fields to process
     * @param ctx processing context
     * @param classBuilder handler implementation class builder
     */
    void process(@NotNull String name, @NotNull TypeElement type,
                 @NotNull ImmutableList<@NotNull VariableElement> fields,
                 @NotNull ComponentContext ctx,
                 @NotNull TypeSpec.Builder classBuilder);
}
