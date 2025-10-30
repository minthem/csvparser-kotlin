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

class IntCsvConverter(
    locale: Locale = Locale.getDefault(),
    pattern: String = "#",
) : NumericConverter<Int>(locale, pattern) {
    override fun cast(value: BigDecimal): Int = value.setScale(0, RoundingMode.DOWN).intValueExact()
}

class LongCsvConverter(
    locale: Locale = Locale.getDefault(),
    pattern: String = "#",
) : NumericConverter<Long>(locale, pattern) {
    override fun cast(value: BigDecimal): Long = value.setScale(0, RoundingMode.DOWN).longValueExact()
}

class ShortCsvConverter(
    locale: Locale = Locale.getDefault(),
    pattern: String = "#",
) : NumericConverter<Short>(locale, pattern) {
    override fun cast(value: BigDecimal): Short = value.setScale(0, RoundingMode.DOWN).shortValueExact()
}

class ByteCsvConverter(
    locale: Locale = Locale.getDefault(),
    pattern: String = "#",
) : NumericConverter<Byte>(locale, pattern) {
    override fun cast(value: BigDecimal): Byte = value.setScale(0, RoundingMode.DOWN).byteValueExact()
}

class FloatCsvConverter(
    locale: Locale = Locale.getDefault(),
    pattern: String = "#.###",
) : NumericConverter<Float>(locale, pattern) {
    override fun cast(value: BigDecimal): Float = value.toFloat()
}

class DoubleCsvConverter(
    locale: Locale = Locale.getDefault(),
    pattern: String = "#.######",
) : NumericConverter<Double>(locale, pattern) {
    override fun cast(value: BigDecimal): Double = value.toDouble()
}

class BigDecimalCsvConverter(
    locale: Locale = Locale.getDefault(),
    pattern: String = "#.###########",
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
}
