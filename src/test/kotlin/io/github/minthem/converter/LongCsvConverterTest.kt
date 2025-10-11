package io.github.minthem.converter

import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import java.util.*
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LongCsvConverterTest {

    @Nested
    inner class DeserializeTest {
        @ParameterizedTest
        @NullAndEmptySource
        fun `deserialize should return null for null or blank inputs`(source: String?) {
            val result = LongCsvConverter.deserialize(source, Locale.US, "")
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.LongCsvConverterTest#deserializeProvider")
        fun `deserialize should correctly parse a Long`(
            strValue: String,
            locale: Locale,
            pattern: String,
            expected: Long
        ) {
            val result = LongCsvConverter.deserialize(strValue, locale, pattern)
            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.LongCsvConverterTest#invalidDeserializeProvider")
        fun `deserialize should fail for invalid input`(
            strValue: String,
            locale: Locale,
            pattern: String
        ) {
            val result = LongCsvConverter.deserialize(strValue, locale, pattern)
            assertTrue(result.isFailure)
        }

        @ParameterizedTest
        @ValueSource(strings = ["9223372036854775807", "-9223372036854775808"])
        fun `deserialize should accept Long max and min values`(strValue: String) {
            val result = LongCsvConverter.deserialize(strValue, Locale.US, "#")
            assertTrue(result.isSuccess)
            assertEquals(strValue.toLong(), result.getOrNull())
        }

        @ParameterizedTest
        @ValueSource(strings = ["9223372036854775808", "-9223372036854775809"])
        fun `deserialize should fail for out of range values`(strValue: String) {
            val result = LongCsvConverter.deserialize(strValue, Locale.US, "#")
            assertTrue(result.isFailure)
        }

        @Test
        fun `deserialize should trim whitespaces`() {
            val result = LongCsvConverter.deserialize("   1234567   ", Locale.US, "#")
            assertTrue(result.isSuccess)
            assertEquals(1_234_567L, result.getOrNull())
        }

        @Test
        fun `deserialize should accept non-grouped number even if pattern uses grouping`() {
            val result = LongCsvConverter.deserialize("1234567", Locale.US, "#,###")
            assertTrue(result.isSuccess)
            assertEquals(1_234_567L, result.getOrNull())
        }
    }

    @Nested
    inner class SerializeTest {
        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.LongCsvConverterTest#serializeProvider")
        fun `serialize should return plain string`(
            value: Long,
            locale: Locale,
            pattern: String,
            expected: String
        ) {
            val result = LongCsvConverter.serialize(value, locale, pattern)
            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

        @Test
        fun `serialize should return null when value is null`() {
            val result = LongCsvConverter.serialize(null, Locale.US, "")
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }
    }

    companion object {
        @JvmStatic
        fun deserializeProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("0", Locale.US, "", 0L),
            Arguments.of("123456", Locale.US, "", 123_456L),
            Arguments.of("-987654321", Locale.US, "", -987_654_321L),
            Arguments.of("1,234,567", Locale.US, "#,###", 1_234_567L),
            Arguments.of("12.345.678", Locale.GERMANY, "#,###", 12_345_678L),
            Arguments.of("1234567", Locale.US, "###;(###)", 1_234_567L),
            Arguments.of("(1234567)", Locale.US, "###;(###)", -1_234_567L),
        )

        @JvmStatic
        fun invalidDeserializeProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("abc", Locale.US, "#"),
            Arguments.of("12 345", Locale.US, "#"),
            Arguments.of("1_234", Locale.US, "#"),
            Arguments.of("1234", Locale.US, "invalid.pattern"),
        )

        @JvmStatic
        fun serializeProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(0L, Locale.US, "", "0"),
            Arguments.of(987_654_321L, Locale.US, "", "987654321"),
            Arguments.of(-1_234_567L, Locale.US, "", "-1234567"),
            Arguments.of(1_234_567L, Locale.US, "#,###", "1,234,567"),
            Arguments.of(1_234_567L, Locale.GERMANY, "#,###", "1.234.567"),
            Arguments.of(1_234_567L, Locale.US, "###;(###)", "1234567"),
            Arguments.of(-1_234_567L, Locale.US, "###;(###)", "(1234567)"),
        )
    }
}