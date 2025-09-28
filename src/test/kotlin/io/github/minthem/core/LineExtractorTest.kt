package io.github.minthem.core

import org.junit.jupiter.api.Test
import java.io.StringReader
import kotlin.test.assertEquals

class LineExtractorTest {

    @Test
    fun testGetLine_emptyLine() {
        val reader = LineExtractor(StringReader("\r\n"))
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(listOf(""), actual)
    }

    @Test
    fun testGetLine_singleLine_CRLF() {
        val input = listOf("123,456,789")
        val reader = LineExtractor(StringReader(input.joinToString("\r\n")))
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(input, actual)
    }

    @Test
    fun testGetLine_twoLines_CRLF() {
        val input = listOf("123,456,789", "abc,def,ghi")
        val reader = LineExtractor(StringReader(input.joinToString("\r\n")))
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(input, actual)
    }

    @Test
    fun testGetLine_lineWithQuotes_CRLF() {
        val input = listOf("123,456,789", "jkm,\"nop,qrs\",tuv", "abc,def,ghi")
        val reader = LineExtractor(StringReader(input.joinToString("\r\n")))
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(input, actual)
    }

    @Test
    fun testGetLine_lineWithEscapedQuotes_CRLF() {
        val input = listOf("123,456,789", "jkm,\"\"nop,qrs\"\",tuv", "abc,def,ghi")
        val reader = LineExtractor(StringReader(input.joinToString("\r\n")))
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(input, actual)
    }

    @Test
    fun testGetLine_lineWithSingleQuotes_CRLF() {
        val input = listOf("123,456,789", "jkm,'nop,qrs',tuv", "abc,def,ghi")
        val reader = LineExtractor(StringReader(input.joinToString("\r\n")), quote = '\'')
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(input, actual)
    }

    @Test
    fun testGetLine_linesSeparatedByLF() {
        val input = listOf("123,456,789", "abc,def,ghi")
        val reader = LineExtractor(StringReader(input.joinToString("\n")))
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(input, actual)
    }

    @Test
    fun testGetLine_linesSeparatedByCR() {
        val input = listOf("123,456,789", "abc,def,ghi")
        val reader = LineExtractor(StringReader(input.joinToString("\r")))
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(input, actual)
    }

    @Test
    fun testGetLine_CRLF_SplitAcrossReads() {
        val input = listOf("abc", "xyz")
        val reader = LineExtractor(StringReader(input.joinToString("\r\n")), lineBufferSize = 4)
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(input, actual)
    }

    @Test
    fun testGetLine_newlineInsideQuotes() {
        val input = listOf("123,\"abc\ndef\",456", "zzz,yyy,xxx")
        val reader = LineExtractor(StringReader(input.joinToString("\r\n")))
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(input, actual)
    }

    @Test
    fun testGetLine_endWithCR() {
        val input = listOf("abc")
        val reader = LineExtractor(StringReader(input.joinToString("\r")))
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(input, actual)
    }

    @Test
    fun testGetLine_endWithCRLF() {
        val input = listOf("abc")
        val reader = LineExtractor(StringReader(input.joinToString("\r\n")))
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(input, actual)
    }

    @Test
    fun testGetLine_emptyLineBetween() {
        val input = listOf("abc", "", "xyz")
        val reader = LineExtractor(StringReader(input.joinToString("\r\n")))
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(input, actual)
    }

    @Test
    fun testGetLine_longLineExceedingBuffer() {
        val input = listOf("a".repeat(2000))
        val reader = LineExtractor(StringReader(input.joinToString("\r\n")), lineBufferSize = 128)
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(input, actual)
    }
}
