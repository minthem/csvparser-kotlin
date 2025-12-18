package io.github.minthem.csvparser.core

import io.github.minthem.csvparser.annotation.BooleanCsvField
import io.github.minthem.csvparser.annotation.CsvFieldFormat
import io.github.minthem.csvparser.converter.BigDecimalCsvConverter
import io.github.minthem.csvparser.converter.BooleanCsvConverter
import io.github.minthem.csvparser.converter.ByteCsvConverter
import io.github.minthem.csvparser.converter.CsvConverter
import io.github.minthem.csvparser.converter.DoubleCsvConverter
import io.github.minthem.csvparser.converter.FloatCsvConverter
import io.github.minthem.csvparser.converter.IntCsvConverter
import io.github.minthem.csvparser.converter.LocalDateCsvConverter
import io.github.minthem.csvparser.converter.LocalDateTimeCsvConverter
import io.github.minthem.csvparser.converter.LongCsvConverter
import io.github.minthem.csvparser.converter.ShortCsvConverter
import io.github.minthem.csvparser.converter.StringCsvConverter
import io.github.minthem.csvparser.exception.CsvUnsupportedTypeException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation

internal object CsvConverterHelper {
    fun resolve(
        clazz: KClass<*>,
        field: KParameter,
    ): CsvConverter<*> = buildConverter(clazz, field.type, field, field.name)

    fun resolve(
        clazz: KClass<*>,
        field: KProperty1<*, *>,
    ): CsvConverter<*> = buildConverter(clazz, field.returnType, field, field.name)

    private fun buildConverter(
        clazz: KClass<*>,
        type: KType,
        field: KAnnotatedElement,
        fieldName: String?,
    ): CsvConverter<*> {
        val fieldFmt = field.findAnnotation<CsvFieldFormat>()

        if (type.classifier == String::class) {
            return StringCsvConverter()
        }
        if (type.classifier == Boolean::class) {
            val boolFmt = field.findAnnotation<BooleanCsvField>() ?: BooleanCsvField()
            return BooleanCsvConverter
                .Builder()
                .trueValues(boolFmt.trueValues.toList())
                .falseValues(boolFmt.falseValues.toList())
                .caseSensitive(!boolFmt.ignoreCase)
                .build()
        }

        var builder =
            when (type.classifier) {
                Int::class -> IntCsvConverter.Builder()
                Long::class -> LongCsvConverter.Builder()
                Short::class -> ShortCsvConverter.Builder()
                Byte::class -> ByteCsvConverter.Builder()
                Float::class -> FloatCsvConverter.Builder()
                Double::class -> DoubleCsvConverter.Builder()
                BigDecimal::class -> BigDecimalCsvConverter.Builder()
                LocalDate::class -> LocalDateCsvConverter.Builder()
                LocalDateTime::class -> LocalDateTimeCsvConverter.Builder()
                else -> {
                    throw CsvUnsupportedTypeException(
                        clazz,
                        fieldName,
                        type,
                    )
                }
            }

        fieldFmt?.locale?.let { builder = builder.locale(Locale.forLanguageTag(it)) }

        if (fieldFmt?.pattern.isNullOrBlank().not()) {
            builder = builder.pattern(fieldFmt.pattern)
        }

        return builder.build()
    }
}
