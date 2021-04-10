package io.github.speedbridgemc.config.processor.api;

import org.jetbrains.annotations.NotNull;

/**
 * A {@link Provider} with an identifier.
 */
public interface IdentifiedProvider extends Provider {
    /**
     * Gets this provider's ID.<p>
     * This method should always return the same constant value.
     * @return provider ID.
     */
    @NotNull String getId();
}
