package io.github.speedbridgemc.config.processor.validate;

import com.squareup.javapoet.CodeBlock;
import io.github.speedbridgemc.config.EnforceMode;
import io.github.speedbridgemc.config.EnforceNotNull;
import io.github.speedbridgemc.config.processor.api.StringUtils;
import io.github.speedbridgemc.config.processor.api.TypeUtils;
import io.github.speedbridgemc.config.processor.validate.api.BaseValidatorDelegate;
import io.github.speedbridgemc.config.processor.validate.api.ErrorDelegate;
import io.github.speedbridgemc.config.processor.validate.api.ValidatorContext;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.List;

public final class ListValidatorDelegate extends BaseValidatorDelegate {
    private TypeMirror listTM;
    private int nestCount;

    @Override
    public void init(@NotNull ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        listTM = TypeUtils.getTypeMirror(processingEnv, List.class.getCanonicalName());
        if (listTM != null)
            listTM = types.erasure(listTM);
        nestCount = 1;
    }

    @Override
    public boolean appendCheck(@NotNull ValidatorContext ctx, @NotNull TypeMirror type, @NotNull String src, @NotNull ErrorDelegate errDelegate, CodeBlock.@NotNull Builder codeBuilder) {
        TypeMirror componentType = null;
        if (type.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) type;
            if (listTM == null)
                return false;
            TypeMirror erasedType = types.erasure(declaredType);
            if (types.isSameType(erasedType, listTM)) {
                List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                if (typeArguments.size() == 0) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Serializer: Raw lists are unsupported", ctx.getEffectiveElement());
                    return false;
                }
                componentType = typeArguments.get(0);
            }
        }
        if (componentType == null)
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

        String simpleName = StringUtils.titleCase(ctx.getEffectiveElement().getSimpleName().toString());
        String indexSrc = "index" + simpleName + "_" + nestCount;
        String sizeSrc = "size" + simpleName + "_" + nestCount;
        String compSrc = "comp" + simpleName + "_" + nestCount;
        nestCount++;

        CodeBlock.Builder checksBuilder = CodeBlock.builder();
        Element elementBackup = ctx.element, enclosingElementBackup = ctx.enclosingElement;

        ctx.appendCheck(componentType, compSrc, errDelegate.derive((details, desc) -> CodeBlock.builder()
                .add("throw new $T(", IllegalArgumentException.class)
                .add("$S", '"' + desc + '[')
                .add(" + $L + ", indexSrc)
                .add("$S", "]\"")
                .add(" + ")
                .add(details)
                .add(")")
                .build()), checksBuilder);

        nestCount--;

        if (!checksBuilder.isEmpty()) {
            codeBuilder
                    .beginControlFlow("for (int $1L = 0, $2L = $3L.size(); $1L < $2L; $1L++)",
                            indexSrc, sizeSrc, src)
                    .addStatement("$T $L = $L.get($L)", componentType, compSrc, src, indexSrc)
                    .add(checksBuilder.build())
                    .addStatement("$L.set($L, $L)", src, indexSrc, compSrc)
                    .endControlFlow();
        }

        return true;
    }
}
