package io.github.speedbridgemc.config.processor.api.util;

import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

public final class MirrorElementPair {
    private final @NotNull TypeMirror mirror;
    private final @NotNull Element element;

    public MirrorElementPair(@NotNull TypeMirror mirror, @NotNull Element element) {
        this.mirror = mirror;
        this.element = element;
    }

    public static @NotNull MirrorElementPair create(@NotNull Types types, @NotNull DeclaredType containing, @NotNull Element element) {
        return new MirrorElementPair(types.asMemberOf(containing, element), element);
    }

    public @NotNull TypeMirror mirror() {
        return mirror;
    }

    public @NotNull Element element() {
        return element;
    }
}
