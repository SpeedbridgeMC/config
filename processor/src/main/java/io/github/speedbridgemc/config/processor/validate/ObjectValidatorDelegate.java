package io.github.speedbridgemc.config.processor.validate;

import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.EnforceMode;
import io.github.speedbridgemc.config.EnforceNotNull;
import io.github.speedbridgemc.config.processor.validate.api.BaseValidatorDelegate;
import io.github.speedbridgemc.config.processor.validate.api.ErrorDelegate;
import io.github.speedbridgemc.config.processor.validate.api.ValidatorContext;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.type.TypeMirror;

public final class ObjectValidatorDelegate extends BaseValidatorDelegate {
    @Override
    public boolean appendCheck(@NotNull ValidatorContext ctx, @NotNull TypeMirror type, @NotNull String src, @NotNull ErrorDelegate errDelegate, CodeBlock.@NotNull Builder codeBuilder) {
        EnforceNotNull enforceNotNull = ctx.getAnnotation(EnforceNotNull.class);
        if (enforceNotNull != null && enforceNotNull.value() != EnforceMode.IGNORE) {
            codeBuilder.beginControlFlow("if ($L == null)", src);
            switch (enforceNotNull.value()) {
            case TRY_FIX:
            case USE_DEFAULT:
                if (ctx.defaultSrc != null) {
                    codeBuilder.addStatement("$L = $L", src, ctx.defaultSrc);
                    break;
                }
            case ERROR:
                codeBuilder.add(errDelegate.generateThrow(" is null!"));
                break;
            }
            codeBuilder.endControlFlow();
        }
        return true;
    }
}
