package io.github.minthem.converter

import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BooleanCsvConverterTest {

    @Nested
    inner class CaseSensitiveTest {
        @Nested
        inner class DeserializeTest {
            @ParameterizedTest
            @NullAndEmptySource
            fun `deserialize should return null for null or blank inputs`(source: String?) {
                val converter = BooleanCsvConverter(listOf("yes", "true"), listOf("no", "false"))
                val result = converter.deserialize(source)
                assertTrue(result.isSuccess)
                assertNull(result.getOrNull())
            }

            @ParameterizedTest
            @MethodSource("io.github.minthem.converter.BooleanCsvConverterTest#truthyFalsyCaseSensitiveProvider")
            fun `deserialize should respect true or false patterns (case-sensitive)`(
                input: String,
                trueValues: List<String>,
                falseValues: List<String>,
                expected: Boolean,
            ) {
                val converter = BooleanCsvConverter(trueValues, falseValues)
                val result = converter.deserialize(input)
                assertTrue(result.isSuccess)
                assertEquals(expected, result.getOrNull())
            }

            @Test
            fun `deserialize should use default exact tokens when none provided`() {
                val converter = BooleanCsvConverter(emptyList(), emptyList())
                val t = converter.deserialize("true")
                val f = converter.deserialize("false")
                assertTrue(t.isSuccess)
                assertEquals(true, t.getOrNull())
                assertTrue(f.isSuccess)
                assertEquals(false, f.getOrNull())
            }

            @Test
            fun `deserialize should fail when input not in either set`() {
                val converter = BooleanCsvConverter(listOf("yes", "true"), listOf("no", "false"))
                val result = converter.deserialize("maybe")
                assertTrue(result.isFailure)
            }
        }

        @Nested
        inner class SerializeTest {
            @ParameterizedTest
            @MethodSource("io.github.minthem.converter.BooleanCsvConverterTest#serializeCaseSensitiveProvider")
            fun `serialize should pick first token of true or false side`(
                value: Boolean,
                trueValues: List<String>,
                falseValues: List<String>,
                expected: String,
            ) {
                val converter = BooleanCsvConverter(trueValues, falseValues)
                val result = converter.serialize(value)
                assertTrue(result.isSuccess)
                assertEquals(expected, result.getOrNull())
            }

            @Test
            fun `serialize should return null when value is null`() {
                val converter = BooleanCsvConverter(emptyList(), emptyList())
                val result = converter.serialize(null)
                assertTrue(result.isSuccess)
                assertNull(result.getOrNull())
            }
        }
    }

    @Nested
    inner class CaseInsensitiveTest {
        @Nested
        inner class DeserializeTest {
            @ParameterizedTest
            @NullAndEmptySource
            fun `deserialize should return null for null or blank inputs`(source: String?) {
                val converter = BooleanCsvConverter(listOf("yes", "true"), listOf("no", "false"), false)
                val result = converter.deserialize(source)
                assertTrue(result.isSuccess)
                assertNull(result.getOrNull())
            }

            @ParameterizedTest
            @MethodSource("io.github.minthem.converter.BooleanCsvConverterTest#truthyFalsyCaseInsensitiveProvider")
            fun `deserialize should respect true or false patterns (case-insensitive)`(
                input: String,
                trueValues: List<String>,
                falseValues: List<String>,
                expected: Boolean,
            ) {
                val converter = BooleanCsvConverter(trueValues, falseValues, false)
                val result = converter.deserialize(input)
                assertTrue(result.isSuccess)
                assertEquals(expected, result.getOrNull())
            }

            @Test
            fun `deserialize should accept default tokens regardless of case when none provided`() {
                val converter = BooleanCsvConverter(emptyList(), emptyList(), false)
                val t = converter.deserialize("TrUe")
                val f = converter.deserialize("FaLsE")
                assertTrue(t.isSuccess)
                assertEquals(true, t.getOrNull())
                assertTrue(f.isSuccess)
                assertEquals(false, f.getOrNull())
            }

            @Test
            fun `deserialize should fail when input not in either set`() {
                val converter = BooleanCsvConverter(listOf("yes", "true"), listOf("no", "false"), false)
                val result = converter.deserialize("maybe")
                assertTrue(result.isFailure)
            }
        }

        @Nested
        inner class SerializeTest {
            @ParameterizedTest
            @MethodSource("io.github.minthem.converter.BooleanCsvConverterTest#serializeCaseInsensitiveProvider")
            fun `serialize should pick first token of true or false side`(
                value: Boolean,
                trueValues: List<String>,
                falseValues: List<String>,
                expected: String,
            ) {
                val converter = BooleanCsvConverter(trueValues, falseValues, false)
                val result = converter.serialize(value)
                assertTrue(result.isSuccess)
                assertEquals(expected, result.getOrNull())
            }

            @Test
            fun `serialize should return null when value is null`() {
                val converter = BooleanCsvConverter(emptyList(), emptyList(), false)
                val result = converter.serialize(null)
                assertTrue(result.isSuccess)
                assertNull(result.getOrNull())
            }
        }
    }

    companion object {
        @JvmStatic
        fun truthyFalsyCaseSensitiveProvider(): Stream<Arguments> =
            Stream.of(
                Arguments
                    .of("yes", listOf("yes", "true"), listOf("no", "false"), true),
                Arguments
                    .of("no", listOf("yes", "true"), listOf("no", "false"), false),
                // different sets
                Arguments
                    .of(
                        "on",
                        listOf("on", "yes", "true"),
                        listOf("off", "no", "false"),
                        true,
                    ),
                Arguments
                    .of("off", listOf("on", "yes", "true"), listOf("off", "no", "false"), false),
            )

        @JvmStatic
        fun serializeCaseSensitiveProvider(): Stream<Arguments> =
            Stream.of(
                Arguments
                    .of(true, listOf("true"), listOf("false"), "true"),
                Arguments
                    .of(false, listOf("true"), listOf("false"), "false"),
                Arguments
                    .of(true, listOf("Y", "Yes"), listOf("N", "No"), "Y"),
                Arguments
                    .of(false, listOf("Y", "Yes"), listOf("N", "No"), "N"),
                Arguments
                    .of(true, listOf("はい", "真"), listOf("いいえ", "偽"), "はい"),
                Arguments
                    .of(false, listOf("はい", "真"), listOf("いいえ", "偽"), "いいえ"),
            )

        @JvmStatic
        fun truthyFalsyCaseInsensitiveProvider(): Stream<Arguments> =
            Stream.of(
                // case-insensitive checks
                Arguments
                    .of("YES", listOf("yes", "true"), listOf("no", "false"), true),
                Arguments
                    .of("No", listOf("yes", "true"), listOf("no", "false"), false),
                // different sets with mixed case inputs
                Arguments
                    .of("On", listOf("on", "yes", "true"), listOf("off", "no", "false"), true),
                Arguments
                    .of("OFF", listOf("on", "yes", "true"), listOf("off", "no", "false"), false),
            )

        @JvmStatic
        fun serializeCaseInsensitiveProvider(): Stream<Arguments> =
            Stream.of(
                Arguments
                    .of(true, listOf("true"), listOf("false"), "true"),
                Arguments
                    .of(false, listOf("true"), listOf("false"), "false"),
                Arguments
                    .of(true, listOf("Y", "Yes"), listOf("N", "No"), "Y"),
                Arguments
                    .of(false, listOf("Y", "Yes"), listOf("N", "No"), "N"),
                Arguments
                    .of(true, listOf("はい", "真"), listOf("いいえ", "偽"), "はい"),
                Arguments
                    .of(false, listOf("はい", "真"), listOf("いいえ", "偽"), "いいえ"),
            )
    }
}

