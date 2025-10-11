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

class ByteCsvConverterTest {
    @Nested
    inner class DeserializeTest {
        @ParameterizedTest
        @NullAndEmptySource
        fun `deserialize should return null for null or blank inputs`(source: String?) {
            val result = ByteCsvConverter.deserialize(source, Locale.US, "")
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.ByteCsvConverterTest#deserializeProvider")
        fun `deserialize should correctly parse a Byte`(
            strValue: String,
            locale: Locale,
            pattern: String,
            expected: Byte,
        ) {
            val result = ByteCsvConverter.deserialize(strValue, locale, pattern)
            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.ByteCsvConverterTest#invalidDeserializeProvider")
        fun `deserialize should fail for invalid input`(
            strValue: String,
            locale: Locale,
            pattern: String,
        ) {
            val result = ByteCsvConverter.deserialize(strValue, locale, pattern)
            assertTrue(result.isFailure)
        }

        @ParameterizedTest
        @ValueSource(strings = ["128", "-129"])
        fun `deserialize should fail for out of range values`(strValue: String) {
            val result = ByteCsvConverter.deserialize(strValue, Locale.US, "#")
            assertTrue(result.isFailure)
        }

        @Test
        fun `deserialize should trim whitespaces`() {
            val result = ByteCsvConverter.deserialize("   127   ", Locale.US, "#")
            assertTrue(result.isSuccess)
            assertEquals(127.toByte(), result.getOrNull())
        }

        @Test
        fun `deserialize should accept non-grouped number even if pattern uses grouping`() {
            val result = ByteCsvConverter.deserialize("127", Locale.US, "#,##")
            assertTrue(result.isSuccess)
            assertEquals(127.toByte(), result.getOrNull())
        }
    }

    @Nested
    inner class SerializeTest {
        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.ByteCsvConverterTest#serializeProvider")
        fun `serialize should return plain string`() {
            val result = ByteCsvConverter.serialize(127, Locale.US, "")
            assertTrue(result.isSuccess)
            assertEquals("127", result.getOrNull())
        }

        @Test
        fun `serialize should return null when value is null`() {
            val result = ByteCsvConverter.serialize(null, Locale.US, "")
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }
    }

    companion object {
        @JvmStatic
        fun deserializeProvider(): Stream<Arguments> =
            Stream.of(
                Arguments.of("1", Locale.US, "", 1.toByte()),
                Arguments.of("-1", Locale.US, "", (-1).toByte()),
                Arguments.of("127", Locale.US, "", 127.toByte()),
                Arguments.of("-128", Locale.US, "", (-128).toByte()),
                Arguments.of("1,27", Locale.US, "#,##", 127.toByte()),
                Arguments.of("1.27", Locale.GERMANY, "#,##", 127.toByte()),
                Arguments.of("127", Locale.US, "###;(###)", 127.toByte()),
                Arguments.of("(127)", Locale.US, "###;(###)", (-127).toByte()),
            )

        @JvmStatic
        fun invalidDeserializeProvider(): Stream<Arguments> =
            Stream.of(
                Arguments.of("300", Locale.US, "#"),
                Arguments.of("abc", Locale.US, "#"),
                Arguments.of("12 7", Locale.US, "#"),
                Arguments.of("1_27", Locale.US, "#"),
                Arguments.of("127", Locale.US, "invalid.pattern"),
            )

        @JvmStatic
        fun serializeProvider(): Stream<Arguments> =
            Stream.of(
                Arguments.of(1, Locale.US, "", "1"),
                Arguments.of(-1, Locale.US, "", "-1"),
                Arguments.of(127, Locale.US, "", "127"),
                Arguments.of(-128, Locale.US, "", "-128"),
                Arguments.of(127, Locale.US, "#,###", "127"),
                Arguments.of(127, Locale.US, "#,##", "1,27"),
                Arguments.of(127, Locale.GERMANY, "#,##", "1.27"),
                Arguments.of(127, Locale.US, "###;(###)", "127"),
                Arguments.of(-127, Locale.US, "###;(###)", "(127)"),
            )
    }
}
