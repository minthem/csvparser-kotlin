package io.github.minthem.converter

import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalDateTimeCsvConverterTest {

    @Nested
    inner class DeserializeTest {
        @ParameterizedTest
        @NullAndEmptySource
        fun `deserialize should return null for null or blank inputs`(source: String?) {
            val result = LocalDateTimeCsvConverter.deserialize(source, Locale.getDefault(), "yyyy-MM-dd'T'HH:mm:ss")
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }

        @Test
        fun `deserialize valid date-time using default locale`() {
            val result = LocalDateTimeCsvConverter.deserialize("2025-10-11T14:30:00", Locale.getDefault(), "yyyy-MM-dd'T'HH:mm:ss")
            assertTrue(result.isSuccess)
            assertEquals(LocalDateTime.of(2025, 10, 11, 14, 30, 0), result.getOrNull())
        }

        @Test
        fun `deserialize valid date-time with specific locale`() {
            val result = LocalDateTimeCsvConverter.deserialize("11.10.2025 14:30:00", Locale.GERMANY, "dd.MM.yyyy HH:mm:ss")
            assertTrue(result.isSuccess)
            assertEquals(LocalDateTime.of(2025, 10, 11, 14, 30, 0), result.getOrNull())
        }

        @Test
        fun `deserialize should fail on invalid pattern`() {
            val result = LocalDateTimeCsvConverter.deserialize("2025-10-11T14:30:00", Locale.getDefault(), "invalid-pattern")
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        }

        @Test
        fun `deserialize should fail on invalid date-time`() {
            val result = LocalDateTimeCsvConverter.deserialize("11-10-2025 14:30:00", Locale.getDefault(), "yyyy-MM-dd'T'HH:mm:ss")
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is DateTimeParseException)
        }
    }

    @Nested
    inner class SerializeTest {
        @Test
        fun `serialize valid date-time with default locale`() {
            val value = LocalDateTime.of(2025, 10, 11, 14, 30, 0)
            val result = LocalDateTimeCsvConverter.serialize(value, Locale.getDefault(), "yyyy-MM-dd'T'HH:mm:ss")
            assertTrue(result.isSuccess)
            assertEquals("2025-10-11T14:30:00", result.getOrNull())
        }

        @Test
        fun `serialize should return null when value is null`() {
            val result = LocalDateTimeCsvConverter.serialize(null, Locale.getDefault(), "yyyy-MM-dd'T'HH:mm:ss")
            assertTrue(result.isSuccess)
            assertNull(result.getOrNull())
        }
    }
}