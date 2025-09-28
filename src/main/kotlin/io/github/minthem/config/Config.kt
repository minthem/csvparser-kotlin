package io.github.minthem.config

import java.util.Locale

data class CsvConfig(
    val delimiter: Char = ',',
    val quoteChar: Char = '"',
    val locale: Locale = Locale.getDefault(),
    val strictMode: Boolean = true
)

data class ReaderConfig(
    val skipRows: Int = 0,
    val hasHeader: Boolean = true,
    val ignoreBlankLine: Boolean = false,
    val skipInvalidLine: Boolean = false,
)

data class WriterConfig(
    val lineSeparator: String = System.lineSeparator(),
)
