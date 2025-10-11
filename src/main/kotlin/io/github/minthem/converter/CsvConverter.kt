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
    fun deserialize(value: String?, locale: Locale, pattern: String): Result<T?>
    fun serialize(value: T?, locale: Locale, pattern: String): Result<String?>
}

private fun getDecimalFormat(locale: Locale, pattern: String): DecimalFormat {
    val symbols = DecimalFormatSymbols(locale)
    return DecimalFormat(pattern, symbols).apply {
        isParseBigDecimal = true
    }
}

internal fun <T : Number> convertNumber(
    type: KClass<T>,
    value: String,
    locale: Locale,
    pattern: String
): T {
    val trimmed = value.trim()
    val formatter = getDecimalFormat(locale, pattern)
    val pos = ParsePosition(0)
    val parsed = formatter.parse(trimmed, pos) as? BigDecimal

    if (pos.index != trimmed.length || parsed == null) {
        throw NumberFormatException("Invalid number: \"$value\"")
    }

    val rounded = if (pattern.isNotBlank()) {
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

object IntCsvConverter : CsvConverter<Int> {
    override fun deserialize(
        value: String?,
        locale: Locale,
        pattern: String
    ): Result<Int?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            convertNumber(Int::class, value, locale, pattern)
        }
    }

    override fun serialize(
        value: Int?,
        locale: Locale,
        pattern: String
    ): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            getDecimalFormat(locale, pattern).format(value)
        }
    }
}

class LongCsvConverter : CsvConverter<Long> {
    override fun deserialize(
        value: String?,
        locale: Locale,
        pattern: String
    ): Result<Long?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            convertNumber(Long::class, value, locale, pattern)
        }
    }

    override fun serialize(
        value: Long?,
        locale: Locale,
        pattern: String
    ): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            getDecimalFormat(locale, pattern).format(value)
        }
    }
}

class ShortCsvConverter : CsvConverter<Short> {
    override fun deserialize(
        value: String?,
        locale: Locale,
        pattern: String
    ): Result<Short?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            convertNumber(Short::class, value, locale, pattern)
        }
    }

    override fun serialize(
        value: Short?,
        locale: Locale,
        pattern: String
    ): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            getDecimalFormat(locale, pattern).format(value)
        }
    }
}

object ByteCsvConverter : CsvConverter<Byte> {
    override fun deserialize(
        value: String?,
        locale: Locale,
        pattern: String
    ): Result<Byte?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            convertNumber(Byte::class, value, locale, pattern)
        }
    }

    override fun serialize(
        value: Byte?,
        locale: Locale,
        pattern: String
    ): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            getDecimalFormat(locale, pattern).format(value)
        }
    }
}

class FloatCsvConverter : CsvConverter<Float> {
    override fun deserialize(
        value: String?,
        locale: Locale,
        pattern: String
    ): Result<Float?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            convertNumber(Float::class, value, locale, pattern)
        }
    }

    override fun serialize(
        value: Float?,
        locale: Locale,
        pattern: String
    ): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            getDecimalFormat(locale, pattern).format(value)
        }
    }
}

class DoubleCsvConverter : CsvConverter<Double> {
    override fun deserialize(
        value: String?,
        locale: Locale,
        pattern: String
    ): Result<Double?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            convertNumber(Double::class, value, locale, pattern)
        }
    }

    override fun serialize(
        value: Double?,
        locale: Locale,
        pattern: String
    ): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            getDecimalFormat(locale, pattern).format(value)
        }
    }
}

class StringCsvConverter : CsvConverter<String> {
    override fun deserialize(
        value: String?,
        locale: Locale,
        pattern: String
    ): Result<String?> {
        return runCatching {
            value
        }
    }

    override fun serialize(
        value: String?,
        locale: Locale,
        pattern: String
    ): Result<String?> {
        return runCatching {
            value
        }
    }
}

object BigDecimalCsvConverter : CsvConverter<BigDecimal> {
    override fun deserialize(value: String?, locale: Locale, pattern: String): Result<BigDecimal?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            convertNumber(BigDecimal::class, value, locale, pattern)
        }
    }

    override fun serialize(value: BigDecimal?, locale: Locale, pattern: String): Result<String?> {
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

class BooleanCsvConverter : CsvConverter<Boolean> {
    override fun deserialize(
        value: String?,
        locale: Locale,
        pattern: String
    ): Result<Boolean?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null

            val (trueParts, falseParts) = pattern.split("|").let {
                val trueValues =
                    it.getOrNull(0)?.split(",")?.map { s -> s.trim().lowercase() }?.toSet() ?: setOf("true")
                val falseValues =
                    it.getOrNull(1)?.split(",")?.map { s -> s.trim().lowercase() }?.toSet() ?: setOf("false")
                trueValues to falseValues
            }

            val normalized = value.trim().lowercase()
            if (normalized in trueParts) true else normalized in falseParts
        }
    }

    override fun serialize(
        value: Boolean?,
        locale: Locale,
        pattern: String
    ): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null

            val (truePart, falsePart) = pattern.split("|").let {
                val trueValue = it.getOrNull(0)?.split(",")?.map { s -> s.trim() } ?: listOf("true")
                val falseValue = it.getOrNull(1)?.split(",")?.map { s -> s.trim() } ?: listOf("false")
                trueValue to falseValue
            }

            if (value) truePart.first() else falsePart.first()
        }
    }
}

class LocalDateCsvConverter : CsvConverter<LocalDate> {
    override fun deserialize(
        value: String?,
        locale: Locale,
        pattern: String
    ): Result<LocalDate?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            DateTimeFormatter.ofPattern(pattern, locale)?.parse(value.trim(), LocalDate::from)
        }
    }

    override fun serialize(
        value: LocalDate?,
        locale: Locale,
        pattern: String
    ): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            DateTimeFormatter.ofPattern(pattern, locale).format(value)
        }
    }
}

class LocalDateTimeCsvConverter : CsvConverter<LocalDateTime> {
    override fun deserialize(
        value: String?,
        locale: Locale,
        pattern: String
    ): Result<LocalDateTime?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null
            DateTimeFormatter.ofPattern(pattern, locale)?.parse(value.trim(), LocalDateTime::from)
        }
    }

    override fun serialize(value: LocalDateTime?, locale: Locale, pattern: String): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            DateTimeFormatter.ofPattern(pattern, locale).format(value)
        }
    }
}
