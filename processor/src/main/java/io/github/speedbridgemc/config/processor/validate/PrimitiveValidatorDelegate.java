package io.github.speedbridgemc.config.processor.validate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.*;
import io.github.speedbridgemc.config.processor.validate.api.BaseValidatorDelegate;
import io.github.speedbridgemc.config.processor.validate.api.ValidatorContext;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class PrimitiveValidatorDelegate extends BaseValidatorDelegate {
    private static final ClassName STRING_TYPE = ClassName.get(String.class);
    private static final DecimalFormat DECIMAL_FORMAT;

    static {
        DECIMAL_FORMAT = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ROOT));
        DECIMAL_FORMAT.setMaximumFractionDigits(340); // DecimalFormat.DOUBLE_FRACTION_DIGITS
    }

    @Override
    public boolean appendCheck(@NotNull ValidatorContext ctx, @NotNull TypeMirror type, @NotNull String src, @NotNull String description, CodeBlock.@NotNull Builder codeBuilder) {
        TypeName typeName = TypeName.get(type);
        boolean string = STRING_TYPE.equals(typeName);
        boolean boxed = typeName.isBoxedPrimitive();
        if (boxed)
            typeName = typeName.unbox();
        else if (!string && !typeName.isPrimitive())
            return false;
        if (string || boxed) {
            EnforceNotNull enforceNotNull = ctx.getAnnotation(EnforceNotNull.class);
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
            case USE_DEFAULT:
                codeBuilder.addStatement("$1L = DEFAULTS.$1L", src);
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
            IntegerRange integerRange = ctx.getAnnotation(IntegerRange.class);
            if (integerRange != null && integerRange.mode() != EnforceMode.IGNORE) {
                long max = integerRange.max();
                if (smol && (max < Integer.MIN_VALUE || max > Integer.MAX_VALUE))
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                            "Validator: Maximum bound of field is out of its bounds", ctx.getEffectiveElement());
                long min = integerRange.min();
                if (smol && (min < Integer.MIN_VALUE || min > Integer.MAX_VALUE))
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                            "Validator: Minimum bound of field is out of its bounds", ctx.getEffectiveElement());
                if (min > max) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Validator: Minimum bound of field is larger than maximum bound", ctx.getEffectiveElement());
                    return true;
                }
                boolean doMax = (smol && max != Integer.MAX_VALUE) || max != Long.MAX_VALUE;
                boolean doMin = (smol && min != Integer.MIN_VALUE) || min != Long.MIN_VALUE;
                if (!doMax && !doMin)
                    return true;
                codeBuilder.beginControlFlow(generateRangeCheck(doMax, doMin, integerRange), src, max, min);
                switch (integerRange.mode()) {
                case TRY_FIX:
                    if (doMax && doMin)
                        codeBuilder.addStatement("$1L = $4T.min($2L, $4T.max($3L, $1L)", src, max, min, Math.class);
                    else if (doMax)
                        codeBuilder.addStatement("$L = $L", src, max);
                    else // if (doMin)
                        codeBuilder.addStatement("$L = $L", src, min);
                    break;
                case USE_DEFAULT:
                    if (ctx.canUseDefaults) {
                        codeBuilder.addStatement("$1L = DEFAULTS.$1L", src);
                        break;
                    }
                case ERROR:
                    String errDetails;
                    if (doMax && doMin)
                        errDetails = "between %1$d and %2$d";
                    else if (doMax)
                        errDetails = "smaller than %1$d";
                    else // if (doMin)
                        errDetails = "larger than %2$d";
                    errDetails = String.format(errDetails, min, max);
                    codeBuilder.addStatement("throw new $T(\"\\\"$L\\\"$L\" + $L)",
                            IllegalArgumentException.class,
                            description,
                            String.format(" is out of bounds! Should be %s, but is ", errDetails),
                            src);
                    break;
                }
                codeBuilder.endControlFlow();
            }
            return true;
        }
        smol = TypeName.FLOAT.equals(typeName);
        if (smol || TypeName.DOUBLE.equals(typeName)) {
            FloatingRange floatingRange = ctx.getAnnotation(FloatingRange.class);
            if (floatingRange != null && floatingRange.mode() != EnforceMode.IGNORE) {
                double max = floatingRange.max();
                if (smol && (max < -Float.MAX_VALUE || max > Float.MAX_VALUE))
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                            "Validator: Maximum bound of field is out of its bounds", ctx.getEffectiveElement());
                double min = floatingRange.min();
                if (smol && (min < -Float.MAX_VALUE || min > Float.MAX_VALUE))
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                            "Validator: Minimum bound of field is out of its bounds", ctx.getEffectiveElement());
                if (min > max) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Validator: Minimum bound of field is larger than maximum bound", ctx.getEffectiveElement());
                    return true;
                }
                boolean doMax = (smol && max != Float.MAX_VALUE) || max != Double.MAX_VALUE;
                boolean doMin = (smol && min != -Float.MAX_VALUE) || min != -Double.MAX_VALUE;
                if (!doMax && !doMin)
                    return true;
                codeBuilder.beginControlFlow(generateRangeCheck(doMax, doMin, floatingRange), src, max, min);
                switch (floatingRange.mode()) {
                case TRY_FIX:
                    if (doMax && doMin)
                        codeBuilder.addStatement("$1L = $4T.min($2L, $4T.max($3L, $1L)", src, max, min, Math.class);
                    else if (doMax)
                        codeBuilder.addStatement("$L = $L", src, max);
                    else // if (doMin)
                        codeBuilder.addStatement("$L = $L", src, min);
                    break;
                case USE_DEFAULT:
                    if (ctx.canUseDefaults) {
                        codeBuilder.addStatement("$1L = DEFAULTS.$1L", src);
                        break;
                    }
                case ERROR:
                    StringBuilder errDetails = new StringBuilder();
                    if (doMax && doMin)
                        errDetails.append("between ").append(DECIMAL_FORMAT.format(min))
                                .append(" and ").append(DECIMAL_FORMAT.format(max));
                    else if (doMax)
                        errDetails.append("smaller than ").append(DECIMAL_FORMAT.format(max));
                    else // if (doMin)
                        errDetails.append("larger than ").append(DECIMAL_FORMAT.format(min));
                    codeBuilder.addStatement("throw new $T(\"\\\"$L\\\"$L\" + $L)",
                            IllegalArgumentException.class,
                            description,
                            String.format(" is out of bounds! Should be %s, but is ", errDetails.toString()),
                            src);
                    break;
                }
                codeBuilder.endControlFlow();
            }
            return true;
        }
        return TypeName.BOOLEAN.equals(typeName);
    }

    private @NotNull String generateRangeCheck(boolean doMax, boolean doMin,
                                               @NotNull RangeMode maxMode, @NotNull RangeMode minMode) {
        StringBuilder cond = new StringBuilder("if (");
        if (doMax) {
            cond.append("$1L >");
            if (maxMode == RangeMode.EXCLUSIVE)
                cond.append('=');
            cond.append(" $2L");
            if (doMin)
                cond.append(" || ");
        }
        if (doMin) {
            cond.append("$1L <");
            if (minMode == RangeMode.EXCLUSIVE)
                cond.append('=');
            cond.append(" $3L");
        }
        cond.append(')');
        return cond.toString();
    }

    private @NotNull String generateRangeCheck(boolean doMax, boolean doMin, @NotNull IntegerRange integerRange) {
        return generateRangeCheck(doMax, doMin, integerRange.maxMode(), integerRange.minMode());
    }

    private @NotNull String generateRangeCheck(boolean doMax, boolean doMin, @NotNull FloatingRange floatingRange) {
        return generateRangeCheck(doMax, doMin, floatingRange.maxMode(), floatingRange.minMode());
    }
}
