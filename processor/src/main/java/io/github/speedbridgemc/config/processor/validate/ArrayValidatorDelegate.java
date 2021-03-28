package io.github.speedbridgemc.config.processor.validate;

import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.EnforceMode;
import io.github.speedbridgemc.config.EnforceNotNull;
import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.validate.api.BaseValidatorDelegate;
import io.github.speedbridgemc.config.processor.validate.api.ErrorDelegate;
import io.github.speedbridgemc.config.processor.validate.api.ValidatorContext;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public final class ArrayValidatorDelegate extends BaseValidatorDelegate {
    @Override
    public boolean appendCheck(@NotNull ValidatorContext ctx, @NotNull TypeMirror type, @NotNull String src, @NotNull ErrorDelegate errDelegate, CodeBlock.@NotNull Builder codeBuilder) {
        if (type.getKind() != TypeKind.ARRAY)
            return false;

        EnforceNotNull enforceNotNull = ctx.getAnnotation(EnforceNotNull.class);
        if (enforceNotNull != null && enforceNotNull.value() != EnforceMode.IGNORE) {
            codeBuilder.beginControlFlow("if ($L == null)", src);
            switch (enforceNotNull.value()) {
            case TRY_FIX:
            case USE_DEFAULT:
                if (ctx.defaultSrc != null) {
                    codeBuilder.addStatement("$L = $L.clone()", src, ctx.defaultSrc);
                    break;
                }
            case ERROR:
                codeBuilder.addStatement(errDelegate.generateThrow(" is null!"));
                break;
            }
            codeBuilder.endControlFlow();
        }

        TypeMirror componentType = ((ArrayType) type).getComponentType();

        String indexSrc = "index" + StringUtils.titleCase(ctx.getEffectiveElement().getSimpleName().toString()) + "_" + ctx.nestingFactor++;

        CodeBlock.Builder checksBuilder = CodeBlock.builder();

        ctx.appendCheck(componentType, src + "[" + indexSrc + "]", errDelegate.derive((details, desc) -> CodeBlock.builder()
                .add("throw new $T(", IllegalArgumentException.class)
                .add("$S", '"' + desc + '[')
                .add(" + $L + ", indexSrc)
                .add("$S", "]\"")
                .add(" + ")
                .add(details)
                .add(")")
                .build()), checksBuilder);

        ctx.nestingFactor--;

        if (!checksBuilder.isEmpty()) {
            codeBuilder
                    .beginControlFlow("for (int $1L = 0; $1L < $2L.length; $1L++)", indexSrc, src)
                    .add(checksBuilder.build())
                    .endControlFlow();
        }

        return true;
    }
}
