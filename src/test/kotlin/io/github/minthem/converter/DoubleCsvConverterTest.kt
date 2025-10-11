package io.github.minthem.converter

import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import java.util.Locale
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DoubleCsvConverterTest {
    @Nested
    inner class DeserializeTest {
        @ParameterizedTest
        @NullAndEmptySource
        fun `deserialize should return null for null or blank inputs`(source: String?) {
            val result = DoubleCsvConverter.deserialize(source, Locale.US, "")
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.DoubleCsvConverterTest#deserializeProvider")
        fun `deserialize should correctly parse a Double`(
            strValue: String,
            locale: Locale,
            pattern: String,
            expected: Double,
        ) {
            val result = DoubleCsvConverter.deserialize(strValue, locale, pattern)
            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.DoubleCsvConverterTest#invalidDeserializeProvider")
        fun `deserialize should fail for invalid input`(
            strValue: String,
            locale: Locale,
            pattern: String,
        ) {
            val result = DoubleCsvConverter.deserialize(strValue, locale, pattern)
            assertTrue(result.isFailure)
        }

        @Test
        fun `deserialize should trim whitespaces`() {
            val result = DoubleCsvConverter.deserialize("   1234.56   ", Locale.US, "#.##")
            assertTrue(result.isSuccess)
            assertEquals(1234.56, result.getOrNull())
        }

        @Test
        fun `deserialize should accept non-grouped number even if pattern uses grouping`() {
            val result = DoubleCsvConverter.deserialize("1234.56", Locale.US, "#,##0.##")
            assertTrue(result.isSuccess)
            assertEquals(1234.56, result.getOrNull())
        }
    }

    @Nested
    inner class SerializeTest {
        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.DoubleCsvConverterTest#serializeProvider")
        fun `serialize should return formatted string`(
            value: Double,
            locale: Locale,
            pattern: String,
            expected: String,
        ) {
            val result = DoubleCsvConverter.serialize(value, locale, pattern)
            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

        @Test
        fun `serialize should return null when value is null`() {
            val result = DoubleCsvConverter.serialize(null, Locale.US, "")
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }
    }

    companion object {
        @JvmStatic
        fun deserializeProvider(): Stream<Arguments> =
            Stream.of(
                Arguments.of("1234.56", Locale.US, "", 1234.56),
                Arguments.of("1,234.56", Locale.US, "#,##0.##", 1234.56),
                Arguments.of("1.234,56", Locale.GERMANY, "#,##0.##", 1234.56),
                Arguments.of("1234.5678", Locale.US, "#.##", 1234.57),
            )

        @JvmStatic
        fun invalidDeserializeProvider(): Stream<Arguments> =
            Stream.of(
                Arguments.of("abcd", Locale.US, "#.##"),
                Arguments.of("12 34.56", Locale.US, "#.##"),
                Arguments.of("1_234.56", Locale.US, "#.##"),
                Arguments.of("1234.56", Locale.US, "invalid.pattern"),
            )

        @JvmStatic
        fun serializeProvider(): Stream<Arguments> =
            Stream.of(
                Arguments.of(1234.56, Locale.US, "", "1234.56"),
                Arguments.of(1234.56, Locale.US, "#,##0.##", "1,234.56"),
                Arguments.of(1234.56, Locale.GERMANY, "#,##0.##", "1.234,56"),
                Arguments.of(1234.5678, Locale.US, "#.##", "1234.57"),
            )
    }
}
