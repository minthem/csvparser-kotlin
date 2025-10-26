package io.github.minthem.annotation

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.full.findAnnotation
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

data class Sample(
    @CsvField(name = "id", index = 0)
    val id: Int,
    @CsvFieldFormat(pattern = "yyyy-MM-dd", locale = "en_US")
    @CsvField(name = "date")
    val date: LocalDate,
    @CsvFieldFormat(pattern = "yyyy-MM-dd'T'HH:MM:SS")
    @CsvField(index = 2)
    val datetime: LocalDateTime,
    @BooleanCsvField(ignoreCase = false)
    @CsvField(name = "active", index = 3)
    val active: Boolean,
)

class CsvAnnotationTest {
    @Test
    fun `id property should have correct CsvField annotation`() {
        val prop = Sample::class.members.first { it.name == "id" }
        val ann = prop.findAnnotation<CsvField>()

        assertNotNull(ann)
        assertEquals("id", ann.name)
        assertEquals(0, ann.index)
    }

    @Test
    fun `date property should have correct CsvField and CsvFieldFormat annotations`() {
        val prop = Sample::class.members.first { it.name == "date" }
        val annField = prop.findAnnotation<CsvField>()
        val annFormat = prop.findAnnotation<CsvFieldFormat>()

        assertNotNull(annField)
        assertEquals("date", annField.name)
        assertEquals(0, annField.index)

        assertNotNull(annFormat)
        assertEquals("yyyy-MM-dd", annFormat.pattern)
        assertEquals("en_US", annFormat.locale)
    }

    @Test
    fun `datetime property should have correct CsvField and CsvFieldFormat annotations`() {
        val prop = Sample::class.members.first { it.name == "datetime" }
        val annField = prop.findAnnotation<CsvField>()
        val annFormat = prop.findAnnotation<CsvFieldFormat>()

        assertNotNull(annField)
        assertEquals("", annField.name)
        assertEquals(2, annField.index)

        assertNotNull(annFormat)
        assertEquals("yyyy-MM-dd'T'HH:MM:SS", annFormat.pattern)
        assertEquals("", annFormat.locale)
    }

    @Test
    fun `active property should have correct CsvField and BooleanCsvField annotations`() {
        val prop = Sample::class.members.first { it.name == "active" }
        val annField = prop.findAnnotation<CsvField>()
        val annFormat = prop.findAnnotation<BooleanCsvField>()

        assertNotNull(annField)
        assertEquals("active", annField.name)
        assertEquals(3, annField.index)

        assertNotNull(annFormat)
        assertEquals(arrayOf("true", "1", "yes", "y").toList(), annFormat.trueValues.toList())
        assertEquals(arrayOf("false", "0", "no", "n").toList(), annFormat.falseValues.toList())
        assertEquals(false, annFormat.ignoreCase)
    }
}
