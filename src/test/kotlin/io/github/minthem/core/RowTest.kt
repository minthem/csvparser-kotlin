package io.github.minthem.core

import io.github.minthem.exception.CsvColumnNotFoundException
import io.github.minthem.exception.CsvHeaderNotFoundException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RowTest {
    private val header = mapOf("name" to 0, "email" to 1, "city" to 2)
    private val cells = listOf("Foo", "test@example.com", "Tokyo")

    @Test
    fun `index access should return correct cell`() {
        val row = Row(cells, header)
        row[0] shouldBe "Foo"
        row[1] shouldBe "test@example.com"
        row[2] shouldBe "Tokyo"
    }

    @Test
    fun `index access out of bounds should throw`() {
        val row = Row(cells, header)
        shouldThrow<IndexOutOfBoundsException> {
            row[3]
        }
    }

    @Test
    fun `getOrNull by index should return null if out of bounds`() {
        val row = Row(cells, header)
        row.getOrNull(3) shouldBe null
    }

    @Test
    fun `column access should return correct cell`() {
        val row = Row(cells, header)
        row["name"] shouldBe "Foo"
        row["email"] shouldBe "test@example.com"
        row["city"] shouldBe "Tokyo"
    }

    @Test
    fun `column access should throw if header is missing`() {
        val row = Row(cells, null)
        shouldThrow<CsvHeaderNotFoundException> {
            row["name"]
        }
    }

    @Test
    fun `column access should throw if column not found`() {
        val row = Row(cells, header)
        shouldThrow<CsvColumnNotFoundException> {
            row["country"]
        }
    }

    @Test
    fun `getOrNull by column should return null if header missing`() {
        val row = Row(cells, null)
        row.getOrNull("name") shouldBe null
    }

    @Test
    fun `getOrNull by column should return null if column not found`() {
        val row = Row(cells, header)
        row.getOrNull("country") shouldBe null
    }

    @Test
    fun `toString should join cells with brackets`() {
        val row = Row(cells, header)
        row.toString() shouldBe "[Foo, test@example.com, Tokyo]"
    }
}
