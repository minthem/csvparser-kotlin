package io.github.minthem.exception

import kotlin.reflect.KClass
import kotlin.reflect.KType

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

open class CsvEntityException(
    message: String,
    cause: Throwable? = null,
) : CsvException(
        message,
        cause,
    )

class CsvEntityMappingException(
    entityClass: KClass<*>,
    detail: String,
    cause: Throwable? = null,
) : CsvEntityException("Failed to map CSV line to ${entityClass.simpleName}: $detail", cause)

class CsvFieldNotFoundInHeaderException(
    entityClass: KClass<*>,
    paramName: String?,
    columnName: String,
) : CsvEntityException("Column '$columnName' for parameter '${paramName ?: "?"}' not found in header(entity: ${entityClass.simpleName})")

class CsvFieldIndexOutOfRangeException(
    entityClass: KClass<*>,
    paramName: String?,
    index: Int,
    headerSize: Int? = null,
) : CsvEntityException(
        buildString {
            append("Invalid column index $index for parameter '${paramName ?: "?"}' (entity: ${entityClass.simpleName})")
            headerSize?.let { append(", header size: $it") }
        },
    )

class CsvFieldConvertException(
    entityClass: KClass<*>,
    paramName: String?,
    columnName: String?,
    columnIndex: Int,
    cause: Throwable,
) : CsvEntityException(
        "Failed to convert column '${columnName ?: "none"}'(index $columnIndex) for parameter '${paramName ?: "?"}' (entity: ${entityClass.simpleName})",
        cause,
    )

class CsvUnsupportedTypeException(
    entityClass: KClass<*>,
    paramName: String?,
    type: KType,
) : CsvEntityException("Unsupported type '$type' for parameter '${paramName ?: "?"}' (entity: ${entityClass.simpleName})")

class CsvEntityConstructionException(
    entityClass: KClass<*>,
    detail: String,
    cause: Throwable? = null,
) : CsvEntityException("Failed to construct entity '${entityClass.simpleName}': $detail", cause)
