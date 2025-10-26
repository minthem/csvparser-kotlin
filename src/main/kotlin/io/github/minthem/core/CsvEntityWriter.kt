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

    fun writeHeader() {
        val header: List<String> =
            valueProperties.mapIndexed { index, cell ->
                cell?.headerName ?: "Column_${index + 1}"
            }

        writer.writeHeader(header)
    }

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
            } else if (index == -1) {
                unIndexProperties.add(cell)
            } else {
                throw CsvEntityMappingException(
                    entityClass,
                    "Invalid index $index: ${param.name} must be positive or -1",
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
            Int::class -> IntCsvConverter(locale, fieldFmt?.pattern ?: "#")
            Long::class -> LongCsvConverter(locale, fieldFmt?.pattern ?: "#")
            Short::class -> ShortCsvConverter(locale, fieldFmt?.pattern ?: "#")
            Byte::class -> ByteCsvConverter(locale, fieldFmt?.pattern ?: "#")
            Float::class -> FloatCsvConverter(locale, fieldFmt?.pattern ?: "#.###")
            Double::class -> DoubleCsvConverter(locale, fieldFmt?.pattern ?: "#.######")
            String::class -> StringCsvConverter()
            BigDecimal::class -> BigDecimalCsvConverter(locale, fieldFmt?.pattern ?: "#.###########")
            LocalDate::class -> LocalDateCsvConverter(locale, fieldFmt?.pattern ?: "YYYY-MM-dd")
            LocalDateTime::class -> LocalDateTimeCsvConverter(locale, fieldFmt?.pattern ?: "YYYY-MM-dd HH:mm:ss")
            Boolean::class -> {
                val boolFmt = property.findAnnotation<BooleanCsvField>() ?: BooleanCsvField()
                BooleanCsvConverter(boolFmt.trueValues.toList(), boolFmt.falseValues.toList())
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
