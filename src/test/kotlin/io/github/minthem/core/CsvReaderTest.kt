package io.github.minthem.core

import io.github.minthem.config.CsvConfig
import io.github.minthem.config.ReaderConfig
import io.github.minthem.exception.CsvFormatException
import io.github.minthem.exception.CsvLineFormatException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.StringReader

class CsvReaderTest {

    @Test
    fun `should read CSV with header correctly`() {
        val csv = """
            name,age,city
            Alice,30,Tokyo
            Bob,25,Osaka
        """.trimIndent()

        val reader = CsvReader(StringReader(csv), CsvConfig(), ReaderConfig(hasHeader = true))
        reader.header() shouldBe listOf("name", "age", "city")

        val rows = reader.toList()
        rows[0]["name"] shouldBe "Alice"
        rows[1]["city"] shouldBe "Osaka"
    }

    @Test
    fun `should read CSV without header correctly`() {
        val csv = """
            Alice,30,Tokyo
            Bob,25,Osaka
        """.trimIndent()

        val reader = CsvReader(StringReader(csv), CsvConfig(), ReaderConfig(hasHeader = false))
        reader.header() shouldBe null

        val rows = reader.toList()
        rows[0][0] shouldBe "Alice"
        rows[1][2] shouldBe "Osaka"
    }

    @Test
    fun `should skip rows when skipRows is set`() {
        val csv = """
            comment line
            name,age
            Alice,30
        """.trimIndent()

        val reader = CsvReader(StringReader(csv), CsvConfig(), ReaderConfig(hasHeader = true, skipRows = 1))
        reader.header() shouldBe listOf("name", "age")

        val rows = reader.toList()
        rows[0]["age"] shouldBe "30"
    }

    @Test
    fun `should throw when header contains blank or null`() {
        val csv = """
            name,,city
            Alice,30,Tokyo
        """.trimIndent()

        val reader = CsvReader(StringReader(csv), CsvConfig(), ReaderConfig(hasHeader = true))
        shouldThrow<CsvLineFormatException> {
            reader.header()
        }
    }

    @Test
    fun `should throw when header contains duplicate names`() {
        val csv = """
            name,age,name
            Alice,30,Tokyo
        """.trimIndent()

        val reader = CsvReader(StringReader(csv), CsvConfig(), ReaderConfig(hasHeader = true))
        shouldThrow<CsvLineFormatException> {
            reader.header()
        }
    }

    @Test
    fun `should throw when row column count does not match header`() {
        val csv = """
            name,age
            Alice,30
            Bob
        """.trimIndent()

        val reader = CsvReader(StringReader(csv), CsvConfig(), ReaderConfig(hasHeader = true))
        val iter = reader.iterator()
        iter.next() // OK
        shouldThrow<CsvLineFormatException> {
            iter.next() // mismatch
        }
    }

    @Test
    fun `should throw CsvFormatException on invalid quoted field`() {
        val csv = """
            name,age
            "Alice,30
        """.trimIndent() // missing closing quote

        val reader = CsvReader(StringReader(csv), CsvConfig(), ReaderConfig(hasHeader = true))
        shouldThrow<CsvFormatException> {
            reader.toList()
        }
    }

    @Test
    fun `should parse quoted field containing comma as single cell`() {
        val csv = """
        name,city
        Alice,"Tokyo, Japan"
    """.trimIndent()

        val reader = CsvReader(
            StringReader(csv),
            CsvConfig(delimiter = ','),
            ReaderConfig(hasHeader = true)
        )

        reader.header() shouldBe listOf("name", "city")

        val rows = reader.toList()
        rows[0]["name"] shouldBe "Alice"
        rows[0]["city"] shouldBe "Tokyo, Japan" // カンマを含んでも1セル
    }

    @Test
    fun `should read TSV with header correctly`() {
        val tsv = """
        name\tage\tcity
        Alice\t30\tTokyo
        Bob\t25\tOsaka
    """.trimIndent().replace("\\t", "\t")

        val reader = CsvReader(
            StringReader(tsv),
            CsvConfig(delimiter = '\t'),
            ReaderConfig(hasHeader = true)
        )

        reader.header() shouldBe listOf("name", "age", "city")

        val rows = reader.toList()
        rows[0]["name"] shouldBe "Alice"
        rows[1]["city"] shouldBe "Osaka"
    }

    @Test
    fun `should read TSV without header correctly`() {
        val tsv = """
        Alice\t30\tTokyo
        Bob\t25\tOsaka
    """.trimIndent().replace("\\t", "\t")

        val reader = CsvReader(
            StringReader(tsv),
            CsvConfig(delimiter = '\t'),
            ReaderConfig(hasHeader = false)
        )

        reader.header() shouldBe null

        val rows = reader.toList()
        rows[0][0] shouldBe "Alice"
        rows[1][2] shouldBe "Osaka"
    }

    @Test
    fun `should throw when TSV row column count does not match header`() {
        val tsv = """
        name\tage
        Alice\t30
        Bob
    """.trimIndent().replace("\\t", "\t")

        val reader = CsvReader(
            StringReader(tsv),
            CsvConfig(delimiter = '\t'),
            ReaderConfig(hasHeader = true)
        )

        val iter = reader.iterator()
        iter.next() // OK
        shouldThrow<CsvLineFormatException> {
            iter.next() // mismatch
        }
    }
    
    @Test
    fun `should parse quoted field containing tab as single cell in TSV`() {
        val tsv = """
        name\tcity
        Alice\t"Tokyo\tJapan"
    """.trimIndent().replace("\\t", "\t")

        val reader = CsvReader(
            StringReader(tsv),
            CsvConfig(delimiter = '\t'),
            ReaderConfig(hasHeader = true)
        )

        reader.header() shouldBe listOf("name", "city")

        val rows = reader.toList()
        rows[0]["name"] shouldBe "Alice"
        rows[0]["city"] shouldBe "Tokyo\tJapan" // タブを含んでも1セル
    }

}
