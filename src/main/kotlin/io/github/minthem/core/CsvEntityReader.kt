package io.github.minthem.core

import io.github.minthem.annotation.CsvField
import io.github.minthem.config.CsvConfig
import io.github.minthem.config.ReaderConfig
import io.github.minthem.converter.CsvConverter
import io.github.minthem.exception.CsvEntityConstructionException
import io.github.minthem.exception.CsvEntityMappingException
import io.github.minthem.exception.CsvFieldConvertException
import io.github.minthem.exception.CsvFieldIndexOutOfRangeException
import io.github.minthem.exception.CsvFieldNotFoundInHeaderException
import io.github.minthem.exception.CsvUnsupportedTypeException
import java.io.Reader
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
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

    private val paramMap: Map<Int, Pair<KParameter, CsvConverter<*>>> by lazy { init() }

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

    private fun init(): Map<Int, Pair<KParameter, CsvConverter<*>>> {
        val parameterMap = mutableMapOf<Int, Pair<KParameter, CsvConverter<*>>>()
        val header = csvReader.header()
        val constructor =
            entityClass.primaryConstructor
                ?: throw CsvEntityMappingException(entityClass, "Entity class must have primary constructor.")

        val propertyMap = entityClass.memberProperties.associateBy { it.name }

        for (parameter in constructor.parameters) {
            parameter.findAnnotation<CsvField>()?.let {
                val index = resolveHeaderIndex(header, it, parameter)
                val converter = CsvConverterHelper.resolve(entityClass, parameter)
                parameterMap[index] = (parameter to converter)
                continue
            }

            propertyMap[parameter.name]?.let { prop ->
                prop.findAnnotation<CsvField>()?.let { anno ->
                    val index = resolveHeaderIndex(header, anno, parameter)
                    val converter = CsvConverterHelper.resolve(entityClass, prop)
                    parameterMap[index] = (parameter to converter)
                    continue
                }
            }

            if(!parameter.isOptional){
                throw CsvEntityMappingException(
                    entityClass,
                    "Parameter ${parameter.name} must be annotated with @CsvField or must be optional.",
                )
            } else {
                continue
            }
        }

        return parameterMap
    }

    private fun resolveHeaderIndex(
        header: List<String>?,
        csvField: CsvField,
        parameter: KParameter,
    ): Int {
        val explicitIndex = csvField.index
        if (explicitIndex < 0) {
            throw CsvFieldIndexOutOfRangeException(
                entityClass,
                parameter.name,
                explicitIndex,
                header?.size,
            )
        }

        // index > 0 → 1-based positional
        if (explicitIndex > 0) {
            val zeroBased = explicitIndex - 1
            if (header != null && zeroBased >= header.size) {
                throw CsvFieldIndexOutOfRangeException(
                    entityClass,
                    parameter.name,
                    explicitIndex,
                    header.size,
                )
            }
            return zeroBased
        }

        // index == 0 → resolve by header name
        if (header == null) {
            throw CsvEntityMappingException(
                entityClass,
                "Header is required to resolve parameter ${parameter.name} by name. Enable ReaderConfig.hasHeader or specify index > 0.",
            )
        }

        val effectiveName = csvField.name.ifBlank { parameter.name ?: "" }
        val idx = header.indexOf(effectiveName)
        if (idx < 0) {
            throw CsvFieldNotFoundInHeaderException(
                entityClass,
                parameter.name,
                effectiveName,
            )
        }
        return idx
    }
}
