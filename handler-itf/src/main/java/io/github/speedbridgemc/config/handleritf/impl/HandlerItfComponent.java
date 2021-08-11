package io.github.speedbridgemc.config.handleritf.impl;

import com.google.auto.service.AutoService;
import io.github.speedbridgemc.config.processor.api.component.BaseComponent;
import io.github.speedbridgemc.config.processor.api.component.Component;
import io.github.speedbridgemc.config.processor.api.generation.CodeGenerator;
import io.github.speedbridgemc.config.processor.api.property.ConfigPropertyExtensionFinder;
import io.github.speedbridgemc.config.processor.impl.ConfigProcessor;

import java.util.Collection;
import java.util.Collections;

@AutoService(Component.class)
public final class HandlerItfComponent extends BaseComponent {
    private final Collection<CodeGenerator> codeGen = Collections.singleton(new HandlerItfImplGenerator());

    public HandlerItfComponent() {
        super(ConfigProcessor.id("handler-itf"));
    }

    @Override
    public Collection<? extends ConfigPropertyExtensionFinder> propertyExtensionFinders() {
        return Collections.emptyList();
    }

    @Override
    public Collection<? extends CodeGenerator> codeGenerators() {
        return codeGen;
    }
}
