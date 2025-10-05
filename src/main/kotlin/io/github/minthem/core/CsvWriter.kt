package io.github.minthem.core

import io.github.minthem.config.CsvConfig
import io.github.minthem.config.WriterConfig

/**
 * A CSV/TSV writer.
 *
 * You can optionally call [writeHeader] first. When a header is present, [writeRow]
 * outputs cells in header order and fills missing columns with [CsvConfig.nullValue].
 * Quoting and escaping follow the rules defined by [CsvConfig].
 *
 * @param out destination to append CSV text
 * @param config general CSV behavior such as delimiter, quote char, and nullValue
 * @param writeConfig writer-specific options such as line separator
 */
class CsvWriter(
    private val out: Appendable,
    private val config: CsvConfig,
    private val writeConfig: WriterConfig = WriterConfig(),
) {
    private var isRowWritten = false
    private var isHeaderWritten = false
    private var header: List<String>? = null

    private val formatter = CsvLineFormatter(config.delimiter, config.quoteChar)

    /**
     * Writes a header row.
     *
     * @throws IllegalArgumentException if the header is empty, contains blank cells or duplicates
     * @throws IllegalStateException if called twice or after any data row has been written
     */
    fun writeHeader(header: List<String>) {
        if (header.isEmpty()) {
            throw IllegalArgumentException("Header cannot be empty.")
        }

        if (header.any { it.isBlank() }) {
            throw IllegalArgumentException("Header cannot contain empty columns.")
        }

        if (header.toSet().size != header.size) {
            throw IllegalArgumentException("Header cannot contain duplicate columns.")
        }

        if (isRowWritten) {
            throw IllegalStateException("Cannot write header after rows have been written.")
        }

        if (isHeaderWritten) {
            throw IllegalStateException("Header has already been written.")
        }

        val headerLine = formatter.join(header)
        out.append(headerLine).append(writeConfig.lineSeparator.value)
        this.header = header
        isHeaderWritten = true
    }

    /**
     * Writes a data row.
     *
     * If a header was written, the row is aligned to the header order; missing values
     * are emitted as [CsvConfig.nullValue]. When needed, cells are quoted and quotes
     * are escaped by doubling them.
     */
    fun writeRow(row: Row) {
        val cells =
            header?.let { header ->
                header.map { row.getOrNull(it) }
            } ?: row.toList()
        val line = formatter.join(cells, config.nullValue)
        out.append(line).append(writeConfig.lineSeparator.value)
        isRowWritten = true
    }
}
