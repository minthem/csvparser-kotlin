package io.github.minthem.converter

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
