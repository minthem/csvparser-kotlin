package io.github.minthem.csvparser.converter

class StringCsvConverter : CsvConverter<String> {
    override fun deserialize(value: String?): Result<String?> =
        runCatching {
            value
        }

    override fun serialize(value: String?): Result<String?> =
        runCatching {
            value
        }
}
