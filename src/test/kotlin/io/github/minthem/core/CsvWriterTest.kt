package io.github.minthem.core

import io.github.minthem.config.CsvConfig
import io.github.minthem.config.WriterConfig
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.IOException

class CsvWriterTest {

    private fun newWriter(out: Appendable = StringBuilder(), nullValue: String = ""): Pair<CsvWriter, Appendable> {
        val config = CsvConfig(delimiter = ',', quoteChar = '"', nullValue = nullValue)
        val writer = CsvWriter(out, config, WriterConfig(WriterConfig.LineSeparator.LF))
        return writer to out
    }

    @Nested
    inner class NormalTest {
        @Test
        fun `should write header and rows`() {
            val (writer, out) = newWriter()
            writer.writeHeader(listOf("name", "age"))
            writer.writeRow(Row(listOf("Alice", "24"), mapOf("name" to 0, "age" to 1)))

            out.toString() shouldBe "name,age\nAlice,24\n"
        }

        @Test
        fun `should write rows without header`() {
            val (writer, out) = newWriter()
            writer.writeRow(Row(listOf("Alice", "24")))

            out.toString() shouldBe "Alice,24\n"
        }

        @Test
        fun `should throw when writing header twice`() {
            val (writer, _) = newWriter()
            writer.writeHeader(listOf("name", "age"))

            shouldThrow<IllegalStateException> {
                writer.writeHeader(listOf("x", "y"))
            }
        }

        @Test
        fun `should throw when writing header after rows`() {
            val (writer, _) = newWriter()
            writer.writeRow(Row(listOf("Alice", "24")))

            shouldThrow<IllegalStateException> {
                writer.writeHeader(listOf("name", "age"))
            }
        }

        @Test
        fun `should quote cell containing delimiter`() {
            val (writer, out) = newWriter()
            writer.writeRow(Row(listOf("Alice,Bob", "24")))

            out.toString() shouldBe "\"Alice,Bob\",24\n"
        }

        @Test
        fun `should quote cell containing newline`() {
            val (writer, out) = newWriter()
            writer.writeRow(Row(listOf("Alice\nBob", "24")))

            out.toString() shouldBe "\"Alice\nBob\",24\n"
        }

        @Test
        fun `should escape quote character in cell`() {
            val (writer, out) = newWriter()
            writer.writeRow(Row(listOf("He said \"Hello\"", "24")))

            out.toString() shouldBe "\"He said \"\"Hello\"\"\",24\n"
        }

        @Test
        fun `should replace null with configured nullValue`() {
            val (writer, out) = newWriter()
            writer.writeRow(Row(listOf(null, "24")))

            out.toString() shouldBe ",24\n"
        }

        @Test
        fun `should replace null with specific nullValue`() {
            val (writer, out) = newWriter(nullValue = "NULL")
            writer.writeRow(Row(listOf(null, "24")))

            out.toString() shouldBe "NULL,24\n"
        }

        @Test
        fun `should write multiple rows correctly`() {
            val (writer, out) = newWriter()
            writer.writeHeader(listOf("name", "age"))
            writer.writeRow(Row(listOf("Alice", "24"), mapOf("name" to 0, "age" to 1)))
            writer.writeRow(Row(listOf("Bob", "30"), mapOf("name" to 0, "age" to 1)))

            out.toString() shouldBe "name,age\nAlice,24\nBob,30\n"
        }

        @Test
        fun `should write multiple rows correctly without header`() {
            val (writer, out) = newWriter()
            writer.writeRow(Row(listOf("Alice", "24"), mapOf("name" to 0, "age" to 1)))
            writer.writeRow(Row(listOf("Bob", "30"), mapOf("name" to 0, "age" to 1)))

            out.toString() shouldBe "Alice,24\nBob,30\n"
        }

        @Test
        fun `should write empty string as empty cell`() {
            val (writer, out) = newWriter()
            writer.writeRow(Row(listOf("", "24")))

            out.toString() shouldBe "\"\",24\n"
        }

        @Test
        fun `should respect custom delimiter`() {
            val out = StringBuilder()
            val config = CsvConfig(delimiter = ';', quoteChar = '"', nullValue = "NULL")
            val writer = CsvWriter(out, config, WriterConfig(WriterConfig.LineSeparator.LF))

            writer.writeRow(Row(listOf("Alice", "24", null, "abc\"def\"\"'")))
            out.toString() shouldBe "Alice;24;NULL;\"abc\"\"def\"\"\"\"'\"\n"
        }

        @Test
        fun `should respect custom line separator`() {
            val out = StringBuilder()
            val config = CsvConfig()
            val writer = CsvWriter(out, config, WriterConfig(WriterConfig.LineSeparator.CRLF))

            writer.writeRow(Row(listOf("Alice", "24")))
            out.toString() shouldBe "Alice,24\r\n"
        }

        @Test
        fun `should fill missing header columns with nullValue`() {
            val (writer, out) = newWriter()
            writer.writeHeader(listOf("name", "age", "city"))
            writer.writeRow(Row(listOf("Alice", "24"), mapOf("name" to 0, "age" to 1)))

            out.toString() shouldBe "name,age,city\nAlice,24,\n"
        }

        @Test
        fun `should escape cell containing delimiter newline and quote`() {
            val (writer, out) = newWriter()
            writer.writeRow(Row(listOf("Alice,\"Bob\"\nTokyo", "24")))

            out.toString() shouldBe "\"Alice,\"\"Bob\"\"\nTokyo\",24\n"
        }
    }

    @Nested
    inner class ErrorTest {

        @Test
        fun `should throw IllegalArgumentException when header is empty`() {
            val (writer, _) = newWriter()
            val e = shouldThrow<IllegalArgumentException> {
                writer.writeHeader(emptyList())
            }
            e.message shouldBe "Header cannot be empty."
        }

        @Test
        fun `should throw IllegalArgumentException when header contains blank strings`() {
            val (writer, _) = newWriter()
            val e = shouldThrow<IllegalArgumentException> {
                writer.writeHeader(listOf("name", " ", "age"))
            }
            e.message shouldBe "Header cannot contain empty columns."
        }

        @Test
        fun `should throw IllegalArgumentException when header contains duplicate names`() {
            val (writer, _) = newWriter()
            val e = shouldThrow<IllegalArgumentException> {
                writer.writeHeader(listOf("name", "age", "name"))
            }
            e.message shouldBe "Header cannot contain duplicate columns."
        }

        @Test
        fun `should throw IllegalStateException when writing row before header`() {
            val (writer, _) = newWriter()
            writer.writeRow(Row(listOf("Alice", "24")))
            val e = shouldThrow<IllegalStateException> {
                writer.writeHeader(listOf("Name", "Age"))
            }
            e.message shouldBe "Cannot write header after rows have been written."
        }

        @Test
        fun `should throw IllegalStateException when writing header after header`() {
            val (writer, _) = newWriter()
            writer.writeHeader(listOf("Name", "Age"))
            val e = shouldThrow<IllegalStateException> {
                writer.writeHeader(listOf("Name", "Age"))
            }
            e.message shouldBe "Header has already been written."
        }

        @Test
        fun `should propagate IOException when writing header`() {
            class TestAppendable : Appendable {
                override fun append(csq: CharSequence?): Appendable {
                    throw IOException("Test exception")
                }

                override fun append(p0: CharSequence?, p1: Int, p2: Int): java.lang.Appendable = this

                override fun append(p0: Char): java.lang.Appendable = this
            }

            val (writer, _) = newWriter(TestAppendable())
            shouldThrow<IOException> {
                writer.writeHeader(listOf("Name", "Age"))
            }
        }

        @Test
        fun `should propagate IOException when writing row`() {
            class TestAppendable : Appendable {
                override fun append(csq: CharSequence?): Appendable {
                    throw IOException("Test exception")
                }

                override fun append(p0: CharSequence?, p1: Int, p2: Int): java.lang.Appendable = this

                override fun append(p0: Char): java.lang.Appendable = this
            }

            val (writer, _) = newWriter(TestAppendable())
            shouldThrow<IOException> {
                writer.writeRow(Row(listOf("Alice", "24")))
            }

        }
    }
}
