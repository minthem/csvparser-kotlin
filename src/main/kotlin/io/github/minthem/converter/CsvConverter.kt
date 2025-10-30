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
