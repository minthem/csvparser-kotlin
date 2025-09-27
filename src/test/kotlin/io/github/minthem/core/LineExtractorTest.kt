package io.github.minthem.core

import org.junit.jupiter.api.Test
import java.io.StringReader
import kotlin.test.assertEquals

class LineExtractorTest {

    @Test
    fun testGetLine_emptyLine() {
        val expected = listOf("")
        val strReader = StringReader("\r\n")
        val reader = LineExtractor(strReader)

        val actual = generateSequence { reader.getLine() }.toList()

        assertEquals(expected, actual)
    }

    @Test
    fun testGetLine_singleLine_CRLF() {
        val expected = listOf("123,456,789")
        val strReader = StringReader("123,456,789\r\n")
        val reader = LineExtractor(strReader)

        val actual = generateSequence { reader.getLine() }.toList()

        assertEquals(expected, actual)
    }

    @Test
    fun testGetLine_twoLines_CRLF() {
        val expected = listOf("123,456,789", "abc,def,ghi")
        val strReader = StringReader("123,456,789\r\nabc,def,ghi\r\n")
        val reader = LineExtractor(strReader)

        val actual = generateSequence { reader.getLine() }.toList()

        assertEquals(expected, actual)
    }

    @Test
    fun testGetLine_lineWithQuotes_CRLF() {
        val expected = listOf("123,456,789", "jkm,\"nop,qrs\",tuv", "abc,def,ghi")
        val strReader = StringReader(expected.joinToString("\r\n"))
        val reader = LineExtractor(strReader)

        val actual = generateSequence { reader.getLine() }.toList()

        assertEquals(expected, actual)
    }

    @Test
    fun testGetLine_lineWithEscapedQuotes_CRLF() {
        val expected = listOf("123,456,789", "jkm,\"\"nop,qrs\"\",tuv", "abc,def,ghi")
        val strReader = StringReader(expected.joinToString("\r\n"))
        val reader = LineExtractor(strReader)

        val actual = generateSequence { reader.getLine() }.toList()

        assertEquals(expected, actual)
    }

    @Test
    fun testGetLine_lineWithSingleQuotes_CRLF() {
        val expected = listOf("123,456,789", "jkm,'nop,qrs',tuv", "abc,def,ghi")
        val strReader = StringReader(expected.joinToString("\r\n"))
        val reader = LineExtractor(strReader, quote = '\'')

        val actual = generateSequence { reader.getLine() }.toList()

        assertEquals(expected, actual)
    }

    @Test
    fun testGetLine_linesSeparatedByLF() {
        val expected = listOf("123,456,789", "abc,def,ghi")
        val strReader = StringReader("123,456,789\nabc,def,ghi\n")
        val reader = LineExtractor(strReader)

        val actual = generateSequence { reader.getLine() }.toList()

        assertEquals(expected, actual)
    }

    @Test
    fun testGetLine_linesSeparatedByCR() {
        val expected = listOf("123,456,789", "abc,def,ghi")
        val strReader = StringReader("123,456,789\rabc,def,ghi\r")
        val reader = LineExtractor(strReader)

        val actual = generateSequence { reader.getLine() }.toList()

        assertEquals(expected, actual)
    }

    @Test
    fun testGetLine_CRLF_SplitAcrossReads() {
        val input = "abc\r\nxyz"
        val reader = LineExtractor(StringReader(input), bufferSize = 4)
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(listOf("abc", "xyz"), actual)
    }

    @Test
    fun testGetLine_newlineInsideQuotes() {
        val input = "123,\"abc\ndef\",456\r\nzzz,yyy,xxx"
        val reader = LineExtractor(StringReader(input))
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(listOf("123,\"abc\ndef\",456", "zzz,yyy,xxx"), actual)
    }

    @Test
    fun testGetLine_endWithCR() {
        val input = "abc\r"
        val reader = LineExtractor(StringReader(input))
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(listOf("abc"), actual)
    }

    @Test
    fun testGetLine_endWithCRLF() {
        val input = "abc\r\n"
        val reader = LineExtractor(StringReader(input))
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(listOf("abc"), actual)
    }

    @Test
    fun testGetLine_emptyLineBetween() {
        val input = "abc\r\n\r\nxyz"
        val reader = LineExtractor(StringReader(input))
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(listOf("abc", "", "xyz"), actual)
    }

    @Test
    fun testGetLine_longLineExceedingBuffer() {
        val longLine = "a".repeat(2000)
        val reader = LineExtractor(StringReader(longLine), bufferSize = 128)
        val actual = generateSequence { reader.getLine() }.toList()
        assertEquals(listOf(longLine), actual)
    }
}
