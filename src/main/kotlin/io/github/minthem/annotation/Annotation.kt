package io.github.minthem.annotation

/**
 * Marks a constructor parameter or property to be bound to a CSV/TSV column.
 *
 * Rules:
 * - When [index] > 0, the column is resolved by 1-based index.
 * - When [index] == 0 (default), the column is resolved by header name. The
 *   name is [name] when non-blank; otherwise the parameter/property name.
 * - [name] is ignored when [index] > 0.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
annotation class CsvField(
    val name: String = "",
    val index: Int = 0,
)

/**
 * Hints for formatting/parsing numbers and date/time values.
 *
 * - [pattern]: number or date/time pattern (e.g., `#,##0.###`, `yyyy-MM-dd`, `yyyy-MM-dd HH:mm:ss`).
 *   When blank, a sensible default for the target type is used.
 * - [locale]: BCP 47 language tag for locale-sensitive parsing/formatting (e.g., `ja-JP`, `en-US`).
 *   When blank, `Locale.getDefault()` is used.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
annotation class CsvFieldFormat(
    val pattern: String = "",
    val locale: String = "",
)

/**
 * Defines how boolean values are recognized when parsing and how they are formatted when writing.
 *
 * - [trueValues]: any of these strings are treated as true
 * - [falseValues]: any of these strings are treated as false
 * - [ignoreCase]: when true (default), matching ignores case
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
annotation class BooleanCsvField(
    val trueValues: Array<String> = ["true", "1", "yes", "y"],
    val falseValues: Array<String> = ["false", "0", "no", "n"],
    val ignoreCase: Boolean = true,
)
