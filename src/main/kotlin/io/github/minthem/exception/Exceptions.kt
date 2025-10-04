package io.github.minthem.exception

open class CsvException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class CsvFormatException(
    message: String,
    val lineNo: Int,
    val position: Int,
) : CsvException("$message (at line $lineNo, position $position)")

class CsvLineFormatException(
    message: String,
    val lineNo: Int,
) : CsvException("$message (at line $lineNo)")

class CsvHeaderNotFoundException : CsvException("No header found")

class CsvColumnNotFoundException(
    column: String,
) : CsvException("No such column: $column")
