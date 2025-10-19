package io.github.minthem.core

import io.github.minthem.annotation.BooleanCsvField
import io.github.minthem.annotation.CsvField
import io.github.minthem.annotation.CsvFieldFormat
import io.github.minthem.config.CsvConfig
import io.github.minthem.config.ReaderConfig
import io.github.minthem.exception.CsvEntityConstructionException
import io.github.minthem.exception.CsvEntityMappingException
import io.github.minthem.exception.CsvFieldConvertException
import io.github.minthem.exception.CsvFieldIndexOutOfRangeException
import io.github.minthem.exception.CsvFieldNotFoundInHeaderException
import io.github.minthem.exception.CsvLineFormatException
import io.github.minthem.exception.CsvUnsupportedTypeException
import org.junit.jupiter.api.Test
import java.io.StringReader
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CsvEntityReaderTest {
    @Test
    fun `should read CSV correctly`() {
        val csv =
            """
            name,age,city
            Alice,30,Tokyo
            Bob,25,Osaka
            """.trimIndent()

        data class Person(
            @CsvField("name")
            val name: String,
            @CsvField("age")
            val age: Int,
            @CsvField("city")
            val city: String,
        )

        val reader = CsvEntityReader(Person::class, StringReader(csv), readConfig = ReaderConfig(hasHeader = true))
        val items = reader.toList()
        assertEquals(Person("Alice", 30, "Tokyo"), items[0])
        assertEquals(Person("Bob", 25, "Osaka"), items[1])
    }

    @Test
    fun `should read CSV without header using index`() {
        val csv =
            """
            Alice,30,Tokyo
            Bob,25,Osaka
            """.trimIndent()

        data class Person(
            @CsvField(index = 0)
            val name: String,
            @CsvField(index = 1)
            val age: Int,
            @CsvField(index = 2)
            val city: String,
        )

        val reader = CsvEntityReader(Person::class, StringReader(csv), readConfig = ReaderConfig(hasHeader = false))
        val items = reader.toList()
        assertEquals(Person("Alice", 30, "Tokyo"), items[0])
        assertEquals(Person("Bob", 25, "Osaka"), items[1])
    }

    @Test
    fun `should skip rows when skipRows is set`() {
        val csv =
            """
            comment line
            name,age
            Alice,30
            """.trimIndent()

        data class Person(
            @CsvField("name") val name: String,
            @CsvField("age") val age: Int,
        )

        val reader =
            CsvEntityReader(
                Person::class,
                StringReader(csv),
                readConfig = ReaderConfig(hasHeader = true, skipRows = 1),
            )
        val items = reader.toList()
        assertEquals(Person("Alice", 30), items[0])
    }

    @Test
    fun `should throw when row column count does not match header`() {
        val csv =
            """
            name,age
            Alice,30
            Bob
            """.trimIndent()

        data class Person(
            @CsvField("name") val name: String,
            @CsvField("age") val age: Int,
        )

        val reader = CsvEntityReader(Person::class, StringReader(csv), readConfig = ReaderConfig(hasHeader = true))
        val iter = reader.iterator()
        iter.next()

        assertFailsWith<CsvLineFormatException> {
            iter.next()
        }
    }

    @Test
    fun `should skip blank line when ignoreBlankLine is true`() {
        val csv = "name,age\nAlice,30\n\nBob,25"

        data class Person(
            @CsvField("name") val name: String,
            @CsvField("age") val age: Int,
        )

        val reader =
            CsvEntityReader(
                Person::class,
                StringReader(csv),
                readConfig = ReaderConfig(hasHeader = true, ignoreBlankLine = true),
            )

        val items = reader.toList()
        assertEquals(2, items.size)
        assertEquals("Alice", items[0].name)
        assertEquals("Bob", items[1].name)
    }

    @Test
    fun `should support property-level annotations as well as parameter-level`() {
        val csv =
            """
            name,age,city
            Alice,30,Tokyo
            """.trimIndent()

        data class PersonPropAnnotated(
            @param:CsvField("name") val name: String,
            @param:CsvField("age") val age: Int,
            @param:CsvField("city") val city: String,
        )

        val reader =
            CsvEntityReader(PersonPropAnnotated::class, StringReader(csv), readConfig = ReaderConfig(hasHeader = true))
        val items = reader.toList()
        assertEquals(PersonPropAnnotated("Alice", 30, "Tokyo"), items[0])
    }

    @Test
    fun `should convert numbers dates booleans with format`() {
        val csv =
            """
            int,long,short,byte,float,double,string,date,datetime,bool,bigdecimal
            "10,000,000","2,000,000,000,000","30,000","1,00","1,234.567","12,345.6789",STRING,2024-01-02,2024-01-02 13:45:00,true,12345.6789
            """.trimIndent()

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

        val reader = CsvEntityReader(Rec::class, StringReader(csv), readConfig = ReaderConfig(hasHeader = true))
        val items = reader.toList()
        val actual = items[0]
        val expected =
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

        assertEquals(expected, actual)
    }

    @Test
    fun `should convert numbers dates booleans with optional value`() {
        val csv =
            """
            int,long,short,byte,float,double,string,date,datetime,bool,bigdecimal
            10000000,,30000,,1234.567,,STRING,,2024-01-02 13:45:00,,12345.6789
            ,2000000000000,,100,,12345.6789,,2024-01-02,,true,
            """.trimIndent()

        data class Rec(
            @CsvField("int") val int: Int = 99_999,
            @CsvField("long") val long: Long = 999_999,
            @CsvField("short") val short: Short = 9_999,
            @CsvField("byte") val byte: Byte = 99,
            @CsvField("float") val float: Float = 9999.999f,
            @CsvField("double") val double: Double = 99999999.9999,
            @CsvField("string") val string: String = "default",
            @CsvField("date") @CsvFieldFormat(pattern = "yyyy-MM-dd", locale = "en") val date: LocalDate = LocalDate.of(2024, 1, 1),
            @CsvField("datetime") @CsvFieldFormat(
                pattern = "yyyy-MM-dd HH:mm:ss",
                locale = "en",
            ) val datetime: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0),
            @CsvField("bool") val bool: Boolean = false,
            @CsvField("bigdecimal") val bigdecimal: BigDecimal = BigDecimal("99999.99999"),
        )

        val reader = CsvEntityReader(Rec::class, StringReader(csv), readConfig = ReaderConfig(hasHeader = true))
        val actual = reader.toList()

        val expected =
            listOf(
                Rec(
                    int = 10000000,
                    short = 30000,
                    float = 1234.567f,
                    string = "STRING",
                    datetime = LocalDateTime.of(2024, 1, 2, 13, 45, 0),
                    bigdecimal = BigDecimal("12345.67890000000"),
                ),
                Rec(
                    long = 2000000000000,
                    byte = 100,
                    double = 12345.6789,
                    date = LocalDate.of(2024, 1, 2),
                    bool = true,
                ),
            )

        assertEquals(expected, actual)
    }

    @Test
    fun `should work with TSV by changing delimiter`() {
        val tsv =
            """
            name\tage\tcity
            Alice\t30\tTokyo
            Bob\t25\tOsaka
            """.trimIndent().replace("\\t", "\t")

        data class Person(
            @CsvField("name") val name: String,
            @CsvField("age") val age: Int,
            @CsvField("city") val city: String,
        )

        val reader =
            CsvEntityReader(
                Person::class,
                StringReader(tsv),
                config = CsvConfig(delimiter = '\t'),
                readConfig = ReaderConfig(hasHeader = true),
            )

        val items = reader.toList()
        assertEquals(Person("Alice", 30, "Tokyo"), items[0])
        assertEquals(Person("Bob", 25, "Osaka"), items[1])
    }

    @Test
    fun `should throw CsvFieldNotFoundInHeaderException when header lacks annotated name`() {
        val csv =
            """
            name,age
            Alice,30
            """.trimIndent()

        data class Person(
            @CsvField("fullname") val name: String,
            @CsvField("age") val age: Int,
        )

        assertFailsWith<CsvFieldNotFoundInHeaderException> {
            CsvEntityReader(Person::class, StringReader(csv), readConfig = ReaderConfig(hasHeader = true))
                .toList()
        }
    }

    @Test
    fun `should throw CsvFieldIndexOutOfRangeException when index is negative`() {
        val csv =
            """
            Alice,30
            """.trimIndent()

        data class Person(
            @CsvField(index = -1) val name: String,
            @CsvField(index = 1) val age: Int,
        )

        assertFailsWith<CsvFieldIndexOutOfRangeException> {
            CsvEntityReader(Person::class, StringReader(csv), readConfig = ReaderConfig(hasHeader = false))
                .toList()
        }
    }

    @Test
    fun `should throw CsvUnsupportedTypeException for unsupported parameter type`() {
        val csv =
            """
            data
            X
            """.trimIndent()

        class UnsupportedType

        data class Person(
            @CsvField("data") val data: UnsupportedType,
        )

        assertFailsWith<CsvUnsupportedTypeException> {
            CsvEntityReader(Person::class, StringReader(csv), readConfig = ReaderConfig(hasHeader = true))
                .toList()
        }
    }

    @Test
    fun `should throw CsvEntityMappingException when required parameter lacks CsvField`() {
        val csv =
            """
            name,age
            Alice,30
            """.trimIndent()

        data class Person(
            @CsvField("name") val name: String,
            val age: Int,
        )

        assertFailsWith<CsvEntityMappingException> {
            CsvEntityReader(Person::class, StringReader(csv), readConfig = ReaderConfig(hasHeader = true))
                .toList()
        }
    }

    @Test
    fun `should throw CsvEntityConstructionException when constructor invocation fails`() {
        val csv =
            """
            name,age
            Alice,1
            """.trimIndent()

        data class Person(
            @CsvField("name") val name: String,
            @CsvField("age") val age: Int,
        ) {
            init {
                if (age == 1) error("boom")
            }
        }

        assertFailsWith<CsvEntityConstructionException> {
            CsvEntityReader(Person::class, StringReader(csv), readConfig = ReaderConfig(hasHeader = true))
                .toList()
        }
    }

    @Test
    fun `should throw CsvFieldConvertException when value conversion fails`() {
        val csv =
            """
            name,age
            Alice,not-a-number
            """.trimIndent()

        data class Person(
            @CsvField("name") val name: String,
            @CsvField("age") val age: Int,
        )

        assertFailsWith<CsvFieldConvertException> {
            CsvEntityReader(Person::class, StringReader(csv), readConfig = ReaderConfig(hasHeader = true))
                .toList()
        }
    }

    @Test
    fun `should throw CsvEntityMappingException when primary constructor is missing`() {
        val csv =
            """
            name,age
            Alice,30
            """.trimIndent()

        class NoPrimaryConstructor {
            val name: String
            val age: Int

            @Suppress("unused")
            constructor(name: String, age: Int) {
                this.name = name
                this.age = age
            }
        }

        assertFailsWith<CsvEntityMappingException> {
            CsvEntityReader(NoPrimaryConstructor::class, StringReader(csv), readConfig = ReaderConfig(hasHeader = true))
                .toList()
        }
    }
}
