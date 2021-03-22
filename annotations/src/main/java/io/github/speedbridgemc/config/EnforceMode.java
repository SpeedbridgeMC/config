package io.github.speedbridgemc.config;

/**
 * Defines how to enforce a validation constraint.
 */
public enum EnforceMode {
    /**
     * Simply ignore the constraint.<br>
     * Effectively acts as if the validation constraint hasn't been applied at all.
     */
    IGNORE,
    /**
     * Try to fix the value so it matches the constraint.<p>
     * This is constraint-dependent, and may {@linkplain #USE_DEFAULT reset the value to its default} if the constraint
     * doesn't support fixing.
     */
    TRY_FIX,
    /**
     * Reset the value to its default.<p>
     * Constraints may fall back to this if the user specified to {@linkplain #TRY_FIX fix the value}, but
     * they don't support a special "fixing" operation.
     */
    USE_DEFAULT,
    /**
     * Throw an error. This implies that the entire configuration is invalid!
     */
    ERROR;
}
