package io.github.minthem.annotation

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
annotation class CsvField(
    val name: String = "",
    val index: Int = -1,
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
annotation class CsvFieldFormat(
    val pattern: String = "",
    val locale: String = "",
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
annotation class BooleanCsvField(
    val trueValues: Array<String> = ["true", "1", "yes", "y"],
    val falseValues: Array<String> = ["false", "0", "no", "n"],
    val ignoreCase: Boolean = true,
)
