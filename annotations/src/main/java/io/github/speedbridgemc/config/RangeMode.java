package io.github.speedbridgemc.config;

/**
 * Defines how to enforce a range constraint.
 */
public enum RangeMode {
    /**
     * <em>Inclusive</em> enforcement, as in the limit itself will be <em>included</em> in the range.
     */
    INCLUSIVE,
    /**
     * <em>Exclusive</em> enforcement, as in the limit itself will be <em>excluded</em> from the range.
     */
    EXCLUSIVE
}
