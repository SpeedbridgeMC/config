package io.github.speedbridgemc.config.processor.api.util;

/**
 * Utilities for working with {@code String}s.
 */
public final class StringUtils {
    private StringUtils() { }

    /**
     * Converts a {@code camelCase} string to {@code UpperCamelCase}.
     * @param s camel-case string
     * @return upper-camel-case string
     */
    public static String camelCaseToUpperCamelCase(String s) {
        if (s.isEmpty())
            return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Converts a {@code camelCase} string to {@code snake_case}.
     * @param s camel-case string
     * @return snake-case string
     */
    public static String camelCaseToSnakeCase(String s) {
        StringBuilder resultBuilder = new StringBuilder();
        for (int i = 0, length = s.length(); i < length; i++) {
            char ch = s.charAt(i);
            if (Character.isUpperCase(ch) && resultBuilder.length() != 0)
                resultBuilder.append('_');
            resultBuilder.append(Character.toLowerCase(ch));
        }
        return resultBuilder.toString();
    }

    /**
     * Converts a {@code snake_case} string to a {@code camelCase} string.
     * @param s snake-case string
     * @return camel-case string
     */
    public static String snakeCaseToCamelCase(String s) {
        StringBuilder resultBuilder = new StringBuilder();
        boolean toUpper = false;
        for (int i = 0, length = s.length(); i < length; i++) {
            char ch = s.charAt(i);
            if (ch == '_') {
                toUpper = true;
                continue;
            }
            if (toUpper)
                ch = Character.toUpperCase(ch);
            resultBuilder.append(ch);
        }
        return resultBuilder.toString();
    }

    /**
     * Strips a number of characters from the start of a string and makes the first character (after stripping) lowercase.
     * @param s original string
     * @param count number of characters to remove
     * @return transformed string
     */
    public static String stripAndLower(String s, int count) {
        if (s.length() > count)
            s = s.substring(count);
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}