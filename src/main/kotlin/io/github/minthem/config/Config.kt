package io.github.minthem.config

import java.util.Locale

data class CsvConfig(
    val delimiter: Char = ',',
    val quoteChar: Char = '"',
    val locale: Locale = Locale.getDefault(),
    val strictMode: Boolean = true,
    val nullValue: String = "",
)

data class ReaderConfig(
    val skipRows: Int = 0,
    val hasHeader: Boolean = true,
    val ignoreBlankLine: Boolean = false,
    val skipInvalidLine: Boolean = false,
)

data class WriterConfig(
    val lineSeparator: LineSeparator = LineSeparator.SYSTEM,
) {
    enum class LineSeparator(val value: String) {
        CRLF("\r\n"),
        LF("\n"),
        CR("\r"),
        SYSTEM(System.lineSeparator())
    }
}
