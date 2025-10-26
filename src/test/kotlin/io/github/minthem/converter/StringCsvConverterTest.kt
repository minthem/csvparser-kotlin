package io.github.minthem.converter

import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StringCsvConverterTest {
    @Nested
    inner class DeserializeTest {
        @ParameterizedTest
        @NullAndEmptySource
        fun `deserialize should return null for null or blank inputs`(source: String?) {
            val converter = StringCsvConverter()
            val result = converter.deserialize(source)
            assertTrue(result.isSuccess)
            assertEquals(source, result.getOrNull())
        }

        @Test
        fun `deserialize should return the original string when non-blank`() {
            val converter = StringCsvConverter()
            val result = converter.deserialize("test string")
            assertTrue(result.isSuccess)
            assertEquals("test string", result.getOrNull())
        }
    }

    @Nested
    inner class SerializeTest {
        @Test
        fun `serialize should return the original string`() {
            val converter = StringCsvConverter()
            val result = converter.serialize("output string")
            assertTrue(result.isSuccess)
            assertEquals("output string", result.getOrNull())
        }

        @Test
        fun `serialize should return null when value is null`() {
            val converter = StringCsvConverter()
            val result = converter.serialize(null)
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }
    }
}
