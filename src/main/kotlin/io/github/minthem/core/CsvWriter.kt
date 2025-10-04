package io.github.minthem.core

import io.github.minthem.config.CsvConfig
import io.github.minthem.config.WriterConfig

class CsvWriter(
    private val out: Appendable,
    private val config: CsvConfig,
    private val writeConfig: WriterConfig = WriterConfig(),
) {
    private var isRowWritten = false
    private var isHeaderWritten = false
    private var header: List<String>? = null

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

        val headerLine = header.joinToString(config.delimiter.toString()) { escape(it) }
        out.append(headerLine).append(writeConfig.lineSeparator.value)
        this.header = header
        isHeaderWritten = true
    }

    fun writeRow(row: Row) {
        val cells =
            header?.let { header ->
                header.map { row.getOrNull(it) }
            } ?: row.toList()
        val line = cells.joinToString(config.delimiter.toString()) { escape(it) }
        out.append(line).append(writeConfig.lineSeparator.value)
        isRowWritten = true
    }

    private fun escape(value: String?): String {
        val needQuote =
            value?.let {
                it.contains(config.delimiter) ||
                    it.contains('\r') ||
                    it.contains('\n') ||
                    it.contains(config.quoteChar)
            } ?: false

        return if (needQuote) {
            val escaped = value.replace(config.quoteChar.toString(), "${config.quoteChar}${config.quoteChar}")
            "${config.quoteChar}${escaped}${config.quoteChar}"
        } else {
            value?.ifEmpty { "${config.quoteChar}${config.quoteChar}" } ?: config.nullValue
        }
    }
}
