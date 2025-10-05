# csvparser

A simple and practical CSV/TSV reader & writer for Kotlin. It covers everyday use cases: headers, automatic handling of line breaks, quoting and escaping, distinguishing empty cells (null) from empty strings, skipping rows and invalid lines, etc.

- Language/Target: Kotlin/JVM (Kotlin 2.2, JVM 21)
- Package: `io.github.minthem`
- License: MIT

## Features
- Read CSV/TSV (with or without header)
- Supports CRLF/LF/CR line endings. Newlines inside quotes are preserved as-is
- Customizable delimiter and quote character (e.g., TSV with `\t`, single quote quoteChar `'`)
- Escape quotes by doubling them (`"He said ""Hello"""` → `He said "Hello"`)
- Treat empty cells (`,,` or trailing delimiter) as null, while `""` becomes an empty string
- Control row skipping, allow/deny blank lines, and skip invalid lines
- Writer supports header and row output, quoting when necessary, configurable line separator, and `null` replacement via `nullValue`

## Installation
Planned coordinates for Maven Central. Until it is published, use local publish or `mavenLocal()`.

- Group: `io.github.minthem`
- Artifact: `csvparser`
- Version: `0.0.1-SNAPSHOT` (when CI_TAG is not set)

Gradle (Kotlin DSL):

```kotlin
repositories {
    mavenCentral()
    // mavenLocal() // Use when you publish locally
}

dependencies {
    implementation("io.github.minthem:csvparser:0.0.1-SNAPSHOT")
}
```

## Quick Start

- Read CSV with a header:

```kotlin
val csv = """
name,age,city
Alice,30,Tokyo
Bob,25,Osaka
""".trimIndent()

val reader = io.github.minthem.core.CsvReader(
    java.io.StringReader(csv),
    io.github.minthem.config.CsvConfig(),
    io.github.minthem.config.ReaderConfig(hasHeader = true)
)
println(reader.header()) // [name, age, city]

for (row in reader) {
    println(row["name"]) // Access by header name
}
```

- Read CSV without a header:

```kotlin
val reader = CsvReader(StringReader("Alice,30,Tokyo"), CsvConfig(), ReaderConfig(hasHeader = false))
val first = reader.first()
println(first[0]) // Access by index
```

- Read TSV (tab-delimited):

```kotlin
val tsv = """
name	age
Alice	30
""".trimIndent()
val tsvReader = CsvReader(StringReader(tsv), CsvConfig(delimiter = '\t'), ReaderConfig(hasHeader = true))
```

## API Overview (main)

- CsvReader(reader: Reader, config: CsvConfig = CsvConfig(), readConfig: ReaderConfig = ReaderConfig()) : Iterable<Row>
  - header(): List<String>? — Returns header when `hasHeader=true`
  - Iterate to get Row values
- Row
  - Index access: `row[0]`
  - Name access: `row["name"]` (throws if no header)
  - Safe access via `getOrNull(index)` / `getOrNull(name)`
  - `toString()` returns `[a, b, c]`
- CsvWriter(out: Appendable, config: CsvConfig, writeConfig: WriterConfig = WriterConfig())
  - writeHeader(header: List<String>)
  - writeRow(row: Row)

## Configuration
- CsvConfig
  - delimiter: Char = ','
  - quoteChar: Char = '"'
  - locale: Locale = Locale.getDefault()
  - strictMode: Boolean = true
  - nullValue: String = "" // String used when writer outputs null
- ReaderConfig
  - skipRows: Int = 0 // Number of lines to skip at the beginning (e.g., comments)
  - hasHeader: Boolean = true
  - ignoreBlankLine: Boolean = false // On blank line: true=skip, false=throw
  - skipInvalidLine: Boolean = false // Whether to skip invalid formatted lines (false=throw)
- WriterConfig
  - lineSeparator: LineSeparator = SYSTEM // CRLF/LF/CR/SYSTEM

## Exceptions
- CsvFormatException(lineNo, position): Invalid CSV/TSV format within a line (e.g., unclosed quote, illegal char after closing quote)
- CsvLineFormatException(lineNo): Line-level error (e.g., invalid header, column count mismatch with header, blank line when not allowed)
- CsvHeaderNotFoundException: Accessed a column by name when no header exists
- CsvColumnNotFoundException: Accessed a non-existing column by name

## Reading behavior (highlights)
- Empty cells: `,,` or trailing delimiter become null
- Empty string: `""` becomes an empty string (distinct from null)
- `""` inside quotes becomes a single `"`
- Newlines inside quotes remain inside the same cell
- When hasHeader=true:
  - Header names must be non-blank and unique (otherwise throws)
  - Data rows must have the same column count as the header (otherwise throws)

## Writing behavior (highlights)
- Cells are quoted when necessary (when containing delimiter/newline/quote)
- Quotes are escaped by doubling them (`"` → `""`)
- `null` is replaced with `CsvConfig.nullValue` (default is empty string)
- When a header is written, rows are output in header order; missing columns are filled with `nullValue`
- You can choose line endings with `WriterConfig.lineSeparator` (LF/CRLF/CR/SYSTEM)

### Writing example

```kotlin
val out = StringBuilder()
val writer = io.github.minthem.core.CsvWriter(
    out,
    io.github.minthem.config.CsvConfig(nullValue = "NULL"),
    io.github.minthem.config.WriterConfig(lineSeparator = io.github.minthem.config.WriterConfig.LineSeparator.LF)
)

writer.writeHeader(listOf("name", "age"))
writer.writeRow(io.github.minthem.core.Row(listOf("Alice", "24"), mapOf("name" to 0, "age" to 1)))
writer.writeRow(io.github.minthem.core.Row(listOf("Bob", null), mapOf("name" to 0, "age" to 1)))

println(out.toString())
// name,age\n
// Alice,24\n
// Bob,NULL\n
```

## Development
- Requirements
  - JDK 21
  - Kotlin 2.2.x
- Build/Test
  - Run: `./gradlew test`
  - Code style: ktlint (`./gradlew ktlintCheck`)
  - Coverage: Kover (e.g., `./gradlew koverHtmlReport`; some report thresholds require 85%)
- Publishing
  - Uses `com.vanniktech.maven.publish`
  - See build.gradle.kts for POM/coordinates

## License
MIT License. See LICENSE for details.