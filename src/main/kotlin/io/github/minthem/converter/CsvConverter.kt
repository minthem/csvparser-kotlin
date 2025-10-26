package io.github.minthem.converter

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParsePosition
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.reflect.KClass

interface CsvConverter<T> {
    fun deserialize(value: String?): Result<T?>

    fun serialize(value: T?): Result<String?>
}

abstract class AbstractCsvConverter<T>(
    protected val locale: Locale,
    protected val pattern: String,
) : CsvConverter<T>

private fun getDecimalFormat(
    locale: Locale,
    pattern: String,
): DecimalFormat {
    val symbols = DecimalFormatSymbols(locale)
    return DecimalFormat(pattern, symbols).apply {
        isParseBigDecimal = true
    }
}

internal fun <T : Number> convertNumber(
    type: KClass<T>,
    value: String,
    locale: Locale,
    pattern: String,
): T {
    val trimmed = value.trim()
    val formatter = getDecimalFormat(locale, pattern)
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

    @Suppress("UNCHECKED_CAST")
    return try {
        when (type) {
            Int::class -> rounded.setScale(0, RoundingMode.DOWN).intValueExact()
            Long::class -> rounded.setScale(0, RoundingMode.DOWN).longValueExact()
            Short::class -> rounded.setScale(0, RoundingMode.DOWN).shortValueExact()
            Byte::class -> rounded.setScale(0, RoundingMode.DOWN).byteValueExact()
            Float::class -> rounded.toFloat()
            Double::class -> rounded.toDouble()
            BigDecimal::class -> rounded
            else -> throw IllegalArgumentException("Unsupported number type: $type")
        } as T
    } catch (_: ArithmeticException) {
        throw ArithmeticException("Value out of range for $type: \"$value\"")
    }
}

class IntCsvConverter(
    locale: Locale = Locale.getDefault(),
    pattern: String = "#",
) : AbstractCsvConverter<Int>(locale, pattern) {
    override fun deserialize(value: String?): Result<Int?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            convertNumber(Int::class, value, locale, pattern)
        }
    }

    override fun serialize(value: Int?): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            if (pattern.isNotBlank()) {
                getDecimalFormat(locale, pattern).format(value)
            } else {
                value.toString()
            }
        }
    }
}

class LongCsvConverter(
    locale: Locale = Locale.getDefault(),
    pattern: String = "#",
) : AbstractCsvConverter<Long>(locale, pattern) {
    override fun deserialize(value: String?): Result<Long?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            convertNumber(Long::class, value, locale, pattern)
        }
    }

    override fun serialize(value: Long?): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            if (pattern.isNotBlank()) {
                getDecimalFormat(locale, pattern).format(value)
            } else {
                value.toString()
            }
        }
    }
}

class ShortCsvConverter(
    locale: Locale = Locale.getDefault(),
    pattern: String = "#",
) : AbstractCsvConverter<Short>(locale, pattern) {
    override fun deserialize(value: String?): Result<Short?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            convertNumber(Short::class, value, locale, pattern)
        }
    }

    override fun serialize(value: Short?): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            if (pattern.isNotBlank()) {
                getDecimalFormat(locale, pattern).format(value)
            } else {
                value.toString()
            }
        }
    }
}

class ByteCsvConverter(
    locale: Locale = Locale.getDefault(),
    pattern: String = "#",
) : AbstractCsvConverter<Byte>(locale, pattern) {
    override fun deserialize(value: String?): Result<Byte?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            convertNumber(Byte::class, value, locale, pattern)
        }
    }

    override fun serialize(value: Byte?): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            if (pattern.isNotBlank()) {
                getDecimalFormat(locale, pattern).format(value)
            } else {
                value.toString()
            }
        }
    }
}

class FloatCsvConverter(
    locale: Locale = Locale.getDefault(),
    pattern: String = "#.###",
) : AbstractCsvConverter<Float>(locale, pattern) {
    override fun deserialize(value: String?): Result<Float?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            convertNumber(Float::class, value, locale, pattern)
        }
    }

    override fun serialize(value: Float?): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            if (pattern.isNotBlank()) {
                getDecimalFormat(locale, pattern).format(value)
            } else {
                value.toString()
            }
        }
    }
}

class DoubleCsvConverter(
    locale: Locale = Locale.getDefault(),
    pattern: String = "#.######",
) : AbstractCsvConverter<Double>(locale, pattern) {
    override fun deserialize(value: String?): Result<Double?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            convertNumber(Double::class, value, locale, pattern)
        }
    }

    override fun serialize(value: Double?): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            if (pattern.isNotBlank()) {
                getDecimalFormat(locale, pattern).format(value)
            } else {
                value.toString()
            }
        }
    }
}

class StringCsvConverter : CsvConverter<String> {
    override fun deserialize(value: String?): Result<String?> =
        runCatching {
            value
        }

    override fun serialize(value: String?): Result<String?> =
        runCatching {
            value
        }
}

class BigDecimalCsvConverter(
    locale: Locale = Locale.getDefault(),
    pattern: String = "#.###########",
) : AbstractCsvConverter<BigDecimal>(locale, pattern) {
    override fun deserialize(value: String?): Result<BigDecimal?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            convertNumber(BigDecimal::class, value, locale, pattern)
        }
    }

    override fun serialize(value: BigDecimal?): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null

            if (pattern.isNotBlank()) {
                getDecimalFormat(locale, pattern).format(value)
            } else {
                value.toPlainString()
            }
        }
    }
}

class LocalDateCsvConverter(
    locale: Locale = Locale.getDefault(),
    pattern: String = "YYYY-MM-dd",
) : AbstractCsvConverter<LocalDate>(locale, pattern) {
    override fun deserialize(value: String?): Result<LocalDate?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            DateTimeFormatter.ofPattern(pattern, locale)?.parse(value.trim(), LocalDate::from)
        }
    }

    override fun serialize(value: LocalDate?): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            DateTimeFormatter.ofPattern(pattern, locale).format(value)
        }
    }
}

class LocalDateTimeCsvConverter(
    locale: Locale = Locale.getDefault(),
    pattern: String = "YYYY-MM-dd HH:mm:ss",
) : AbstractCsvConverter<LocalDateTime>(locale, pattern) {
    override fun deserialize(value: String?): Result<LocalDateTime?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            DateTimeFormatter.ofPattern(pattern, locale)?.parse(value.trim(), LocalDateTime::from)
        }
    }

    override fun serialize(value: LocalDateTime?): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            DateTimeFormatter.ofPattern(pattern, locale).format(value)
        }
    }
}

class BooleanCsvConverter(
    private val trueValues: List<String> = listOf("true", "yes", "ok"),
    private val falseValues: List<String> = listOf("false", "no", "bad"),
    private val caseSensitive: Boolean = true,
) : CsvConverter<Boolean> {
    override fun deserialize(value: String?): Result<Boolean?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null

            val normalized = if (caseSensitive) value.trim() else value.trim().lowercase()

            if (trueValues.isEmpty() && falseValues.isEmpty()) {
                return@runCatching normalized.toBooleanStrictOrNull()
            }

            val trueVals = if (caseSensitive) trueValues else trueValues.map { it.lowercase() }
            val falseVals = if (caseSensitive) falseValues else falseValues.map { it.lowercase() }

            when (normalized) {
                in trueVals -> true
                in falseVals -> false
                else -> throw IllegalArgumentException(
                    "Invalid boolean value: \"$value\" (trueValues: $trueValues, falseValues: $falseValues, caseSensitive=true)",
                )
            }
        }
    }

    override fun serialize(value: Boolean?): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            if (trueValues.isEmpty() && falseValues.isEmpty()) return@runCatching value.toString()

            if (value) trueValues.first() else falseValues.first()
        }
    }
}
