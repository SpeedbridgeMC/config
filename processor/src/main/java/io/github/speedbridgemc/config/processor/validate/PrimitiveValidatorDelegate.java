package io.github.speedbridgemc.config.processor.validate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.EnforceMode;
import io.github.speedbridgemc.config.EnforceNotNull;
import io.github.speedbridgemc.config.FloatingRange;
import io.github.speedbridgemc.config.IntegerRange;
import io.github.speedbridgemc.config.processor.validate.api.BaseValidatorDelegate;
import io.github.speedbridgemc.config.processor.validate.api.ValidatorContext;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

public final class PrimitiveValidatorDelegate extends BaseValidatorDelegate {
    private static final ClassName STRING_TYPE = ClassName.get(String.class);

    @Override
    public boolean appendCheck(@NotNull ValidatorContext ctx, @NotNull TypeMirror type, @NotNull String src, @NotNull String description, CodeBlock.@NotNull Builder codeBuilder) {
        TypeName typeName = TypeName.get(type);
        boolean string = STRING_TYPE.equals(typeName);
        boolean boxed = typeName.isBoxedPrimitive();
        if (boxed)
            typeName = typeName.unbox();
        else if (!string && !typeName.isPrimitive())
            return false;
        VariableElement effectiveFieldElement = ctx.getEffectiveFieldElement();
        if (string || boxed) {
            EnforceNotNull enforceNotNull = effectiveFieldElement.getAnnotation(EnforceNotNull.class);
            if (enforceNotNull == null || enforceNotNull.value() == EnforceMode.IGNORE)
                return true;
            codeBuilder.beginControlFlow("if ($L == null)", src);
            switch (enforceNotNull.value()) {
            case TRY_FIX:
                if (string)
                    codeBuilder.addStatement("$L = $S", src, "");
                else if (TypeName.BOOLEAN.equals(typeName))
                    codeBuilder.addStatement("$L = false", src);
                else
                    codeBuilder.addStatement("$L = 0", src);
                break;
            case ERROR:
                codeBuilder.addStatement("throw new $T($S)",
                        IllegalArgumentException.class, String.format("\"%s\" is null!", description));
                break;
            }
            codeBuilder.endControlFlow();
        }
        boolean smol = TypeName.INT.equals(typeName);
        if (smol || TypeName.LONG.equals(typeName)) {
            IntegerRange integerRange = effectiveFieldElement.getAnnotation(IntegerRange.class);
            if (integerRange != null && integerRange.mode() != EnforceMode.IGNORE) {
                long max = integerRange.max();
                if (smol && (max < Integer.MIN_VALUE || max > Integer.MAX_VALUE))
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                            "Validator: Maximum bound of field is out of its bounds", effectiveFieldElement);
                long min = integerRange.min();
                if (smol && (min < Integer.MIN_VALUE || min > Integer.MAX_VALUE))
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                            "Validator: Minimum bound of field is out of its bounds", effectiveFieldElement);
                boolean doMin = (smol && min != Integer.MIN_VALUE) || min != Long.MIN_VALUE;
                if ((smol && max != Integer.MAX_VALUE) || max != Long.MAX_VALUE) {
                    codeBuilder.beginControlFlow("if ($L > $L)", src, max);
                    switch (integerRange.mode()) {
                    case TRY_FIX:
                        codeBuilder.addStatement("$L = $L", src, max);
                        break;
                    case ERROR:
                        codeBuilder.addStatement("throw new $T($S + $L)",
                                IllegalArgumentException.class,
                                String.format("\"%s\" is too big! Should be smaller than %s, but is", description, max));
                        break;
                    }
                    if (doMin)
                        codeBuilder.nextControlFlow("else if ($L < $L)", src, min);
                } else if (doMin)
                    codeBuilder.beginControlFlow("if ($L < $L)", src, min);
                if (doMin) {
                    switch (integerRange.mode()) {
                    case TRY_FIX:
                        codeBuilder.addStatement("$L = $L", src, min);
                        break;
                    case ERROR:
                        codeBuilder.addStatement("throw new $T($S + $L)",
                                IllegalArgumentException.class,
                                String.format("\"%s\" is too small! Should be bigger than %s, but is", description, min));
                        break;
                    }
                }
                codeBuilder.endControlFlow();
            }
            return true;
        }
        smol = TypeName.FLOAT.equals(typeName);
        if (smol || TypeName.DOUBLE.equals(typeName)) {
            FloatingRange floatingRange = effectiveFieldElement.getAnnotation(FloatingRange.class);
            if (floatingRange != null && floatingRange.mode() != EnforceMode.IGNORE) {
                double max = floatingRange.max();
                if (smol && (max > Float.MAX_VALUE))
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                            "Validator: Maximum bound of field is out of its bounds", effectiveFieldElement);
                double min = floatingRange.min();
                if (smol && (min > Float.MAX_VALUE))
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                            "Validator: Minimum bound of field is out of its bounds", effectiveFieldElement);
                boolean doMin = (smol && min != Integer.MIN_VALUE) || min != Long.MIN_VALUE;
                if ((smol && max != Float.MAX_VALUE) || max != Double.MAX_VALUE) {
                    codeBuilder.beginControlFlow("if ($L > $L)", src, max);
                    switch (floatingRange.mode()) {
                    case TRY_FIX:
                        codeBuilder.addStatement("$L = $L", src, max);
                        break;
                    case ERROR:
                        codeBuilder.addStatement("throw new $T($S + $L)",
                                IllegalArgumentException.class,
                                String.format("\"%s\" is too big! Should be smaller than %s, but is", description, max),
                                src);
                        break;
                    }
                    if (doMin)
                        codeBuilder.nextControlFlow("else if ($L < $L)", src, min);
                } else if (doMin)
                    codeBuilder.beginControlFlow("if ($L < $L)", src, min);
                if (doMin) {
                    switch (floatingRange.mode()) {
                    case TRY_FIX:
                        codeBuilder.addStatement("$L = $L", src, min);
                        break;
                    case ERROR:
                        codeBuilder.addStatement("throw new $T($S + $L)",
                                IllegalArgumentException.class,
                                String.format("\"%s\" is too small! Should be bigger than %s, but is", description, min),
                                src);
                        break;
                    }
                }
                codeBuilder.endControlFlow();
            }
            return true;
        }
        return TypeName.BOOLEAN.equals(typeName);
    }
}
