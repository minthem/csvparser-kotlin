package io.github.minthem.core

import io.github.minthem.exception.CsvColumnNotFoundException
import io.github.minthem.exception.CsvHeaderNotFoundException

class Row(
    private val cells: List<String?>,
    private val headerIndex: Map<String, Int>? = null,
) {

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

    override fun toString(): String = cells.joinToString(prefix = "[", postfix = "]")
}
