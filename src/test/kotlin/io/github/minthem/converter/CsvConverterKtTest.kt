package io.github.minthem.converter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Locale
import kotlin.reflect.KClass

class CsvConverterKtTest {
    @Test
    fun `convertNumber handles valid Int conversion`() {
        val result = convertNumber(Int::class, "123", Locale.US, "#")
        assertEquals(123, result)
    }

    @Test
    fun `convertNumber handles valid Long conversion`() {
        val result = convertNumber(Long::class, "1234567890123", Locale.US, "#")
        assertEquals(1234567890123L, result)
    }

    @Test
    fun `convertNumber handles valid Short conversion`() {
        val result = convertNumber(Short::class, "32767", Locale.US, "#")
        assertEquals(32767.toShort(), result)
    }

    @Test
    fun `convertNumber handles valid Byte conversion`() {
        val result = convertNumber(Byte::class, "127", Locale.US, "#")
        assertEquals(127.toByte(), result)
    }

    @Test
    fun `convertNumber handles valid Float conversion`() {
        val result = convertNumber(Float::class, "123.45", Locale.US, "#.##")
        assertEquals(123.45f, result, 0.0001f)
    }

    @Test
    fun `convertNumber handles valid Double conversion`() {
        val result = convertNumber(Double::class, "123.456789", Locale.US, "#.######")
        assertEquals(123.456789, result, 0.000001)
    }

    @Test
    fun `convertNumber handles valid BigDecimal conversion`() {
        val result = convertNumber(BigDecimal::class, "1,234,567.89", Locale.US, "#,###.##")
        assertEquals(BigDecimal("1234567.89"), result)
    }

    @Test
    fun `convertNumber throws NumberFormatException for invalid format`() {
        val exception =
            assertThrows(NumberFormatException::class.java) {
                convertNumber(Int::class, "invalid", Locale.US, "#")
            }
        assertEquals("Invalid number: \"invalid\"", exception.message)
    }

    @Test
    fun `convertNumber throws ArithmeticException for value out of range`() {
        val exception =
            assertThrows(ArithmeticException::class.java) {
                convertNumber(Byte::class, "128", Locale.US, "#")
            }
        assertEquals("Value out of range for class kotlin.Byte: \"128\"", exception.message)
    }

    @Test
    fun `convertNumber handles localized numbers in different locales`() {
        val resultUS = convertNumber(Double::class, "1,234.56", Locale.US, "#,###.##")
        assertEquals(1234.56, resultUS, 0.0001)

        val resultDE = convertNumber(Double::class, "1.234,56", Locale.GERMANY, "#,###.##")
        assertEquals(1234.56, resultDE, 0.0001)
    }

    @Test
    fun `convertNumber throws NumberFormatException for empty string`() {
        val exception =
            assertThrows(NumberFormatException::class.java) {
                convertNumber(Int::class, "", Locale.US, "#")
            }
        assertEquals("Invalid number: \"\"", exception.message)
    }

    @Test
    fun `convertNumber throws IllegalArgumentException for unsupported type`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                convertNumber(String::class as KClass<Number>, "123", Locale.US, "#")
            }
        assertEquals("Unsupported number type: class kotlin.String", exception.message)
    }
}
