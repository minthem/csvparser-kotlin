package io.github.minthem.core

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.StringReader

class LineExtractorTest {
    private fun extractAll(
        input: String,
        quote: Char = '"',
        buffer: Int = 1024,
    ): List<String> {
        val reader = LineExtractor(StringReader(input), quote = quote, lineBufferSize = buffer)
        return generateSequence { reader.getLine() }.toList()
    }

    @Test
    fun `empty line should return empty string`() {
        extractAll("\r\n") shouldBe listOf("")
    }

    @Test
    fun `single line with CRLF`() {
        val input = "123,456,789"
        extractAll("$input\r\n") shouldBe listOf(input)
    }

    @Test
    fun `two lines with CRLF`() {
        val input = listOf("123,456,789", "abc,def,ghi")
        extractAll(input.joinToString("\r\n")) shouldBe input
    }

    @Test
    fun `quoted field should be preserved`() {
        val input = listOf("123,456,789", "jkm,\"nop,qrs\",tuv", "abc,def,ghi")
        extractAll(input.joinToString("\r\n")) shouldBe input
    }

    @Test
    fun `escaped quotes inside quoted field`() {
        val input = listOf("123,456,789", "jkm,\"\"nop,qrs\"\",tuv", "abc,def,ghi")
        extractAll(input.joinToString("\r\n")) shouldBe input
    }

    @Test
    fun `single quote as quoteChar`() {
        val input = listOf("123,456,789", "jkm,'nop,qrs',tuv", "abc,def,ghi")
        extractAll(input.joinToString("\r\n"), quote = '\'') shouldBe input
    }

    @Test
    fun `LF as separator`() {
        val input = listOf("123,456,789", "abc,def,ghi")
        extractAll(input.joinToString("\n")) shouldBe input
    }

    @Test
    fun `CR as separator`() {
        val input = listOf("123,456,789", "abc,def,ghi")
        extractAll(input.joinToString("\r")) shouldBe input
    }

    @Test
    fun `CRLF split across buffer reads`() {
        val input = listOf("abc", "xyz")
        extractAll(input.joinToString("\r\n"), buffer = 4) shouldBe input
    }

    @Test
    fun `newline inside quoted field`() {
        val input = listOf("123,\"abc\ndef\",456", "zzz,yyy,xxx")
        extractAll(input.joinToString("\r\n")) shouldBe input
    }

    @Test
    fun `input ending with CR`() {
        val input = listOf("abc")
        extractAll(input.joinToString("\r")) shouldBe input
    }

    @Test
    fun `input ending with CRLF`() {
        val input = listOf("abc")
        extractAll(input.joinToString("\r\n")) shouldBe input
    }

    @Test
    fun `empty line between lines`() {
        val input = listOf("abc", "", "xyz")
        extractAll(input.joinToString("\r\n")) shouldBe input
    }

    @Test
    fun `long line exceeding buffer size`() {
        val input = listOf("a".repeat(2000))
        extractAll(input.joinToString("\r\n"), buffer = 128) shouldBe input
    }
}
