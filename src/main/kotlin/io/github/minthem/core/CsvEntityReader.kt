package io.github.minthem.core

import io.github.minthem.annotation.BooleanCsvField
import io.github.minthem.annotation.CsvField
import io.github.minthem.annotation.CsvFieldFormat
import io.github.minthem.config.CsvConfig
import io.github.minthem.config.ReaderConfig
import io.github.minthem.converter.*
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
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

class CsvEntityReader<T : Any>(
    private val entityClass: KClass<T>,
    reader: Reader,
    config: CsvConfig = CsvConfig(),
    readConfig: ReaderConfig = ReaderConfig(),
) : Iterable<T> {
    private val csvReader = CsvReader(reader, config, readConfig)

    private var initialized = false
    private var paramMap: Map<Int, Pair<KParameter, CsvConverter<*>>> = mutableMapOf()

    override fun iterator(): Iterator<T> {
        init()
        val constructor = entityClass.primaryConstructor!!

        val sequence = sequence {
            for (row in csvReader) {
                val constructorParams = mutableMapOf<KParameter, Any?>()

                for ((index, pair) in paramMap) {
                    val (param, converter) = pair
                    val value = row.getOrNull(index)
                    val converted = try {
                        converter.deserialize(value).getOrThrow()
                    } catch (e: Exception) {
                        throw CsvFieldConvertException(
                            entityClass,
                            param.name,
                            csvReader.header()?.getOrNull(index),
                            index,
                            e
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
                        e
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
            val csvField = getAnnotation<CsvField>(parameter, classPropertiesMap) ?: if (!parameter.isOptional) {
                throw CsvEntityMappingException(
                    entityClass, "Parameter ${parameter.name} must be annotated with @CsvField or must be optional."
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
        propertyMap: Map<String, KProperty1<*, *>>
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
                csvField.index
            }

        if (index < 0) {
            throw CsvFieldIndexOutOfRangeException(
                entityClass,
                parameter.name,
                index,
                header?.size,
            )
        }

        return index
    }

    private fun resolveConverter(member: KParameter, propertyMap: Map<String, KProperty1<*, *>>): CsvConverter<*> {
        val fieldFmt = this.getAnnotation<CsvFieldFormat>(member, propertyMap)
        val locale = if (fieldFmt?.locale.isNullOrBlank()) {
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
                    member.type
                )
            }
        }
    }
}
