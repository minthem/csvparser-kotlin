package io.github.minthem.core

internal class Tokenizer(
    private val delimiter: Char = ',',
    private val quote: Char = '"'
) {

    fun tokenize(line: String): List<String?> {
        val context = Context(delimiter, quote)
        var state = State.START

        var index = 0
        while (index < line.length) {
            val ch = line[index]
            state = state.action(ch, context)
            index++
        }

        if (state == State.IN_QUOTE_FIELD) {
            throw IllegalArgumentException("Unclosed quoted field")
        }

        context.endToken()

        return context.result();
    }
}

private class Context(
    val delimiter: Char, val quote: Char,
) {
    private val result = mutableListOf<String?>()
    private val sb = StringBuilder()
    private var inQuote = false

    fun endToken() {
        result.add(
            when {
                inQuote -> sb.toString()
                sb.isNotEmpty() -> sb.toString()
                else -> null
            }
        )
        sb.clear()
        inQuote = false
    }

    fun startQuoteField() {
        inQuote = true
    }

    fun result(): List<String?> {
        return result
    }

    fun addChar(ch: Char) {
        sb.append(ch)
    }
};

private enum class State {

    START {
        override fun action(char: Char, ctx: Context): State {
            return when (char) {
                ctx.delimiter -> {
                    ctx.endToken()
                    START
                }

                ctx.quote -> {
                    ctx.startQuoteField()
                    IN_QUOTE_FIELD
                }

                else -> {
                    ctx.addChar(char)
                    IN_FIELD
                }
            }
        }
    },
    IN_FIELD {
        override fun action(char: Char, ctx: Context): State {
            return when (char) {
                ctx.delimiter -> {
                    ctx.endToken()
                    START
                }

                ctx.quote -> {
                    throw IllegalArgumentException("Unexpected quote inside unquoted field")
                }

                else -> {
                    ctx.addChar(char)
                    IN_FIELD
                }
            }
        }
    },
    IN_QUOTE_FIELD {
        override fun action(char: Char, ctx: Context): State {
            return when (char) {
                ctx.quote -> {
                    AFTER_QUOTE
                }

                else -> {
                    ctx.addChar(char)
                    IN_QUOTE_FIELD
                }
            }
        }
    },
    AFTER_QUOTE {
        override fun action(char: Char, ctx: Context): State {
            return when (char) {
                ctx.delimiter -> {
                    ctx.endToken()
                    START
                }

                ctx.quote -> {
                    ctx.addChar(char)
                    IN_QUOTE_FIELD
                }

                else -> {
                    throw IllegalArgumentException("Unexpected character after closing quote: $char")
                }
            }
        }
    };

    abstract fun action(char: Char, ctx: Context): State
};