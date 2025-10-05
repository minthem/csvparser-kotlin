package io.github.minthem.core

import io.github.minthem.exception.CsvColumnNotFoundException
import io.github.minthem.exception.CsvHeaderNotFoundException

/**
 * A single CSV row with optional header-aware access.
 *
 * Provides both index-based and name-based access. Name-based access requires that
 * the reader was configured with a header.
 */
class Row internal constructor(
    private val cells: List<String?>,
    private val headerIndex: Map<String, Int>? = null,
) : Iterable<String?> {
    /** Returns the cell at the given zero-based [index]. */
    operator fun get(index: Int) = cells[index]

    /** Returns the cell at [index], or null if the index is out of bounds. */
    fun getOrNull(index: Int) = cells.getOrNull(index)

    /** Returns the cell for the given column name. Requires a header. */
    operator fun get(column: String): String? {
        if (headerIndex == null) {
            throw CsvHeaderNotFoundException()
        }

        val index = headerIndex[column] ?: throw CsvColumnNotFoundException(column)

        return cells[index]
    }

    /** Returns the cell for the given column name, or null when no header or no such column exists. */
    fun getOrNull(column: String): String? {
        val index = headerIndex?.get(column) ?: return null
        return cells.getOrNull(index)
    }

    /** Returns an iterator over cells in this row. */
    override fun iterator(): Iterator<String?> = cells.iterator()

    /** Returns a human-readable representation like: [a, b, c]. */
    override fun toString(): String = cells.joinToString(prefix = "[", postfix = "]")
}
