package io.github.minthem.core

import io.github.minthem.config.CsvConfig
import io.github.minthem.config.ReaderConfig
import io.github.minthem.exception.CsvFormatException
import io.github.minthem.exception.CsvLineFormatException
import java.io.Reader

class CsvReader(
    private val reader: Reader,
    private val config: CsvConfig = CsvConfig(),
    private val readConfig: ReaderConfig = ReaderConfig()
) : Iterable<Row> {

    private val lineExtractor = LineExtractor(reader, config.quoteChar, lineBufferSize = 8192)
    private val tokenizer = Tokenizer(config.delimiter, config.quoteChar)

    private var header: List<String>? = null
    private var headerMap: Map<String, Int>? = null
    private var initialized = false

    private var currentReadLineNum = 0

    override fun iterator(): Iterator<Row> {
        init()

        val sequence = generateSequence {
            readLine()
        }.map { line ->
            val cells: List<String?>
            val lineNo = currentReadLineNum
            try {
                cells = tokenizer.tokenize(line)
            } catch (e: CsvFormatInternalException) {
                throw CsvFormatException(e.message ?: "Invalid CSV(TSV) Format", lineNo, e.position)
            }

            header?.let {
                if (it.size != cells.size) {
                    throw CsvLineFormatException(
                        "Column count mismatch: header has ${it.size} columns but data row has ${cells.size} columns",
                        lineNo
                    )
                }
            }

            cells
        }.map {
            Row(it, headerMap)
        }

        return sequence.iterator()
    }

    fun header(): List<String>? {
        init()
        return header
    }

    private fun init() {
        if (initialized) return
        initialized = true

        repeat(readConfig.skipRows) { readLine() }

        if (readConfig.hasHeader) {
            val headerLine = readLine() ?: throw CsvLineFormatException(
                "Expected header line but reached end of input",
                currentReadLineNum
            )

            val cells = tokenizer.tokenize(headerLine)
            val (hdr, indexMap) = validateAndBuildHeader(cells)
            header = hdr
            headerMap = indexMap
        }
    }

    private fun readLine(): String? {
        currentReadLineNum++
        return lineExtractor.getLine()
    }

    private fun validateAndBuildHeader(
        columns: List<String?>,
    ): Pair<List<String>, Map<String, Int>> {
        val seen = mutableSetOf<String>()
        val header = columns.mapIndexed { index, name ->
            val colName = name?.takeIf { it.isNotBlank() }
                ?: throw CsvLineFormatException(
                    "Header column name at index $index cannot be null or blank",
                    currentReadLineNum
                )

            if (!seen.add(colName)) {
                throw CsvLineFormatException(
                    "Header column name '$colName' at index $index is duplicated",
                    currentReadLineNum
                )
            }
            colName
        }
        val indexMap = header.withIndex().associate { it.value to it.index }
        return header to indexMap
    }
}