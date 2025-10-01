package io.github.minthem.config

import io.github.minthem.config.WriterConfig.LineSeparator
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.*

class ConfigTest {

    @Test
    fun `CsvConfig default values`() {
        val config = CsvConfig()
        config.delimiter shouldBe ','
        config.quoteChar shouldBe '"'
        config.locale shouldBe Locale.getDefault()
        config.strictMode shouldBe true
    }

    @Test
    fun `CsvConfig custom values`() {
        val config = CsvConfig(
            delimiter = ';',
            quoteChar = '\'',
            locale = Locale.JAPAN,
            strictMode = false
        )
        config.delimiter shouldBe ';'
        config.quoteChar shouldBe '\''
        config.locale shouldBe Locale.JAPAN
        config.strictMode shouldBe false
    }

    @Test
    fun `ReaderConfig default and custom`() {
        ReaderConfig().apply {
            skipRows shouldBe 0
            hasHeader shouldBe true
        }
        ReaderConfig(skipRows = 2, hasHeader = false).apply {
            skipRows shouldBe 2
            hasHeader shouldBe false
        }
    }

    @Test
    fun `WriterConfig default and custom`() {
        WriterConfig().apply {
            lineSeparator.value shouldBe System.lineSeparator()
        }
        WriterConfig(lineSeparator = LineSeparator.LF).apply {
            lineSeparator.value shouldBe "\n"
        }
    }

    @Test
    fun `data class copy and equality`() {
        val base = CsvConfig()
        val copy = base.copy(delimiter = ';')
        copy.delimiter shouldBe ';'
        (base == copy) shouldBe false
    }
}
