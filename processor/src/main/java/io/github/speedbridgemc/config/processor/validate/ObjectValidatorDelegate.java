package io.github.speedbridgemc.config.processor.validate;

import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.EnforceMode;
import io.github.speedbridgemc.config.EnforceNotNull;
import io.github.speedbridgemc.config.processor.validate.api.BaseValidatorDelegate;
import io.github.speedbridgemc.config.processor.validate.api.ValidatorContext;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

public final class ObjectValidatorDelegate extends BaseValidatorDelegate {
    @Override
    public boolean appendCheck(@NotNull ValidatorContext ctx, @NotNull TypeMirror type, @NotNull String src, @NotNull String description, CodeBlock.@NotNull Builder codeBuilder) {
        VariableElement effectiveFieldElement = ctx.getEffectiveFieldElement();
        EnforceNotNull enforceNotNull = effectiveFieldElement.getAnnotation(EnforceNotNull.class);
        if (enforceNotNull != null && enforceNotNull.value() != EnforceMode.IGNORE) {
            codeBuilder
                    .beginControlFlow("if ($L == null)", src)
                    .addStatement("throw new $T($S)",
                            IllegalArgumentException.class, String.format("\"%s\" is null!", description))
                    .endControlFlow();
        }
        return true;
    }
}
