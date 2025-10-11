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

class ShortCsvConverterTest {

    @Nested
    inner class DeserializeTest {
        @ParameterizedTest
        @NullAndEmptySource
        fun `deserialize should return null for null or blank inputs`(source: String?) {
            val result = ShortCsvConverter.deserialize(source, Locale.US, "")
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.ShortCsvConverterTest#deserializeProvider")
        fun `deserialize should correctly parse a Short`(
            strValue: String,
            locale: Locale,
            pattern: String,
            expected: Short
        ) {
            val result = ShortCsvConverter.deserialize(strValue, locale, pattern)
            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.ShortCsvConverterTest#invalidDeserializeProvider")
        fun `deserialize should fail for invalid input`(
            strValue: String,
            locale: Locale,
            pattern: String
        ) {
            val result = ShortCsvConverter.deserialize(strValue, locale, pattern)
            assertTrue(result.isFailure)
        }

        @ParameterizedTest
        @ValueSource(strings = ["32767", "-32768"])
        fun `deserialize should accept Short max and min values`(strValue: String) {
            val result = ShortCsvConverter.deserialize(strValue, Locale.US, "#")
            assertTrue(result.isSuccess)
            assertEquals(strValue.toShort(), result.getOrNull())
        }

        @ParameterizedTest
        @ValueSource(strings = ["32768", "-32769"])
        fun `deserialize should fail for out of range values`(strValue: String) {
            val result = ShortCsvConverter.deserialize(strValue, Locale.US, "#")
            assertTrue(result.isFailure)
        }

        @Test
        fun `deserialize should trim whitespaces`() {
            val result = ShortCsvConverter.deserialize("   123   ", Locale.US, "#")
            assertTrue(result.isSuccess)
            assertEquals(123.toShort(), result.getOrNull())
        }

        @Test
        fun `deserialize should accept non-grouped number even if pattern uses grouping`() {
            val result = ShortCsvConverter.deserialize("1234", Locale.US, "#,###")
            assertTrue(result.isSuccess)
            assertEquals(1234.toShort(), result.getOrNull())
        }
    }

    @Nested
    inner class SerializeTest {
        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.ShortCsvConverterTest#serializeProvider")
        fun `serialize should return plain string`(
            value: Short,
            locale: Locale,
            pattern: String,
            expected: String
        ) {
            val result = ShortCsvConverter.serialize(value, locale, pattern)
            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

        @Test
        fun `serialize should return null when value is null`() {
            val result = ShortCsvConverter.serialize(null, Locale.US, "")
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }
    }

    companion object {
        @JvmStatic
        fun deserializeProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("0", Locale.US, "", 0.toShort()),
            Arguments.of("123", Locale.US, "", 123.toShort()),
            Arguments.of("-123", Locale.US, "", (-123).toShort()),
            Arguments.of("1,234", Locale.US, "#,###", 1234.toShort()),
            Arguments.of("12.345", Locale.GERMANY, "#,###", 12345.toShort()),
            Arguments.of("1234", Locale.US, "###;(###)", 1234.toShort()),
            Arguments.of("(1234)", Locale.US, "###;(###)", (-1234).toShort()),
        )

        @JvmStatic
        fun invalidDeserializeProvider(): Stream<Arguments> = Stream.of(
            Arguments.of("abc", Locale.US, "#"),
            Arguments.of("12 34", Locale.US, "#"),
            Arguments.of("1_234", Locale.US, "#"),
            Arguments.of("1234", Locale.US, "invalid.pattern"),
        )

        @JvmStatic
        fun serializeProvider(): Stream<Arguments> = Stream.of(
            Arguments.of(0.toShort(), Locale.US, "", "0"),
            Arguments.of(123.toShort(), Locale.US, "", "123"),
            Arguments.of((-123).toShort(), Locale.US, "", "-123"),
            Arguments.of(1234.toShort(), Locale.US, "#,###", "1,234"),
            Arguments.of(1234.toShort(), Locale.GERMANY, "#,###", "1.234"),
            Arguments.of(1234.toShort(), Locale.US, "###;(###)", "1234"),
            Arguments.of((-1234).toShort(), Locale.US, "###;(###)", "(1234)"),
        )
    }
}