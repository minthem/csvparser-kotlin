package io.github.minthem.core

import io.github.minthem.annotation.BooleanCsvField
import io.github.minthem.annotation.CsvField
import io.github.minthem.annotation.CsvFieldFormat
import io.github.minthem.config.CsvConfig
import io.github.minthem.config.ReaderConfig
import io.github.minthem.converter.BigDecimalCsvConverter
import io.github.minthem.converter.BooleanCsvConverter
import io.github.minthem.converter.ByteCsvConverter
import io.github.minthem.converter.CsvConverter
import io.github.minthem.converter.DoubleCsvConverter
import io.github.minthem.converter.FloatCsvConverter
import io.github.minthem.converter.IntCsvConverter
import io.github.minthem.converter.LocalDateCsvConverter
import io.github.minthem.converter.LocalDateTimeCsvConverter
import io.github.minthem.converter.LongCsvConverter
import io.github.minthem.converter.ShortCsvConverter
import io.github.minthem.converter.StringCsvConverter
import io.github.minthem.exception.CsvEntityConstructionException
import io.github.minthem.exception.CsvEntityMappingException
import io.github.minthem.exception.CsvFieldConvertException
import io.github.minthem.exception.CsvFieldIndexOutOfRangeException
import io.github.minthem.exception.CsvFieldNotFoundInHeaderException
import io.github.minthem.exception.CsvUnsupportedTypeException
import java.io.Reader
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Reads CSV/TSV rows and maps them to Kotlin data classes using annotations.
 *
 * Mapping rules:
 * - The target [entityClass] must have a primary constructor.
 * - Each constructor parameter should be annotated with `@CsvField`; optional parameters may omit it.
 * - Column resolution prefers index when `@CsvField.index > 0`; otherwise, the header name (or property name when `@CsvField.name` is blank) is used.
 * - Conversion is controlled by `@CsvFieldFormat` (number/date patterns and locale) and `@BooleanCsvField` for booleans.
 *
 * Behavior:
 * - This class delegates CSV parsing to [CsvReader]; header presence and other read options are defined by [ReaderConfig].
 * - Iteration is lazy; conversion happens per row while iterating.
 *
 * Errors:
 * - [CsvEntityMappingException] when annotations are missing/invalid or indexes conflict.
 * - [CsvFieldNotFoundInHeaderException] or [CsvFieldIndexOutOfRangeException] when a column cannot be resolved from header/index.
 * - [CsvFieldConvertException] when a value fails to convert to the parameter type.
 * - [CsvEntityConstructionException] when invoking the constructor fails.
 * - [CsvUnsupportedTypeException] for unsupported parameter types.
 *
 * @param entityClass target entity class to instantiate per row
 * @param reader input character stream
 * @param config CSV behavior such as delimiter and quote char
 * @param readConfig reader options such as header handling and skipping rules
 */
class CsvEntityReader<T : Any>(
    private val entityClass: KClass<T>,
    reader: Reader,
    config: CsvConfig = CsvConfig(),
    readConfig: ReaderConfig = ReaderConfig(),
) : Iterable<T> {
    private val csvReader = CsvReader(reader, config, readConfig)

    private var initialized = false
    private var paramMap: Map<Int, Pair<KParameter, CsvConverter<*>>> = mutableMapOf()

    /**
     * Returns an iterator that lazily reads CSV rows and constructs entities.
     *
     * Initialization occurs on the first call and includes validating annotations,
     * resolving header/index mapping, and preparing converters.
     *
     * Errors from parsing are delegated from [CsvReader]; mapping and conversion
     * errors are thrown as [io.github.minthem.exception.CsvEntityException] subtypes during iteration.
     */
    override fun iterator(): Iterator<T> {
        init()
        val constructor = entityClass.primaryConstructor!!

        val sequence =
            sequence {
                for (row in csvReader) {
                    val constructorParams = mutableMapOf<KParameter, Any?>()

                    for ((index, pair) in paramMap) {
                        val (param, converter) = pair
                        val value = row.getOrNull(index)
                        val converted =
                            try {
                                converter.deserialize(value).getOrThrow()
                            } catch (e: Exception) {
                                throw CsvFieldConvertException(
                                    entityClass,
                                    param.name,
                                    csvReader.header()?.getOrNull(index),
                                    index,
                                    e,
                                )
                            }

                        if (converted == null && param.isOptional) {
                            continue
                        }
                        constructorParams[param] = converted
                    }

                    try {
                        yield(constructor.callBy(constructorParams))
                    } catch (e: Exception) {
                        throw CsvEntityConstructionException(
                            entityClass,
                            "Constructor invocation failed for parameters: ${constructorParams.keys.map { it.name }}",
                            e,
                        )
                    }
                }
            }

        return sequence.iterator()
    }

    private fun init() {
        if (initialized) return

        val header = csvReader.header()
        val constructor =
            entityClass.primaryConstructor
                ?: throw CsvEntityMappingException(entityClass, "Entity class must have primary constructor.")

        val classPropertiesMap = entityClass.memberProperties.associateBy { it.name }

        for (parameter in constructor.parameters) {
            val csvField =
                getAnnotation<CsvField>(parameter, classPropertiesMap) ?: if (!parameter.isOptional) {
                    throw CsvEntityMappingException(
                        entityClass,
                        "Parameter ${parameter.name} must be annotated with @CsvField or must be optional.",
                    )
                } else {
                    continue
                }

            val index = resolveHeaderIndex(header, csvField, parameter)
            val converter = resolveConverter(parameter, classPropertiesMap)

            paramMap = paramMap + (index to (parameter to converter))
        }

        initialized = true
    }

    private inline fun <reified A : Annotation> getAnnotation(
        parameter: KParameter,
        propertyMap: Map<String, KProperty1<*, *>>,
    ): A? {
        val ann = parameter.findAnnotation<A>()
        if (ann != null) return ann

        val property = propertyMap[parameter.name]
        return property?.findAnnotation<A>()
    }

    private fun resolveHeaderIndex(
        header: List<String>?,
        csvField: CsvField,
        parameter: KParameter,
    ): Int {
        val name = csvField.name

        val index =
            if (header != null && name.isNotBlank()) {
                val idx = header.indexOf(csvField.name)
                if (idx < 0) {
                    throw CsvFieldNotFoundInHeaderException(
                        entityClass,
                        parameter.name,
                        csvField.name,
                    )
                }
                idx
            } else {
                val idx = csvField.index
                if (idx <= 0) {
                    throw CsvFieldIndexOutOfRangeException(
                        entityClass,
                        parameter.name,
                        idx,
                        header?.size,
                    )
                }

                idx - 1
            }

        return index
    }

    private fun resolveConverter(
        member: KParameter,
        propertyMap: Map<String, KProperty1<*, *>>,
    ): CsvConverter<*> {
        val fieldFmt = this.getAnnotation<CsvFieldFormat>(member, propertyMap)
        val locale =
            if (fieldFmt?.locale.isNullOrBlank()) {
                Locale.getDefault()
            } else {
                Locale.forLanguageTag(fieldFmt.locale)
            }

        return when (member.type.classifier) {
            Int::class -> {
                IntCsvConverter(locale, fieldFmt?.pattern ?: "#")
            }

            Long::class -> {
                LongCsvConverter(locale, fieldFmt?.pattern ?: "#")
            }

            Short::class -> {
                ShortCsvConverter(locale, fieldFmt?.pattern ?: "#")
            }

            Byte::class -> {
                ByteCsvConverter(locale, fieldFmt?.pattern ?: "#")
            }

            Float::class -> {
                FloatCsvConverter(locale, fieldFmt?.pattern ?: "#.###")
            }

            Double::class -> {
                DoubleCsvConverter(locale, fieldFmt?.pattern ?: "#.######")
            }

            String::class -> {
                StringCsvConverter()
            }

            BigDecimal::class -> {
                BigDecimalCsvConverter(locale, fieldFmt?.pattern ?: "#.###########")
            }

            LocalDate::class -> {
                LocalDateCsvConverter(locale, fieldFmt?.pattern ?: "YYYY-MM-dd")
            }

            LocalDateTime::class -> {
                LocalDateTimeCsvConverter(locale, fieldFmt?.pattern ?: "YYYY-MM-dd HH:mm:ss")
            }

            Boolean::class -> {
                val boolFmt = getAnnotation<BooleanCsvField>(member, propertyMap) ?: BooleanCsvField()
                BooleanCsvConverter(boolFmt.trueValues.toList(), boolFmt.falseValues.toList())
            }

            else -> {
                throw CsvUnsupportedTypeException(
                    entityClass,
                    member.name,
                    member.type,
                )
            }
        }
    }
}
