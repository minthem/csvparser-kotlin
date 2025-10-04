package io.github.minthem.core

import java.io.Reader

internal class LineExtractor(
    reader: Reader,
    private val quote: Char = '"',
    private val lineBufferSize: Int = 1024,
) {
    private val charReader = CharReader(reader)

    fun getLine(): String? {
        val sb = StringBuilder(lineBufferSize)
        var inQuote = false

        while (true) {
            val ch = charReader.readChar() ?: return if (sb.isNotEmpty()) sb.toString() else null

            if (ch == '\r' || ch == '\n') {
                if (inQuote) {
                    sb.append(ch)
                    continue
                }

                if (ch == '\r' && charReader.peekChar() == '\n') {
                    charReader.readChar()
                }

                return sb.toString()
            }

            if (ch == quote) {
                inQuote = !inQuote
            }

            sb.append(ch)
        }
    }
}

private class CharReader(
    private val reader: Reader,
) {
    private var cache: Char? = null

    fun readChar(): Char? {
        if (cache != null) {
            val result = cache
            cache = null
            return result
        } else {
            return reader.read().takeIf { it != -1 }?.toChar()
        }
    }

    fun peekChar(): Char? {
        if (cache == null) {
            cache = readChar()
        }
        return cache
    }
}
