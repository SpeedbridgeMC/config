package io.github.speedbridgemc.config.processor.api;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A base {@link ComponentProvider} implementation.<p>
 * Provides the same benefits as a {@link BaseIdentifiedProvider}, plus some utility methods.
 */
public abstract class BaseComponentProvider extends BaseIdentifiedProvider implements ComponentProvider {
    /**
     * Initializes this provider's ID.
     * @param id provider ID
     */
    protected BaseComponentProvider(String id) {
        super(id);
    }

    /**
     * Parses a set of flags into a {@code String} to {@code boolean} map.
     * @param options flag set
     * @param map map to store results in
     */
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
