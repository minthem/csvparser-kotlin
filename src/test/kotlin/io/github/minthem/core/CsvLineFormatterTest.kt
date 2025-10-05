package io.github.minthem.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CsvLineFormatterTest {
    @Test
    fun `simple comma separated`() {
        val csvLineFormatter = CsvLineFormatter()
        val input = "a,b,c"
        csvLineFormatter.split(input) shouldBe listOf("a", "b", "c")
    }

    @Test
    fun `empty cell becomes null`() {
        val csvLineFormatter = CsvLineFormatter()
        val input = "a,,c"
        csvLineFormatter.split(input) shouldBe listOf("a", null, "c")
    }

    @Test
    fun `trailing empty cell becomes null`() {
        val csvLineFormatter = CsvLineFormatter()
        val input = "a,b,"
        csvLineFormatter.split(input) shouldBe listOf("a", "b", null)
    }

    @Test
    fun `quoted empty cell becomes empty string`() {
        val csvLineFormatter = CsvLineFormatter()
        val input = "a,\"\",c"
        csvLineFormatter.split(input) shouldBe listOf("a", "", "c")
    }

    @Test
    fun `only delimiter yields two nulls`() {
        val csvLineFormatter = CsvLineFormatter()
        val input = ","
        csvLineFormatter.split(input) shouldBe listOf(null, null)
    }

    @Test
    fun `quoted cell with comma`() {
        val csvLineFormatter = CsvLineFormatter()
        val input = "\"a,b\",c"
        csvLineFormatter.split(input) shouldBe listOf("a,b", "c")
    }

    @Test
    fun `quoted cell with newline`() {
        val csvLineFormatter = CsvLineFormatter()
        val input = "\"a\nb\",c"
        csvLineFormatter.split(input) shouldBe listOf("a\nb", "c")
    }

    @Test
    fun `escaped quote inside cell`() {
        val csvLineFormatter = CsvLineFormatter()
        val input = "\"He said \"\"Hello\"\"\",x"
        csvLineFormatter.split(input) shouldBe listOf("He said \"Hello\"", "x")
    }

    @Test
    fun `single quote as quoteChar`() {
        val csvLineFormatter = CsvLineFormatter(delimiter = ',', quote = '\'')
        val input = "'a,b',c"
        csvLineFormatter.split(input) shouldBe listOf("a,b", "c")
    }

    @Test
    fun `different delimiter`() {
        val csvLineFormatter = CsvLineFormatter(delimiter = ';')
        val input = "a;b;c"
        csvLineFormatter.split(input) shouldBe listOf("a", "b", "c")
    }

    @Test
    fun `long cell`() {
        val csvLineFormatter = CsvLineFormatter()
        val input = "a,${"x".repeat(1000)},b"
        csvLineFormatter.split(input) shouldBe listOf("a", "x".repeat(1000), "b")
    }

    @Test
    fun `unclosed quote should throw`() {
        val csvLineFormatter = CsvLineFormatter()
        val input = "\"abc,def"
        shouldThrow<CsvFormatInternalException> {
            csvLineFormatter.split(input)
        }
    }

    @Test
    fun `quote starts in middle of cell should throw`() {
        val csvLineFormatter = CsvLineFormatter()
        val input = "abc\"def\",ghi"
        shouldThrow<CsvFormatInternalException> {
            csvLineFormatter.split(input)
        }
    }

    @Test
    fun `unescaped quote inside quoted cell should throw`() {
        val csvLineFormatter = CsvLineFormatter()
        val input = "\"abc \" def\""
        shouldThrow<CsvFormatInternalException> {
            csvLineFormatter.split(input)
        }
    }

    @Test
    fun `newline inside unclosed quote should throw`() {
        val csvLineFormatter = CsvLineFormatter()
        val input = "\"abc\ndef"
        shouldThrow<CsvFormatInternalException> {
            csvLineFormatter.split(input)
        }
    }

    @Test
    fun `simple join with non-null values`() {
        val csvLineFormatter = CsvLineFormatter()
        val input = listOf("a", "b", "c")
        csvLineFormatter.join(input) shouldBe "a,b,c"
    }

    @Test
    fun `join with null values`() {
        val csvLineFormatter = CsvLineFormatter()
        val input = listOf("a", null, "c")
        csvLineFormatter.join(input, nullValue = "") shouldBe "a,,c"
    }

    @Test
    fun `join with alternate delimiter`() {
        val csvLineFormatter = CsvLineFormatter(delimiter = ';')
        val input = listOf("a", "b", "c")
        csvLineFormatter.join(input) shouldBe "a;b;c"
    }

    @Test
    fun `join with quoted values`() {
        val csvLineFormatter = CsvLineFormatter()
        val input = listOf("a", "b,c", "d")
        csvLineFormatter.join(input) shouldBe "a,\"b,c\",d"
    }

    @Test
    fun `join with special characters`() {
        val csvLineFormatter = CsvLineFormatter()
        val input = listOf("a", "b\nc", "d\"e\"f")
        csvLineFormatter.join(input) shouldBe "a,\"b\nc\",\"d\"\"e\"\"f\""
    }
}
