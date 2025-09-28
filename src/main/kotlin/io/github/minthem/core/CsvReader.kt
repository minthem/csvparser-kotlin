package io.github.minthem.core

import io.github.minthem.config.CsvConfig
import io.github.minthem.config.ReaderConfig
import io.github.minthem.exception.CsvException
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

        val sequence = sequence {
            inner@while (true) {
                val line = readLine() ?: break
                val lineNo = currentReadLineNum

                if (line.isBlank()) {
                    if (readConfig.ignoreBlankLine) {
                        continue
                    } else {
                        throw CsvLineFormatException(
                            "Blank line encountered but ignoreBlankLine=false",
                            lineNo
                        )
                    }
                }
                try {
                    val cells = try {
                        tokenizer.tokenize(line)
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

                    yield(Row(cells, headerMap))

                } catch (ce: CsvException) {
                    if(readConfig.skipInvalidLine) {
                        //TODO output warning log
                        continue@inner
                    }
                    throw ce
                }
            }
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