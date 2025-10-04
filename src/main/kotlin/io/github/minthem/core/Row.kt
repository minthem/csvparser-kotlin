package io.github.minthem.core

import io.github.minthem.exception.CsvColumnNotFoundException
import io.github.minthem.exception.CsvHeaderNotFoundException

class Row internal constructor(
    private val cells: List<String?>,
    private val headerIndex: Map<String, Int>? = null,
) : Iterable<String?> {
    operator fun get(index: Int) = cells[index]

    fun getOrNull(index: Int) = cells.getOrNull(index)

    operator fun get(column: String): String? {
        if (headerIndex == null) {
            throw CsvHeaderNotFoundException()
        }

        val index = headerIndex[column] ?: throw CsvColumnNotFoundException(column)

        return cells[index]
    }

    fun getOrNull(column: String): String? {
        val index = headerIndex?.get(column) ?: return null
        return cells.getOrNull(index)
    }

    override fun iterator(): Iterator<String?> = cells.iterator()

    override fun toString(): String = cells.joinToString(prefix = "[", postfix = "]")
}
