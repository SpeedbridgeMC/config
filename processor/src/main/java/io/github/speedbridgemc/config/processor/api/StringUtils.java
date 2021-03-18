package io.github.speedbridgemc.config.processor.api;

import org.jetbrains.annotations.NotNull;

public final class StringUtils {
    private StringUtils() { }

    public static @NotNull String titleCase(@NotNull String s) {
        if (s.isEmpty())
            return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static @NotNull String camelCaseToScreamingSnakeCase(@NotNull String s) {
        StringBuilder resultBuilder = new StringBuilder();
        for (int i = 0, length = s.length(); i < length; i++) {
            char ch = s.charAt(i);
            if (Character.isUpperCase(ch) && resultBuilder.length() != 0)
                resultBuilder.append('_');
            resultBuilder.append(Character.toUpperCase(ch));
        }
        return resultBuilder.toString();
    }
}
