package io.github.minthem.exception

/**
 * Base exception type for csvparser.
 */
open class CsvException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Thrown when a CSV line has invalid in-line format such as an unclosed quote
 * or an unexpected character after closing quote.
 *
 * @param lineNo 1-based line number in the original input
 * @param position 1-based character position within the line where the error occurred
 */
class CsvFormatException(
    message: String,
    val lineNo: Int,
    val position: Int,
) : CsvException("$message (at line $lineNo, position $position)")

/**
 * Thrown when a line-level constraint is violated (e.g., header problems or
 * a row having different column count than the header).
 *
 * @param lineNo 1-based line number in the original input
 */
class CsvLineFormatException(
    message: String,
    val lineNo: Int,
) : CsvException("$message (at line $lineNo)")

/**
 * Thrown when attempting to access a column by name but no header is available.
 */
class CsvHeaderNotFoundException : CsvException("No header found")

class CsvColumnNotFoundException(
    column: String,
) : CsvException("No such column: $column")
