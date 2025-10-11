package io.github.minthem.converter

import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import java.util.*
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BooleanCsvConverterTest {

    @Nested
    inner class DeserializeTest {
        @ParameterizedTest
        @NullAndEmptySource
        fun `deserialize should return null for null or blank inputs`(source: String?) {
            val result = BooleanCsvConverter.deserialize(source, Locale.getDefault(), "yes,true|no,false")
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.BooleanCsvConverterTest#truthyFalsyProvider")
        fun `deserialize should respect true or false patterns (case-sensitive)`(
            input: String,
            pattern: String,
            expected: Boolean
        ) {
            val result = BooleanCsvConverter.deserialize(input, Locale.getDefault(), pattern)
            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

        @Test
        fun `deserialize should use default exact tokens when none provided`() {
            val t = BooleanCsvConverter.deserialize("true", Locale.getDefault(), "")
            val f = BooleanCsvConverter.deserialize("false", Locale.getDefault(), "")
            assertTrue(t.isSuccess); assertEquals(true, t.getOrNull())
            assertTrue(f.isSuccess); assertEquals(false, f.getOrNull())
        }

        @Test
        fun `deserialize should fail when input not in either set`() {
            val result = BooleanCsvConverter.deserialize("maybe", Locale.getDefault(), "yes,true|no,false")
            assertTrue(result.isFailure)
        }
    }

    @Nested
    inner class SerializeTest {
        @ParameterizedTest
        @MethodSource("io.github.minthem.converter.BooleanCsvConverterTest#serializeProvider")
        fun `serialize should pick first token of true or false side`(
            value: Boolean,
            pattern: String,
            expected: String
        ) {
            val result = BooleanCsvConverter.serialize(value, Locale.getDefault(), pattern)
            assertTrue(result.isSuccess)
            assertEquals(expected, result.getOrNull())
        }

        @Test
        fun `serialize should return null when value is null`() {
            val result = BooleanCsvConverter.serialize(null, Locale.getDefault(), "")
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }
    }

    companion object {
        @JvmStatic
        fun truthyFalsyProvider(): Stream<org.junit.jupiter.params.provider.Arguments> = Stream.of(
            org.junit.jupiter.params.provider.Arguments.of("yes", "yes,true|no,false", true),
            org.junit.jupiter.params.provider.Arguments.of("no", "yes,true|no,false", false),
            // different sets
            org.junit.jupiter.params.provider.Arguments.of("on", "on,yes,true|off,no,false", true),
            org.junit.jupiter.params.provider.Arguments.of("off", "on,yes,true|off,no,false", false),
        )

        @JvmStatic
        fun serializeProvider(): Stream<org.junit.jupiter.params.provider.Arguments> = Stream.of(
            org.junit.jupiter.params.provider.Arguments.of(true, "", "true"),
            org.junit.jupiter.params.provider.Arguments.of(false, "", "false"),
            org.junit.jupiter.params.provider.Arguments.of(true, "Y,Yes|N,No", "Y"),
            org.junit.jupiter.params.provider.Arguments.of(false, "Y,Yes|N,No", "N"),
            org.junit.jupiter.params.provider.Arguments.of(true, "はい,真|いいえ,偽", "はい"),
            org.junit.jupiter.params.provider.Arguments.of(false, "はい,真|いいえ,偽", "いいえ"),
        )
    }
}