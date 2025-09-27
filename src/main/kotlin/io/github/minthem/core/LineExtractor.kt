package io.github.minthem.core

import java.io.Reader

internal class LineExtractor(
    private val reader: Reader,
    private val quote: Char = '"',
    private val bufferSize: Int = 1024
) {

    private val buffer = CharArray(bufferSize)
    private var pos = 0
    private var limit = 0
    private var inQuote = false
    private var incomingCr = false


    fun getLine(): String? {
        val sb = StringBuilder(bufferSize)

        while (true) {
            if (limit <= pos) {
                val readn = reader.read(buffer)
                if (readn == -1) {
                    return if (sb.isNotEmpty()) sb.toString() else null
                }

                limit = readn
                pos = 0
            }

            when (val c = buffer[pos++]) {
                quote -> {
                    incomingCr = false
                    inQuote = !inQuote
                    sb.append(c)
                }

                '\r' -> {
                    if (inQuote) {
                        sb.append(c)
                    } else {
                        return if (pos < limit) {
                           if (buffer[pos] == '\n') {
                                pos++
                                sb.toString() // CRLF
                            } else {
                                sb.toString() // CR
                            }
                        } else {
                            incomingCr = true
                            sb.toString()
                        }

                    }
                }

                '\n' -> {
                    if (inQuote) {
                        incomingCr = false
                        sb.append(c)
                        continue
                    }

                    if (!incomingCr) {
                        return sb.toString()
                    }

                    incomingCr = false
                }

                else -> {
                    if(!incomingCr) {
                        sb.append(c)
                        continue
                    }

                    incomingCr = false
                    return sb.toString()
                }
            }
        }

    }

}
