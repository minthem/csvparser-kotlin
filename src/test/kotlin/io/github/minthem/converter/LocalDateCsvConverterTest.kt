package io.github.minthem.converter

import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalDateCsvConverterTest {
    @Nested
    inner class DeserializeTest {
        @ParameterizedTest
        @NullAndEmptySource
        fun `deserialize should return null for null or blank inputs`(source: String?) {
            val converter =
                LocalDateCsvConverter
                    .Builder()
                    .locale(Locale.getDefault())
                    .pattern("yyyy-MM-dd")
                    .build()
            val result = converter.deserialize(source)
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

        @Test
        fun `deserialize valid date with default locale`() {
            val converter =
                LocalDateCsvConverter
                    .Builder()
                    .locale(Locale.getDefault())
                    .pattern("yyyy-MM-dd")
                    .build()
            val result = converter.deserialize("2025-10-11")
            assertTrue(result.isSuccess)
            assertEquals(LocalDate.of(2025, 10, 11), result.getOrNull())
        }

        @Test
        fun `deserialize valid date with specific locale`() {
            val converter =
                LocalDateCsvConverter
                    .Builder()
                    .locale(Locale.FRENCH)
                    .pattern("dd MMMM yyyy")
                    .build()
            val result = converter.deserialize("11 octobre 2025")
            assertTrue(result.isSuccess)
            assertEquals(LocalDate.of(2025, 10, 11), result.getOrNull())
        }

        @Test
        fun `deserialize should fail on invalid format`() {
            val converter =
                LocalDateCsvConverter
                    .Builder()
                    .locale(Locale.getDefault())
                    .pattern("yyyy-MM-dd")
                    .build()
            val result = converter.deserialize("11/10/2025")
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is DateTimeParseException)
        }
    }

    @Nested
    inner class SerializeTest {
        @Test
        fun `serialize valid date with default locale`() {
            val converter =
                LocalDateCsvConverter
                    .Builder()
                    .locale(Locale.getDefault())
                    .pattern("yyyy-MM-dd")
                    .build()
            val result = converter.serialize(LocalDate.of(2025, 10, 11))
            assertTrue(result.isSuccess)
            assertEquals("2025-10-11", result.getOrNull())
        }

        @Test
        fun `serialize valid date with specific locale`() {
            val converter =
                LocalDateCsvConverter
                    .Builder()
                    .locale(Locale.FRENCH)
                    .pattern("dd MMMM yyyy")
                    .build()
            val result = converter.serialize(LocalDate.of(2025, 10, 11))
            assertTrue(result.isSuccess)
            assertEquals("11 octobre 2025", result.getOrNull())
        }

        @Test
        fun `serialize should return null when value is null`() {
            val converter =
                LocalDateCsvConverter
                    .Builder()
                    .locale(Locale.getDefault())
                    .pattern("yyyy-MM-dd")
                    .build()
            val result = converter.serialize(null)
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }
    }
}
