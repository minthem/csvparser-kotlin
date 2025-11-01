package io.github.minthem.converter

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class LocalDateCsvConverter private constructor(
    locale: Locale,
    pattern: String,
) : LocalizedCsvConverter<LocalDate>(locale, pattern) {
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

    class Builder : LocalizedCsvConverterBuilder<LocalDate>("YYYY-MM-dd") {
        override fun build(): LocalDateCsvConverter = LocalDateCsvConverter(locale, pattern)
    }
}

class LocalDateTimeCsvConverter private constructor(
    locale: Locale,
    pattern: String,
) : LocalizedCsvConverter<LocalDateTime>(locale, pattern) {
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

    class Builder : LocalizedCsvConverterBuilder<LocalDateTime>("YYYY-MM-dd") {
        override fun build(): LocalDateTimeCsvConverter = LocalDateTimeCsvConverter(locale, pattern)
    }
}
