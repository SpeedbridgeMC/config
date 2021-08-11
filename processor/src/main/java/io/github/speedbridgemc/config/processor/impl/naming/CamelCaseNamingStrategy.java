package io.github.speedbridgemc.config.processor.impl.naming;

import com.google.auto.service.AutoService;
import io.github.speedbridgemc.config.processor.api.naming.BaseNamingStrategy;
import io.github.speedbridgemc.config.processor.api.naming.NamingStrategy;
import io.github.speedbridgemc.config.processor.api.util.StringUtils;
import io.github.speedbridgemc.config.processor.impl.ConfigProcessor;

@AutoService(NamingStrategy.class)
public final class CamelCaseNamingStrategy extends BaseNamingStrategy {
    public CamelCaseNamingStrategy() {
        super(ConfigProcessor.id("camel_case"));
    }

    @Override
    public String name(String variant, String originalName) {
        String s = StringUtils.snakeCaseToCamelCase(originalName);
        if ("upper".equalsIgnoreCase(variant))
            s = StringUtils.camelCaseToUpperCamelCase(s);
        return s;
    }
}
