package io.github.minthem.converter

import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import java.util.Locale
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
            val result = StringCsvConverter.deserialize(source, Locale.US, "")
            assertTrue(result.isSuccess)
            assertEquals(source, result.getOrNull())
        }

        @Test
        fun `deserialize should return the original string when non-blank`() {
            val result = StringCsvConverter.deserialize("test string", Locale.US, "")
            assertTrue(result.isSuccess)
            assertEquals("test string", result.getOrNull())
        }
    }

    @Nested
    inner class SerializeTest {
        @Test
        fun `serialize should return the original string`() {
            val result = StringCsvConverter.serialize("output string", Locale.US, "")
            assertTrue(result.isSuccess)
            assertEquals("output string", result.getOrNull())
        }

        @Test
        fun `serialize should return null when value is null`() {
            val result = StringCsvConverter.serialize(null, Locale.US, "")
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }
    }
}
