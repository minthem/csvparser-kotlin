package io.github.minthem.converter

import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import java.util.Locale
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntCsvConverterTest {
    @Nested
    inner class DeserializeTest {
        @ParameterizedTest
        @NullAndEmptySource
        fun `deserialize should return null for null or blank inputs`(source: String?) {
            val result = IntCsvConverter.deserialize(source, Locale.US, "")
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.IntCsvConverterTest#deserializeProvider")
        fun `deserialize should correctly parse an Int`(
            strValue: String,
            locale: Locale,
            pattern: String,
            expected: Int,
        ) {
            val result = IntCsvConverter.deserialize(strValue, locale, pattern)
            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.IntCsvConverterTest#invalidDeserializeProvider")
        fun `deserialize should fail for invalid input`(
            strValue: String,
            locale: Locale,
            pattern: String,
        ) {
            val result = IntCsvConverter.deserialize(strValue, locale, pattern)
            assertTrue(result.isFailure)
        }

        @ParameterizedTest
        @ValueSource(strings = ["2147483647", "-2147483648"])
        fun `deserialize should accept Int max and min values`(strValue: String) {
            val result = IntCsvConverter.deserialize(strValue, Locale.US, "#")
            assertTrue(result.isSuccess)
            assertEquals(strValue.toInt(), result.getOrNull())
        }

        @ParameterizedTest
        @ValueSource(strings = ["2147483648", "-2147483649"])
        fun `deserialize should fail for out of range values`(strValue: String) {
            val result = IntCsvConverter.deserialize(strValue, Locale.US, "#")
            assertTrue(result.isFailure)
        }

        @Test
        fun `deserialize should trim whitespaces`() {
            val result = IntCsvConverter.deserialize("   456   ", Locale.US, "#")
            assertTrue(result.isSuccess)
            assertEquals(456, result.getOrNull())
        }

        @Test
        fun `deserialize should accept non-grouped number even if pattern uses grouping`() {
            val result = IntCsvConverter.deserialize("12345", Locale.US, "#,###")
            assertTrue(result.isSuccess)
            assertEquals(12345, result.getOrNull())
        }
    }

    @Nested
    inner class SerializeTest {
        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.IntCsvConverterTest#serializeProvider")
        fun `serialize should return plain string`(
            value: Int,
            locale: Locale,
            pattern: String,
            expected: String,
        ) {
            val result = IntCsvConverter.serialize(value, locale, pattern)
            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

        @Test
        fun `serialize should return null when value is null`() {
            val result = IntCsvConverter.serialize(null, Locale.US, "")
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }
    }

    companion object {
        @JvmStatic
        fun deserializeProvider(): Stream<Arguments> =
            Stream.of(
                Arguments.of("0", Locale.US, "", 0),
                Arguments.of("123", Locale.US, "", 123),
                Arguments.of("-456", Locale.US, "", -456),
                Arguments.of("1,234", Locale.US, "#,###", 1234),
                Arguments.of("12.345", Locale.GERMANY, "#,###", 12345),
                Arguments.of("12345", Locale.US, "###;(###)", 12345),
                Arguments.of("(12345)", Locale.US, "###;(###)", -12345),
            )

        @JvmStatic
        fun invalidDeserializeProvider(): Stream<Arguments> =
            Stream.of(
                Arguments.of("abc", Locale.US, "#"),
                Arguments.of("12 345", Locale.US, "#"),
                Arguments.of("1_234", Locale.US, "#"),
                Arguments.of("1234", Locale.US, "invalid.pattern"),
            )

        @JvmStatic
        fun serializeProvider(): Stream<Arguments> =
            Stream.of(
                Arguments.of(0, Locale.US, "", "0"),
                Arguments.of(123, Locale.US, "", "123"),
                Arguments.of(-456, Locale.US, "", "-456"),
                Arguments.of(12345, Locale.US, "", "12345"),
                Arguments.of(12345, Locale.US, "#,###", "12,345"),
                Arguments.of(12345, Locale.GERMANY, "#,###", "12.345"),
                Arguments.of(12345, Locale.US, "###;(###)", "12345"),
                Arguments.of(-12345, Locale.US, "###;(###)", "(12345)"),
            )
    }
}
