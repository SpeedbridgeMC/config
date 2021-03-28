package io.github.speedbridgemc.config.processor.api;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public abstract class BaseComponentProvider extends BaseIdentifiedProvider implements ComponentProvider {
    public BaseComponentProvider(String id) {
        super(id);
    }

    public void parseOptions(@NotNull String[] options, @NotNull Map<@NotNull String, @NotNull Boolean> map) {
        for (String option : options) {
            if (option.isEmpty())
                continue;
            char first = option.charAt(0);
            boolean enabled = first != '-';
            if (!enabled || first == '+')
                option = option.substring(1);
            map.put(option, enabled);
        }
    }
}
