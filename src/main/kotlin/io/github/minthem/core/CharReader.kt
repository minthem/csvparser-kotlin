package io.github.minthem.core

import java.io.Reader

internal class CharReader(private val reader: Reader) {

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