package io.github.minthem.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TokenizerTest {

    @Test
    fun `simple comma separated`() {
        val tokenizer = Tokenizer()
        val input = "a,b,c"
        tokenizer.tokenize(input) shouldBe listOf("a", "b", "c")
    }

    @Test
    fun `empty cell becomes null`() {
        val tokenizer = Tokenizer()
        val input = "a,,c"
        tokenizer.tokenize(input) shouldBe listOf("a", null, "c")
    }

    @Test
    fun `trailing empty cell becomes null`() {
        val tokenizer = Tokenizer()
        val input = "a,b,"
        tokenizer.tokenize(input) shouldBe listOf("a", "b", null)
    }

    @Test
    fun `quoted empty cell becomes empty string`() {
        val tokenizer = Tokenizer()
        val input = "a,\"\",c"
        tokenizer.tokenize(input) shouldBe listOf("a", "", "c")
    }

    @Test
    fun `only delimiter yields two nulls`() {
        val tokenizer = Tokenizer()
        val input = ","
        tokenizer.tokenize(input) shouldBe listOf(null, null)
    }

    @Test
    fun `quoted cell with comma`() {
        val tokenizer = Tokenizer()
        val input = "\"a,b\",c"
        tokenizer.tokenize(input) shouldBe listOf("a,b", "c")
    }

    @Test
    fun `quoted cell with newline`() {
        val tokenizer = Tokenizer()
        val input = "\"a\nb\",c"
        tokenizer.tokenize(input) shouldBe listOf("a\nb", "c")
    }

    @Test
    fun `escaped quote inside cell`() {
        val tokenizer = Tokenizer()
        val input = "\"He said \"\"Hello\"\"\",x"
        tokenizer.tokenize(input) shouldBe listOf("He said \"Hello\"", "x")
    }

    @Test
    fun `single quote as quoteChar`() {
        val tokenizer = Tokenizer(delimiter = ',', quote = '\'')
        val input = "'a,b',c"
        tokenizer.tokenize(input) shouldBe listOf("a,b", "c")
    }

    @Test
    fun `different delimiter`() {
        val tokenizer = Tokenizer(delimiter = ';')
        val input = "a;b;c"
        tokenizer.tokenize(input) shouldBe listOf("a", "b", "c")
    }

    @Test
    fun `long cell`() {
        val tokenizer = Tokenizer()
        val input = "a,${"x".repeat(1000)},b"
        tokenizer.tokenize(input) shouldBe listOf("a", "x".repeat(1000), "b")
    }

    @Test
    fun `unclosed quote should throw`() {
        val tokenizer = Tokenizer()
        val input = "\"abc,def"
        shouldThrow<CsvFormatInternalException> {
            tokenizer.tokenize(input)
        }
    }

    @Test
    fun `quote starts in middle of cell should throw`() {
        val tokenizer = Tokenizer()
        val input = "abc\"def\",ghi"
        shouldThrow<CsvFormatInternalException> {
            tokenizer.tokenize(input)
        }
    }

    @Test
    fun `unescaped quote inside quoted cell should throw`() {
        val tokenizer = Tokenizer()
        val input = "\"abc \" def\""
        shouldThrow<CsvFormatInternalException> {
            tokenizer.tokenize(input)
        }
    }

    @Test
    fun `newline inside unclosed quote should throw`() {
        val tokenizer = Tokenizer()
        val input = "\"abc\ndef"
        shouldThrow<CsvFormatInternalException> {
            tokenizer.tokenize(input)
        }
    }
}
