package io.github.minthem.converter

import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import java.math.BigDecimal
import java.util.*
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the `BigDecimalCsvConverter` class.
 */
class BigDecimalCsvConverterTest {

    @Nested
    inner class DeserializeTest {
        @ParameterizedTest
        @NullAndEmptySource
        fun `deserialize should return null for null or blank inputs`(source: String?) {
            val result1 = BigDecimalCsvConverter.deserialize(source, Locale.US, "")
            assertTrue(result1.isSuccess)
            assertNull(result1.getOrNull())
        }

        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.BigDecimalCsvConverterTest#deserializeProvider")
        fun `deserialize should correctly parse a BigDecimal`(
            strValue: String,
            locale: Locale,
            pattern: String,
            expected: BigDecimal
        ) {
            val result = BigDecimalCsvConverter.deserialize(strValue, locale, pattern)
            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.BigDecimalCsvConverterTest#invalidDeserializeProvider")
        fun `deserialize should throw error for invalid input`(
            strValue: String,
            locale: Locale,
            pattern: String
        ) {
            val result = BigDecimalCsvConverter.deserialize(strValue, locale, pattern)
            assertTrue(result.isFailure)
        }

        @Test
        fun `deserialize should fail with incorrect locale specific format`() {
            val result = BigDecimalCsvConverter.deserialize("12,345.67", Locale.GERMANY, "#,##0.##")
            assertTrue(result.isFailure)
        }

        @Test
        fun `deserialize should fail when separators don't match locale`() {
            val result = BigDecimalCsvConverter.deserialize("12.345,67", Locale.US, "#,##0.##")
            assertTrue(result.isFailure)
        }

        @Test
        fun `deserialize should trim whitespaces`() {
            val result = BigDecimalCsvConverter.deserialize("   12,345.678   ", Locale.US, "#,##0.###")
            assertTrue(result.isSuccess)
            assertEquals(BigDecimal("12345.678"), result.getOrNull())
        }

        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.BigDecimalCsvConverterTest#roundingEdgeProvider")
        fun `deserialize should round HALF_UP with pattern scale`(
            strValue: String,
            pattern: String,
            expected: BigDecimal
        ) {
            val result = BigDecimalCsvConverter.deserialize(strValue, Locale.US, pattern)
            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

        @Test
        fun `deserialize should accept non-grouped number even if pattern uses grouping`() {
            val result = BigDecimalCsvConverter.deserialize("12345.678", Locale.US, "#,##0.###")
            assertTrue(result.isSuccess)
            assertEquals(BigDecimal("12345.678"), result.getOrNull())
        }
    }

    @Nested
    inner class SerializeTest {
        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.BigDecimalCsvConverterTest#serializeProvider")
        fun `serialize should return plain string `(
            bdValue: BigDecimal,
            locale: Locale,
            pattern: String,
            expected: String
        ) {
            val result = BigDecimalCsvConverter.serialize(bdValue, locale, pattern)
            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

        @Test
        fun `serialize should format BigDecimal with a pattern`() {
            val result = BigDecimalCsvConverter.serialize(BigDecimal("12345.678"), Locale.US, "#,##0.00")
            assertTrue(result.isSuccess)
            assertEquals("12,345.68", result.getOrNull())
        }

        @Test
        fun `serialize should return null when value is null`() {
            val result = BigDecimalCsvConverter.serialize(null, Locale.US, "")
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }
    }

    companion object {
        @JvmStatic
        fun deserializeProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("12345.678", Locale.US, "", BigDecimal("12345.678")),
            Arguments.of("12,345.678", Locale.US, "#,##0.###", BigDecimal("12345.678")),
            Arguments.of("$ 12,345.678", Locale.US, "'$' #,##0.###", BigDecimal("12345.678")),
            Arguments.of("12.345,678", Locale.GERMANY, "#,##0.###", BigDecimal("12345.678")),
        )

        @JvmStatic
        fun invalidDeserializeProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("invalid", Locale.US, ""),
            Arguments.of("12345.67abc", Locale.US, ""),
            Arguments.of("abc12345.67", Locale.US, ""),
            Arguments.of("12 345.67", Locale.US, ""),
            Arguments.of("1_234.56", Locale.US, ""),
            Arguments.of("1234", Locale.US, "invalid.pattern"),

            )

        @JvmStatic
        fun roundingEdgeProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("0.005", "#.00", BigDecimal("0.01")),
            Arguments.of("9.995", "#.00", BigDecimal("10.00")),
            Arguments.of("1.2345", "#.000", BigDecimal("1.235"))
        )

        @JvmStatic
        fun serializeProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(BigDecimal("12345.678"), Locale.US, "", "12345.678"),
            Arguments.of(BigDecimal("12345.678"), Locale.US, "#,##0.###", "12,345.678"),
            Arguments.of(BigDecimal("12345.678"), Locale.US, "'$' #,##0.###", "$ 12,345.678"),
            Arguments.of(BigDecimal("12345.678"), Locale.GERMANY, "#,##0.###", "12.345,678"),
            Arguments.of(BigDecimal("1E+3"), Locale.US, "", "1000"),
            Arguments.of(BigDecimal("-1234.5"), Locale.US, "#,##0.00;(#,##0.00)", "(1,234.50)"),
        )
    }
}