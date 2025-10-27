package io.github.minthem.converter

class BooleanCsvConverter(
    private val trueValues: List<String> = listOf("true", "yes", "ok"),
    private val falseValues: List<String> = listOf("false", "no", "bad"),
    private val caseSensitive: Boolean = true,
) : CsvConverter<Boolean> {
    override fun deserialize(value: String?): Result<Boolean?> {
        return runCatching {
            if (value.isNullOrBlank()) return@runCatching null

            val normalized = if (caseSensitive) value.trim() else value.trim().lowercase()

            if (trueValues.isEmpty() && falseValues.isEmpty()) {
                return@runCatching normalized.toBooleanStrictOrNull()
            }

            val trueVals = if (caseSensitive) trueValues else trueValues.map { it.lowercase() }
            val falseVals = if (caseSensitive) falseValues else falseValues.map { it.lowercase() }

            when (normalized) {
                in trueVals -> true
                in falseVals -> false
                else -> throw IllegalArgumentException(
                    "Invalid boolean value: \"$value\" (trueValues: $trueValues, falseValues: $falseValues, caseSensitive=true)",
                )
            }
        }
    }

    override fun serialize(value: Boolean?): Result<String?> {
        return runCatching {
            if (value == null) return@runCatching null
            if (trueValues.isEmpty() && falseValues.isEmpty()) return@runCatching value.toString()

            if (value) trueValues.first() else falseValues.first()
        }
    }
}
