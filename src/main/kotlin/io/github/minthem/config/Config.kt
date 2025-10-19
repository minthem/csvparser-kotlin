package io.github.minthem.config

/**
 * Global CSV/TSV behavior settings used by both reader and writer.
 *
 * - delimiter: field separator (e.g., ',' for CSV, '\t' for TSV)
 * - quoteChar: quote character used to wrap and escape fields (e.g., '"')
 * - strictMode: reserved for future conversions; when true, converters should fail fast
 * - nullValue: text to emit when writing null cells
 */
data class CsvConfig(
    val delimiter: Char = ',',
    val quoteChar: Char = '"',
    val strictMode: Boolean = true,
    val nullValue: String = "",
) {
    init {
        if (delimiter == quoteChar) throw IllegalArgumentException("delimiter and quoteChar cannot be same.")
    }
}

/**
 * CSV reader options.
 *
 * - skipRows: number of leading lines to skip before reading (e.g., comments)
 * - hasHeader: whether the first non-skipped line is a header row
 * - ignoreBlankLine: when true, blank lines are skipped; when false, they cause an error
 * - skipInvalidLine: when true, invalid lines are skipped; when false, exceptions are thrown
 */
data class ReaderConfig(
    val skipRows: Int = 0,
    val hasHeader: Boolean = true,
    val ignoreBlankLine: Boolean = false,
    val skipInvalidLine: Boolean = false,
) {
    init {
        if (skipRows < 0) throw IllegalArgumentException("skipRows cannot be negative.")
    }
}

/**
 * CSV writer options.
 *
 * - lineSeparator: which newline sequence to use when writing
 */
data class WriterConfig(
    val lineSeparator: LineSeparator = LineSeparator.SYSTEM,
) {
    /**
     * Supported line separator values used by the writer when emitting lines.
     */
    enum class LineSeparator(
        val value: String,
    ) {
        /** Windows-style CRLF (\r\n) */
        CRLF("\r\n"),

        /** Unix-style LF (\n) */
        LF("\n"),

        /** Classic Mac-style CR (\r) */
        CR("\r"),

        /** Use the current JVM's system line separator */
        SYSTEM(System.lineSeparator()),
    }
}
