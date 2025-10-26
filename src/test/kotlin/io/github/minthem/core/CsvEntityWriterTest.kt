package io.github.minthem.core

import io.github.minthem.annotation.BooleanCsvField
import io.github.minthem.annotation.CsvField
import io.github.minthem.annotation.CsvFieldFormat
import io.github.minthem.config.CsvConfig
import io.github.minthem.config.WriterConfig
import io.github.minthem.exception.CsvEntityMappingException
import io.github.minthem.exception.CsvFieldConvertException
import io.github.minthem.exception.CsvUnsupportedTypeException
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CsvEntityWriterTest {
    @Test
    fun `should write CSV correctly`() {
        data class Person(
            @CsvField("name")
            val name: String,
            @CsvField("age")
            val age: Int,
            @CsvField("city")
            val city: String,
        )

        val out = StringBuilder()
        val writer =
            CsvEntityWriter(
                Person::class,
                out,
                CsvConfig(),
                writeConfig = WriterConfig(WriterConfig.LineSeparator.CRLF),
            )
        writer.writeRow(Person("Alice", 30, "Tokyo"))
        writer.writeRow(Person("Bob", 25, "Osaka"))
        assertEquals("Alice,30,Tokyo\r\nBob,25,Osaka\r\n", out.toString())
    }

    @Test
    fun `should write CSV correctly with header`() {
        data class Person(
            @CsvField("Name")
            val name: String,
            @CsvField("Age")
            val age: Int,
            @CsvField("City")
            val city: String,
        )

        val out = StringBuilder()
        val writer =
            CsvEntityWriter(
                Person::class,
                out,
                CsvConfig(),
                writeConfig = WriterConfig(WriterConfig.LineSeparator.CRLF),
            )
        writer.writeHeader()
        writer.writeRow(Person("Alice", 30, "Tokyo"))
        writer.writeRow(Person("Bob", 25, "Osaka"))
        assertEquals("Name,Age,City\r\nAlice,30,Tokyo\r\nBob,25,Osaka\r\n", out.toString())
    }

    @Test
    fun `should write CSV correctly with index`() {
        data class Person(
            @CsvField("name", index = 2)
            val name: String,
            @CsvField("age", index = 3)
            val age: Int,
            @CsvField("city", index = 1)
            val city: String,
        )

        val out = StringBuilder()
        val writer =
            CsvEntityWriter(
                Person::class,
                out,
                CsvConfig(),
                writeConfig = WriterConfig(WriterConfig.LineSeparator.CRLF),
            )
        writer.writeHeader()
        writer.writeRow(Person("Alice", 30, "Tokyo"))
        writer.writeRow(Person("Bob", 25, "Osaka"))
        assertEquals("city,name,age\r\nTokyo,Alice,30\r\nOsaka,Bob,25\r\n", out.toString())
    }

    @Test
    fun `should write CSV correctly index only`() {
        data class Person(
            @CsvField(index = 1)
            val name: String,
            @CsvField(index = 2)
            val age: Int,
            @CsvField(index = 3)
            val city: String,
        )

        val out = StringBuilder()
        val writer =
            CsvEntityWriter(
                Person::class,
                out,
                CsvConfig(),
                writeConfig = WriterConfig(WriterConfig.LineSeparator.CRLF),
            )
        writer.writeHeader()
        writer.writeRow(Person("Alice", 30, "Tokyo"))
        writer.writeRow(Person("Bob", 25, "Osaka"))
        assertEquals("name,age,city\r\nAlice,30,Tokyo\r\nBob,25,Osaka\r\n", out.toString())
    }

    @Test
    fun `should write CSV correctly with sparse index`() {
        data class Person(
            @CsvField("name", index = 2)
            val name: String,
            @CsvField("age", index = 4)
            val age: Int,
            @CsvField("city", index = 6)
            val city: String,
        )

        val out = StringBuilder()
        val writer =
            CsvEntityWriter(
                Person::class,
                out,
                CsvConfig(),
                writeConfig = WriterConfig(WriterConfig.LineSeparator.CRLF),
            )
        writer.writeHeader()
        writer.writeRow(Person("Alice", 30, "Tokyo"))
        writer.writeRow(Person("Bob", 25, "Osaka"))
        assertEquals(
            "Column_1,name,Column_3,age,Column_5,city\r\n,Alice,,30,,Tokyo\r\n,Bob,,25,,Osaka\r\n",
            out.toString(),
        )
    }

    @Test
    fun `should convert numbers dates booleans with format`() {
        data class Rec(
            @CsvField("int") @CsvFieldFormat(pattern = "#,###", locale = "en") val int: Int,
            @CsvField("long") @CsvFieldFormat(pattern = "#,###", locale = "en") val long: Long,
            @CsvField("short") @CsvFieldFormat(pattern = "#,###", locale = "en") val short: Short,
            @CsvField("byte") @CsvFieldFormat(pattern = "#,##", locale = "en") val byte: Byte,
            @CsvField("float") @CsvFieldFormat(pattern = "#,###.###", locale = "en") val float: Float,
            @CsvField("double") @CsvFieldFormat(pattern = "#,###.#####", locale = "en") val double: Double,
            @CsvField("string") val string: String,
            @CsvField("date") @CsvFieldFormat(pattern = "yyyy-MM-dd", locale = "en") val date: LocalDate,
            @CsvField("datetime") @CsvFieldFormat(
                pattern = "yyyy-MM-dd HH:mm:ss",
                locale = "en",
            ) val datetime: LocalDateTime,
            @CsvField("bool") @BooleanCsvField(
                trueValues = ["true", "1", "yes"],
                falseValues = ["false", "0", "no"],
            ) val bool: Boolean,
            @CsvField("bigdecimal") @CsvFieldFormat(pattern = "#.#####", locale = "en") val bigdecimal: BigDecimal,
        )

        val out = StringBuilder()
        val writer = CsvEntityWriter(Rec::class, out, CsvConfig())
        val entity =
            Rec(
                10000000,
                2000000000000,
                30000,
                100,
                1234.567f,
                12345.6789,
                "STRING",
                LocalDate.of(2024, 1, 2),
                LocalDateTime.of(2024, 1, 2, 13, 45, 0),
                true,
                BigDecimal("12345.67890"),
            )

        val expected =
            """
            int,long,short,byte,float,double,string,date,datetime,bool,bigdecimal
            "10,000,000","2,000,000,000,000","30,000","1,00","1,234.567","12,345.6789",STRING,2024-01-02,2024-01-02 13:45:00,true,12345.6789
            
            """.trimIndent()

        writer.writeHeader()
        writer.writeRow(entity)

        assertEquals(expected, out.toString())
    }

    @Test
    fun `should work with TSV by changing delimiter`() {
        data class Person(
            @CsvField("name") val name: String,
            @CsvField("age") val age: Int,
            @CsvField("city") val city: String,
        )

        val out = StringBuilder()
        val writer =
            CsvEntityWriter(
                Person::class,
                out,
                CsvConfig(delimiter = '\t'),
                writeConfig = WriterConfig(WriterConfig.LineSeparator.LF),
            )

        writer.writeHeader()
        writer.writeRow(Person("Alice", 30, "Tokyo"))
        writer.writeRow(Person("Bob", 25, "Osaka"))

        val expected =
            """
            name\tage\tcity
            Alice\t30\tTokyo
            Bob\t25\tOsaka
            
            """.trimIndent().replace("\\t", "\t")

        assertEquals(expected, out.toString())
    }

    @Test
    fun `should write has no output field`() {
        @Suppress("unused")
        class Person(
            @CsvField("name")
            val name: String,
            onlyConstructor: Short,
            @CsvField("age")
            val age: Int,
            val noOutputField: Long,
            @CsvField("city")
            val city: String,
        )

        val out = StringBuilder()
        val writer =
            CsvEntityWriter(
                Person::class,
                out,
                CsvConfig(),
                writeConfig = WriterConfig(WriterConfig.LineSeparator.CRLF),
            )
        writer.writeRow(Person("Alice", 1, 30, 1, "Tokyo"))
        writer.writeRow(Person("Bob", 1, 25, 1, "Osaka"))
        assertEquals("Alice,30,Tokyo\r\nBob,25,Osaka\r\n", out.toString())
    }

    @Test
    fun `should throw CsvUnsupportedTypeException for unsupported parameter type`() {
        class UnsupportedType

        data class Person(
            @CsvField("data") val data: UnsupportedType,
        )

        val writer =
            CsvEntityWriter(
                Person::class,
                StringBuilder(),
                CsvConfig(),
                writeConfig = WriterConfig(WriterConfig.LineSeparator.CRLF),
            )

        assertFailsWith<CsvUnsupportedTypeException> {
            writer.writeRow(Person(UnsupportedType()))
        }
    }

    @Test
    fun `should throw CsvEntityMappingException for no CsvField annotation`() {
        data class Person(
            val id: String,
        )

        val writer =
            CsvEntityWriter(
                Person::class,
                StringBuilder(),
                CsvConfig(),
                writeConfig = WriterConfig(WriterConfig.LineSeparator.CRLF),
            )

        assertFailsWith<CsvEntityMappingException> {
            writer.writeRow(Person("123"))
        }
    }

    @Test
    fun `should throw CsvEntityMappingException for less than 0 CsvField index`() {
        data class Person(
            @CsvField(index = -1) val id: String,
        )

        val writer =
            CsvEntityWriter(
                Person::class,
                StringBuilder(),
                CsvConfig(),
                writeConfig = WriterConfig(WriterConfig.LineSeparator.CRLF),
            )
        assertFailsWith<CsvEntityMappingException> {
            writer.writeRow(Person("123"))
        }
    }

    @Test
    fun `should throw CsvEntityMappingException for duplicate CsvField index`() {
        data class Person(
            @CsvField(index = 1) val id: String,
            @CsvField(index = 1) val name: String,
        )

        val writer =
            CsvEntityWriter(
                Person::class,
                StringBuilder(),
                CsvConfig(),
                writeConfig = WriterConfig(WriterConfig.LineSeparator.CRLF),
            )

        assertFailsWith<CsvEntityMappingException> {
            writer.writeRow(Person("123", "Alice"))
        }
    }

    @Test
    fun `should throw CsvFieldConvertException for invalid CsvFieldFormat`() {
        data class Person(
            @CsvField("name") @CsvFieldFormat(pattern = "invalid") val date: LocalDate,
        )

        val writer =
            CsvEntityWriter(
                Person::class,
                StringBuilder(),
                CsvConfig(),
                writeConfig = WriterConfig(WriterConfig.LineSeparator.CRLF),
            )

        assertFailsWith<CsvFieldConvertException> {
            writer.writeRow(Person(LocalDate.of(2024, 1, 2)))
        }
    }
}
