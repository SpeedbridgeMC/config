package io.github.speedbridgemc.config.processor.validate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import io.github.speedbridgemc.config.*;
import io.github.speedbridgemc.config.processor.validate.api.BaseValidatorDelegate;
import io.github.speedbridgemc.config.processor.validate.api.ErrorDelegate;
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
    public boolean appendCheck(@NotNull ValidatorContext ctx, @NotNull TypeMirror type, @NotNull String src, @NotNull ErrorDelegate errDelegate, CodeBlock.@NotNull Builder codeBuilder) {
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
                if (ctx.canSet) {
                    if (string)
                        codeBuilder.addStatement("$L = $S", src, "");
                    else if (TypeName.BOOLEAN.equals(typeName))
                        codeBuilder.addStatement("$L = false", src);
                    else
                        codeBuilder.addStatement("$L = 0", src);
                    break;
                }
            case USE_DEFAULT:
                if (ctx.canSet) {
                    codeBuilder.addStatement("$1L = DEFAULTS.$1L", src);
                    break;
                }
            case ERROR:
                codeBuilder.addStatement(errDelegate.generateThrow(" is null!"));
                break;
            }
            codeBuilder.endControlFlow();
            if (string)
                return true;
        }
        IntegerRange integerRange = ctx.getAnnotation(IntegerRange.class);
        if (integerRange != null && integerRange.mode() != EnforceMode.IGNORE) {
            boolean isInteger = TypeName.LONG.equals(typeName);
            long possibleMinI = Long.MIN_VALUE, possibleMaxI = Long.MAX_VALUE;
            if (!isInteger && TypeName.INT.equals(typeName)) {
                isInteger = true;
                possibleMinI = Integer.MIN_VALUE;
                possibleMaxI = Integer.MAX_VALUE;
            }
            if (isInteger) {
                long max = integerRange.max();
                if (max < possibleMinI || max > possibleMaxI)
                    messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                            "Validator: Maximum bound of field is out of its bounds", ctx.getEffectiveElement());
                long min = integerRange.min();
                if (min < possibleMinI || min > possibleMaxI)
                    messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                            "Validator: Minimum bound of field is out of its bounds", ctx.getEffectiveElement());
                if (min >= max) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Validator: Minimum bound of field is larger than or equal to maximum bound", ctx.getEffectiveElement());
                    return true;
                }
                boolean doMax = max != possibleMaxI;
                boolean doMin = min != possibleMinI;
                if (!doMax && !doMin)
                    return true;
                codeBuilder.beginControlFlow(generateRangeCheck(doMax, doMin, integerRange), src, max, min);
                switch (integerRange.mode()) {
                case TRY_FIX:
                    if (ctx.canSet) {
                        if (doMax && doMin)
                            codeBuilder.addStatement("$1L = $4T.min($2L, $4T.max($3L, $1L))", src, max, min, Math.class);
                        else if (doMax)
                            codeBuilder.addStatement("$L = $L", src, max);
                        else // if (doMin)
                            codeBuilder.addStatement("$L = $L", src, min);
                        break;
                    }
                case USE_DEFAULT:
                    if (ctx.canSet) {
                        if (ctx.defaultSrc != null) {
                            codeBuilder.addStatement("$L = $L", src, ctx.defaultSrc);
                            break;
                        }
                    }
                case ERROR:
                    codeBuilder.addStatement(errDelegate.generateThrow(generateErrorDetails(src, max, min, doMax, doMin, integerRange)));
                    break;
                }
                codeBuilder.endControlFlow();
            }
            return true;
        }
        FloatingRange floatingRange = ctx.getAnnotation(FloatingRange.class);
        if (floatingRange != null && floatingRange.mode() != EnforceMode.IGNORE) {
            boolean isFloating = TypeName.DOUBLE.equals(typeName);
            double possibleMaxF = Double.MAX_VALUE;
            if (!isFloating && TypeName.FLOAT.equals(typeName)) {
                isFloating = true;
                possibleMaxF = Float.MAX_VALUE;
            }
            double possibleMinF = -possibleMaxF;
            if (isFloating) {
                double max = floatingRange.max();
                if (max < possibleMinF || max > possibleMaxF)
                    messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                            "Validator: Maximum bound of field is out of its bounds", ctx.getEffectiveElement());
                double min = floatingRange.min();
                if (min < possibleMinF || min > possibleMaxF)
                    messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                            "Validator: Minimum bound of field is out of its bounds", ctx.getEffectiveElement());
                if (min >= max) {
                    messager.printMessage(Diagnostic.Kind.ERROR,
                            "Validator: Minimum bound of field is larger than or equal to maximum bound", ctx.getEffectiveElement());
                    return true;
                }
                boolean doMax = max != possibleMaxF;
                boolean doMin = min != possibleMinF;
                if (!doMax && !doMin)
                    return true;
                codeBuilder.beginControlFlow(generateRangeCheck(doMax, doMin, floatingRange), src, max, min);
                switch (floatingRange.mode()) {
                case TRY_FIX:
                    if (ctx.canSet) {
                        if (doMax && doMin)
                            codeBuilder.addStatement("$1L = $4T.min($2L$5L, $4T.max($3L$5L, $1L))", src, max, min, Math.class,
                                    possibleMaxF == Float.MAX_VALUE ? "f" : "");
                        else if (doMax)
                            codeBuilder.addStatement("$L = $L", src, max);
                        else // if (doMin)
                            codeBuilder.addStatement("$L = $L", src, min);
                        break;
                    }
                case USE_DEFAULT:
                    if (ctx.canSet) {
                        if (ctx.defaultSrc != null) {
                            codeBuilder.addStatement("$L = $L", src, ctx.defaultSrc);
                            break;
                        }
                    }
                case ERROR:
                    codeBuilder.addStatement(errDelegate.generateThrow(generateErrorDetails(src, max, min, doMax, doMin, floatingRange)));
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
        if (doMin) {
            cond.append("$1L <");
            if (minMode == RangeMode.EXCLUSIVE)
                cond.append('=');
            cond.append(" $3L");
            if (doMax)
                cond.append(" || ");
        }
        if (doMax) {
            cond.append("$1L >");
            if (maxMode == RangeMode.EXCLUSIVE)
                cond.append('=');
            cond.append(" $2L");

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

    private @NotNull CodeBlock generateErrorDetails(@NotNull String src,
                                                    double max, double min, boolean doMax, boolean doMin,
                                                    @NotNull RangeMode maxMode, @NotNull RangeMode minMode) {
        StringBuilder detailsBuilder = new StringBuilder(" is out of bounds! Should be ");
        if (doMax && doMin) {
            detailsBuilder.append("between ").append(DECIMAL_FORMAT.format(min))
                    .append(' ').append(minMode == RangeMode.EXCLUSIVE ? "(exclusive)" : "(inclusive)")
                    .append(" and ").append(DECIMAL_FORMAT.format(max))
                    .append(' ').append(maxMode == RangeMode.EXCLUSIVE ? "(exclusive)" : "(inclusive)");
        } else if (doMax)
            detailsBuilder.append("smaller than ")
                    .append(maxMode == RangeMode.EXCLUSIVE ? "" : " or equal to ")
                    .append(DECIMAL_FORMAT.format(max));
        else if (doMin)
            detailsBuilder.append("larger than ")
                    .append(minMode == RangeMode.EXCLUSIVE ? "" : " or equal to ")
                    .append(DECIMAL_FORMAT.format(min));
        detailsBuilder.append(", but is ");
        return CodeBlock.builder()
                .add("$S", detailsBuilder.toString())
                .add(" + $L", src)
                .build();
    }

    private @NotNull CodeBlock generateErrorDetails(@NotNull String src,
                                                    long max, long min, boolean doMax, boolean doMin,
                                                    @NotNull IntegerRange integerRange) {
        return generateErrorDetails(src, max, min, doMax, doMin, integerRange.maxMode(), integerRange.minMode());
    }

    private @NotNull CodeBlock generateErrorDetails(@NotNull String src,
                                                    double max, double min, boolean doMax, boolean doMin,
                                                    @NotNull FloatingRange floatingRange) {
        return generateErrorDetails(src, max, min, doMax, doMin, floatingRange.maxMode(), floatingRange.minMode());
    }
}
