package io.github.speedbridgemc.config.handleritf.impl;

import com.google.auto.service.AutoService;
import io.github.speedbridgemc.config.processor.api.generation.BaseCodeGenerator;
import io.github.speedbridgemc.config.processor.api.generation.CodeGenerator;
import io.github.speedbridgemc.config.processor.api.type.ConfigType;

import javax.lang.model.element.TypeElement;

@AutoService(CodeGenerator.class)
public final class HandlerItfImplGenerator extends BaseCodeGenerator {
    @Override
    public void generateFor(Context ctx, ConfigType rootType, TypeElement rootElement) {

    }
}
