package io.github.minthem.core

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TokenizerTest {

    @Test
    fun testSimpleCommaSeparated() {
        val tokenizer = Tokenizer()
        val input = "a,b,c"
        val actual = tokenizer.tokenize(input)
        assertEquals(listOf("a", "b", "c"), actual)
    }

    @Test
    fun testEmptyCellBecomesNull() {
        val tokenizer = Tokenizer()
        val input = "a,,c"
        val actual = tokenizer.tokenize(input)
        assertEquals(listOf("a", null, "c"), actual)
    }

    @Test
    fun testTrailingEmptyCellBecomesNull() {
        val tokenizer = Tokenizer()
        val input = "a,b,"
        val actual = tokenizer.tokenize(input)
        assertEquals(listOf("a", "b", null), actual)
    }

    @Test
    fun testQuotedEmptyCellBecomesEmptyString() {
        val tokenizer = Tokenizer()
        val input = "a,\"\",c"
        val actual = tokenizer.tokenize(input)
        assertEquals(listOf("a", "", "c"), actual)
    }

    @Test
    fun testOnlyDelimiter() {
        val tokenizer = Tokenizer()
        val input = ","
        val actual = tokenizer.tokenize(input)
        assertEquals(listOf(null, null), actual)
    }

    @Test
    fun testQuotedCellWithComma() {
        val tokenizer = Tokenizer()
        val input = "\"a,b\",c"
        val actual = tokenizer.tokenize(input)
        assertEquals(listOf("a,b", "c"), actual)
    }

    @Test
    fun testQuotedCellWithNewline() {
        val tokenizer = Tokenizer()
        val input = "\"a\nb\",c"
        val actual = tokenizer.tokenize(input)
        assertEquals(listOf("a\nb", "c"), actual)
    }

    @Test
    fun testEscapedQuoteInsideCell() {
        val tokenizer = Tokenizer()
        val input = "\"He said \"\"Hello\"\"\",x"
        val actual = tokenizer.tokenize(input)
        assertEquals(listOf("He said \"Hello\"", "x"), actual)
    }

    @Test
    fun testSingleQuoteAsQuoteChar() {
        val tokenizer = Tokenizer(delimiter = ',', quote = '\'')
        val input = "'a,b',c"
        val actual = tokenizer.tokenize(input)
        assertEquals(listOf("a,b", "c"), actual)
    }

    @Test
    fun testDifferentDelimiter() {
        val tokenizer = Tokenizer(delimiter = ';')
        val input = "a;b;c"
        val actual = tokenizer.tokenize(input)
        assertEquals(listOf("a", "b", "c"), actual)
    }

    @Test
    fun testLongCell() {
        val tokenizer = Tokenizer()
        val input = "a,${"x".repeat(1000)},b"
        val actual = tokenizer.tokenize(input)
        assertEquals(listOf("a", "x".repeat(1000), "b"), actual)
    }

    @Test
    fun testUnclosedQuote() {
        val tokenizer = Tokenizer()
        val input = "\"abc,def" // クオートが閉じられていない
        assertFailsWith<IllegalArgumentException> {
            tokenizer.tokenize(input)
        }
    }

    @Test
    fun testQuoteStartsInMiddleOfCell() {
        val tokenizer = Tokenizer()
        val input = "abc\"def\",ghi" // セル途中でクオート開始
        assertFailsWith<IllegalArgumentException> {
            tokenizer.tokenize(input)
        }
    }

    @Test
    fun testUnescapedQuoteInsideQuotedCell() {
        val tokenizer = Tokenizer()
        val input = "\"abc \" def\"" // クオート内に単独の `"`
        assertFailsWith<IllegalArgumentException> {
            tokenizer.tokenize(input)
        }
    }

    @Test
    fun testNewlineInsideUnclosedQuote() {
        val tokenizer = Tokenizer()
        val input = "\"abc\ndef" // 改行を含むが閉じクオートなし
        assertFailsWith<IllegalArgumentException> {
            tokenizer.tokenize(input)
        }
    }
}
