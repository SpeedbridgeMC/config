package io.github.speedbridgemc.config.processor.api.util;

import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

public final class MirrorElementPair {
    private final TypeMirror mirror;
    private final Element element;

    public MirrorElementPair(TypeMirror mirror, Element element) {
        this.mirror = mirror;
        this.element = element;
    }

    public static MirrorElementPair create(Types types, DeclaredType containing, Element element) {
        return new MirrorElementPair(types.asMemberOf(containing, element), element);
    }

    public TypeMirror mirror() {
        return mirror;
    }

    public Element element() {
        return element;
    }
}
