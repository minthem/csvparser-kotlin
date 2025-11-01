package io.github.minthem.converter

import java.util.Locale

interface CsvConverter<T> {
    fun deserialize(value: String?): Result<T?>

    fun serialize(value: T?): Result<String?>
}

abstract class LocalizedCsvConverter<T>(
    protected val locale: Locale,
    protected val pattern: String,
) : CsvConverter<T>

abstract class LocalizedCsvConverterBuilder<T>(
    defaultPattern: String,
) {
    protected var locale: Locale = Locale.getDefault()
    protected var pattern: String = defaultPattern

    fun locale(locale: Locale): LocalizedCsvConverterBuilder<T> {
        this.locale = locale
        return this
    }

    fun pattern(pattern: String): LocalizedCsvConverterBuilder<T> {
        this.pattern = pattern
        return this
    }

    abstract fun build(): LocalizedCsvConverter<T>
}
