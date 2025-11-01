package io.github.minthem.converter

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParsePosition
import java.util.Locale

abstract class NumericConverter<T : Number>(
    locale: Locale,
    format: String,
) : LocalizedCsvConverter<T>(locale, format) {
    protected abstract fun cast(value: BigDecimal): T

    override fun deserialize(value: String?): Result<T?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            cast(convertNumber(value))
        }
    }

    override fun serialize(value: T?): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            if (pattern.isNotBlank()) {
                getDecimalFormat().format(value)
            } else {
                value.toString()
            }
        }
    }

    protected fun convertNumber(value: String): BigDecimal {
        val trimmed = value.trim()
        val formatter = getDecimalFormat()
        val pos = ParsePosition(0)
        val parsed = formatter.parse(trimmed, pos) as? BigDecimal

        if (pos.index != trimmed.length || parsed == null) {
            throw NumberFormatException("Invalid number: \"$value\"")
        }

        val rounded =
            if (pattern.isNotBlank()) {
                val scale = formatter.maximumFractionDigits
                parsed.setScale(scale, RoundingMode.HALF_UP)
            } else {
                parsed
            }

        return rounded
    }

    protected fun getDecimalFormat(): DecimalFormat {
        val symbols = DecimalFormatSymbols(locale)
        return DecimalFormat(pattern, symbols).apply {
            isParseBigDecimal = true
        }
    }
}

class IntCsvConverter private constructor(
    locale: Locale,
    pattern: String,
) : NumericConverter<Int>(locale, pattern) {
    override fun cast(value: BigDecimal): Int = value.setScale(0, RoundingMode.DOWN).intValueExact()

    class Builder : LocalizedCsvConverterBuilder<Int>("#") {
        override fun build(): IntCsvConverter = IntCsvConverter(locale, pattern)
    }
}

class LongCsvConverter private constructor(
    locale: Locale,
    pattern: String,
) : NumericConverter<Long>(locale, pattern) {
    override fun cast(value: BigDecimal): Long = value.setScale(0, RoundingMode.DOWN).longValueExact()

    class Builder : LocalizedCsvConverterBuilder<Long>("#") {
        override fun build(): LongCsvConverter = LongCsvConverter(locale, pattern)
    }
}

class ShortCsvConverter private constructor(
    locale: Locale,
    pattern: String,
) : NumericConverter<Short>(locale, pattern) {
    override fun cast(value: BigDecimal): Short = value.setScale(0, RoundingMode.DOWN).shortValueExact()

    class Builder : LocalizedCsvConverterBuilder<Short>("#") {
        override fun build(): ShortCsvConverter = ShortCsvConverter(locale, pattern)
    }
}

class ByteCsvConverter private constructor(
    locale: Locale,
    pattern: String,
) : NumericConverter<Byte>(locale, pattern) {
    override fun cast(value: BigDecimal): Byte = value.setScale(0, RoundingMode.DOWN).byteValueExact()

    class Builder : LocalizedCsvConverterBuilder<Byte>("#") {
        override fun build(): ByteCsvConverter = ByteCsvConverter(locale, pattern)
    }
}

class FloatCsvConverter private constructor(
    locale: Locale,
    pattern: String,
) : NumericConverter<Float>(locale, pattern) {
    override fun cast(value: BigDecimal): Float = value.toFloat()

    class Builder : LocalizedCsvConverterBuilder<Float>("#.###") {
        override fun build(): FloatCsvConverter = FloatCsvConverter(locale, pattern)
    }
}

class DoubleCsvConverter private constructor(
    locale: Locale,
    pattern: String,
) : NumericConverter<Double>(locale, pattern) {
    override fun cast(value: BigDecimal): Double = value.toDouble()

    class Builder : LocalizedCsvConverterBuilder<Double>("#.######") {
        override fun build(): DoubleCsvConverter = DoubleCsvConverter(locale, pattern)
    }
}

class BigDecimalCsvConverter private constructor(
    locale: Locale,
    pattern: String,
) : NumericConverter<BigDecimal>(locale, pattern) {
    override fun cast(value: BigDecimal): BigDecimal = value

    override fun serialize(value: BigDecimal?): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            if (pattern.isNotBlank()) {
                getDecimalFormat().format(value)
            } else {
                value.toPlainString()
            }
        }
    }

    class Builder : LocalizedCsvConverterBuilder<BigDecimal>("#.###########") {
        override fun build(): BigDecimalCsvConverter = BigDecimalCsvConverter(locale, pattern)
    }
}
