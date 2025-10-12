package io.github.minthem.annotation

import io.github.minthem.converter.CsvConverter
import io.github.minthem.converter.NoopCsvConverter
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
annotation class CsvField(
    val name: String = "",
    val index: Int = -1,
    val converter: KClass<out CsvConverter<*>> = NoopCsvConverter::class,
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
annotation class CsvFieldFormat(
    val pattern: String = "",
    val locale: String = "",
)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
annotation class BooleanCsvField(
    val trueValues: Array<String> = ["true", "1", "yes", "y"],
    val falseValues: Array<String> = ["false", "0", "no", "n"],
    val ignoreCase: Boolean = true
)
