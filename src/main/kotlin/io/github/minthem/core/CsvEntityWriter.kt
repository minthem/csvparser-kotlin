package io.github.minthem.core

import io.github.minthem.annotation.BooleanCsvField
import io.github.minthem.annotation.CsvField
import io.github.minthem.annotation.CsvFieldFormat
import io.github.minthem.config.CsvConfig
import io.github.minthem.config.WriterConfig
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
import io.github.minthem.exception.CsvEntityMappingException
import io.github.minthem.exception.CsvFieldConvertException
import io.github.minthem.exception.CsvUnsupportedTypeException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Writes Kotlin data classes to CSV/TSV using annotations on properties.
 *
 * Mapping rules:
 * - The target [entityClass] must have a primary constructor and properties matching its parameters.
 * - Properties annotated with `@CsvField` are written. Column order is determined by
 *   `@CsvField.index` when greater than 0; otherwise, remaining properties are placed sequentially.
 * - Column names use `@CsvField.name` when provided; otherwise, the property name.
 * - Values are converted to text via type-specific converters. You can control formatting with
 *   `@CsvFieldFormat` (numbers/dates) and `@BooleanCsvField`.
 *
 * Behavior:
 * - You may call [writeHeader] to output a header based on resolved column names.
 * - Use [writeRow] to serialize an entity instance as one CSV line.
 *
 * Errors:
 * - [io.github.minthem.exception.CsvEntityMappingException] when indexes are duplicated/invalid
 *   or no columns are found.
 * - [io.github.minthem.exception.CsvFieldConvertException] when a value cannot be converted.
 * - [io.github.minthem.exception.CsvUnsupportedTypeException] for unsupported property types.
 *
 * @param entityClass target entity class whose annotated properties are written
 * @param out destination appendable
 * @param config CSV behavior such as delimiter, quote char, and nullValue
 * @param writeConfig writer options such as line separator
 */
class CsvEntityWriter<T : Any>(
    private val entityClass: KClass<T>,
    private val out: Appendable,
    val config: CsvConfig,
    val writeConfig: WriterConfig = WriterConfig(),
) {
    private val writer = CsvWriter(out, config, writeConfig)
    private val formatter = CsvLineFormatter(config.delimiter, config.quoteChar)

    private val valueProperties: List<Cell?> by lazy {
        buildHeaderIndexMap()
    }

    /**
     * Writes a header row using resolved column names.
     *
     * Column names come from `@CsvField.name` when non-blank, otherwise the property name.
     * For gaps with no mapped property at an index, a default name like `Column_1` is used.
     *
     * Delegates to [CsvWriter.writeHeader]. See it for header validation rules.
     */
    fun writeHeader() {
        val header: List<String> =
            valueProperties.mapIndexed { index, cell ->
                cell?.headerName ?: "Column_${index + 1}"
            }

        writer.writeHeader(header)
    }

    /**
     * Writes one entity instance as a CSV row.
     *
     * For each property annotated with `@CsvField`, the value is read and converted
     * according to the resolved converter. Null values are emitted as [CsvConfig.nullValue].
     *
     * @throws io.github.minthem.exception.CsvFieldConvertException when conversion fails
     * @throws io.github.minthem.exception.CsvUnsupportedTypeException when the property type is unsupported
     */
    fun writeRow(entity: T) {
        val values: List<String?> =
            valueProperties.mapIndexed { idx, cell ->
                cell?.let {
                    val v: Any? = it.prop.getter.call(entity)
                    it.serialize(v).getOrElse { cause ->
                        val columnName = it.headerName
                        throw CsvFieldConvertException(
                            entityClass = entityClass,
                            paramName = it.prop.name,
                            columnName = columnName,
                            columnIndex = idx + 1,
                            cause = cause,
                        )
                    }
                }
            }
        val nullToken = config.nullValue
        val line = formatter.join(values, nullToken)
        out.append(line).append(writeConfig.lineSeparator.value)
    }

    private fun buildHeaderIndexMap(): List<Cell?> {
        val ctor = entityClass.primaryConstructor!!
        val params = ctor.parameters
        val props = entityClass.memberProperties.associateBy { it.name }

        val headerIndexMap = mutableMapOf<Int, Cell>()
        val unIndexProperties = mutableListOf<Cell>()

        for (param in params) {
            val prop = props[param.name] ?: continue
            val csvField = prop.findAnnotation<CsvField>() ?: continue
            val headerName = csvField.name.ifBlank { prop.name }
            val index = csvField.index
            val cell = createCell(headerName, prop)

            if (0 < index) {
                if (headerIndexMap.containsKey(index)) {
                    val existing = headerIndexMap[index]!!.prop.name
                    throw CsvEntityMappingException(
                        entityClass,
                        "Duplicate index $index: ${param.name} conflicts with $existing",
                    )
                }
                headerIndexMap[index] = cell
            } else if (index == 0) {
                unIndexProperties.add(cell)
            } else {
                throw CsvEntityMappingException(
                    entityClass,
                    "Invalid index $index: ${param.name} must be non-negative (0 for auto, >0 for explicit position)",
                )
            }
        }

        for (cell in unIndexProperties) {
            val nextIndex = nextAvailableIndex(headerIndexMap.keys)
            headerIndexMap[nextIndex] = cell
        }

        val maxIndex =
            headerIndexMap.keys.maxOrNull()
                ?: throw CsvEntityMappingException(entityClass, "No header cell found")
        return (1..maxIndex).map { headerIndexMap[it] }
    }

    private fun nextAvailableIndex(used: Set<Int>): Int {
        val free = generateSequence(1) { it + 1 }.firstOrNull { it !in used }
        return free ?: (used.maxOrNull()?.plus(1) ?: 1)
    }

    private fun createCell(
        headerName: String,
        prop: KProperty1<*, *>,
    ): Cell {
        val converter = resolveConverter(prop)
        return Cell(headerName, prop, converter)
    }

    private fun resolveConverter(property: KProperty1<*, *>): CsvConverter<*> {
        val fieldFmt = property.findAnnotation<CsvFieldFormat>()
        val locale =
            if (fieldFmt?.locale.isNullOrBlank()) {
                Locale.getDefault()
            } else {
                Locale.forLanguageTag(fieldFmt.locale)
            }

        return when (property.returnType.classifier) {
            Int::class ->
                IntCsvConverter
                    .Builder()
                    .locale(locale)
                    .pattern(fieldFmt?.pattern ?: "#")
                    .build()
            Long::class ->
                LongCsvConverter
                    .Builder()
                    .locale(locale)
                    .pattern(fieldFmt?.pattern ?: "#")
                    .build()
            Short::class ->
                ShortCsvConverter
                    .Builder()
                    .locale(locale)
                    .pattern(fieldFmt?.pattern ?: "#")
                    .build()
            Byte::class ->
                ByteCsvConverter
                    .Builder()
                    .locale(locale)
                    .pattern(fieldFmt?.pattern ?: "#")
                    .build()
            Float::class ->
                FloatCsvConverter
                    .Builder()
                    .locale(locale)
                    .pattern(fieldFmt?.pattern ?: "#.###")
                    .build()
            Double::class ->
                DoubleCsvConverter
                    .Builder()
                    .locale(locale)
                    .pattern(fieldFmt?.pattern ?: "#.######")
                    .build()
            String::class -> StringCsvConverter()
            BigDecimal::class ->
                BigDecimalCsvConverter
                    .Builder()
                    .locale(locale)
                    .pattern(fieldFmt?.pattern ?: "#.###########")
                    .build()
            LocalDate::class ->
                LocalDateCsvConverter
                    .Builder()
                    .locale(locale)
                    .pattern(fieldFmt?.pattern ?: "YYYY-MM-dd")
                    .build()
            LocalDateTime::class ->
                LocalDateTimeCsvConverter
                    .Builder()
                    .locale(locale)
                    .pattern(fieldFmt?.pattern ?: "YYYY-MM-dd HH:mm:ss")
                    .build()
            Boolean::class -> {
                val boolFmt = property.findAnnotation<BooleanCsvField>() ?: BooleanCsvField()
                BooleanCsvConverter
                    .Builder()
                    .trueValues(boolFmt.trueValues.toList())
                    .falseValues(boolFmt.falseValues.toList())
                    .caseSensitive(!boolFmt.ignoreCase)
                    .build()
            }

            else -> {
                throw CsvUnsupportedTypeException(
                    entityClass,
                    property.name,
                    property.returnType,
                )
            }
        }
    }
}

private class Cell(
    val headerName: String,
    val prop: KProperty1<*, *>,
    private val converter: CsvConverter<*>,
) {
    @Suppress("UNCHECKED_CAST")
    fun serialize(value: Any?): Result<String?> =
        when (converter) {
            is IntCsvConverter ->
                (converter as CsvConverter<Int>).serialize(value as Int?)

            is LongCsvConverter ->
                (converter as CsvConverter<Long>).serialize(value as Long?)

            is ShortCsvConverter ->
                (converter as CsvConverter<Short>).serialize(value as Short?)

            is ByteCsvConverter ->
                (converter as CsvConverter<Byte>).serialize(value as Byte?)

            is FloatCsvConverter ->
                (converter as CsvConverter<Float>).serialize(value as Float?)

            is DoubleCsvConverter ->
                (converter as CsvConverter<Double>).serialize(value as Double?)

            is StringCsvConverter ->
                (converter as CsvConverter<String>).serialize(value as String?)

            is BigDecimalCsvConverter ->
                (converter as CsvConverter<BigDecimal>).serialize(value as BigDecimal?)

            is LocalDateCsvConverter ->
                (converter as CsvConverter<LocalDate>).serialize(value as LocalDate?)

            is LocalDateTimeCsvConverter ->
                (converter as CsvConverter<LocalDateTime>).serialize(value as LocalDateTime?)

            is BooleanCsvConverter ->
                (converter as CsvConverter<Boolean>).serialize(value as Boolean?)

            else ->
                Result.failure(
                    CsvUnsupportedTypeException(
                        entityClass = prop.returnType.classifier as? KClass<*> ?: Any::class,
                        paramName = prop.name,
                        type = prop.returnType,
                    ),
                )
        }
}
