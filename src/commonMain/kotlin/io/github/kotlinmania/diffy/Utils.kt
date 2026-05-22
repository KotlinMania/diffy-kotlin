// port-lint: source src/utils.rs
package io.github.kotlinmania.diffy

// Common utilities

/**
 * Classifies lines, converting lines into unique [Long]s for quicker comparison.
 *
 * Upstream's `Classifier<'a, T: ?Sized>` keys its hash map on `&'a T`. Kotlin's
 * built-in equality works directly for [String] but not for [ByteArray], so the
 * Kotlin port routes hashing and equality through the supplied [TextLike], which
 * returns a stable [equalityKey] for each value of [T].
 */
class Classifier<T>(private val textLike: TextLike<T>) {
    private var nextId: Long = 0
    private val uniqueIds: MutableMap<Any, Long> = mutableMapOf()

    private fun classify(record: T): Long =
        uniqueIds.getOrPut(textLike.equalityKey(record)) {
            val id = nextId
            nextId += 1
            id
        }

    fun classifyLines(text: T): Pair<List<T>, List<Long>> {
        val items = mutableListOf<T>()
        val ids = mutableListOf<Long>()
        for (line in LineIter(textLike, text)) {
            items.add(line)
            ids.add(classify(line))
        }
        return items to ids
    }
}

/** Iterator over the lines of a string, including the `\n` character. */
class LineIter<T>(private val textLike: TextLike<T>, initial: T) : Iterator<T> {
    private var current: T = initial

    override fun hasNext(): Boolean = !textLike.isEmpty(current)

    override fun next(): T {
        if (textLike.isEmpty(current)) throw NoSuchElementException()

        val end = textLike.find(current, "\n")?.let { it + 1 } ?: textLike.len(current)

        val (line, remaining) = textLike.splitAt(current, end)
        current = remaining
        return line
    }
}

/**
 * A helper trait for processing text like [String] and [ByteArray].
 * Useful for abstracting over those types for parsing as well as breaking input into lines.
 *
 * Upstream Rust expresses this as `pub trait Text: Eq + Hash` implemented for `str` and
 * `[u8]`. Kotlin can't add trait methods to those external types, so the Kotlin port follows
 * the [SliceLike] pattern from [Range]: methods take the value as a parameter, and the per-type
 * impls live as singleton objects ([StrTextLike], [ByteTextLike]).
 */
interface TextLike<T> {
    fun isEmpty(value: T): Boolean
    fun len(value: T): Int
    fun startsWith(value: T, prefix: String): Boolean
    // The "unused" upstream attribute marks `endsWith` as currently dead; it is kept here
    // to mirror the trait surface.
    fun endsWith(value: T, suffix: String): Boolean
    fun stripPrefix(value: T, prefix: String): T?
    fun stripSuffix(value: T, suffix: String): T?
    fun splitAtExclusive(value: T, needle: String): Pair<T, T>?
    fun find(value: T, needle: String): Int?
    fun splitAt(value: T, mid: Int): Pair<T, T>
    fun asStr(value: T): String?
    fun asBytes(value: T): ByteArray
    // The "unused" upstream attribute marks `lines` as currently dead; it is kept here
    // to mirror the trait surface.
    fun lines(value: T): LineIter<T>

    /**
     * Stable equality / hash key for [value]. Upstream relies on `Eq + Hash` being implemented
     * for the slice type. [String] satisfies that natively, but [ByteArray] uses identity
     * equality, so [ByteTextLike] wraps the bytes in a content-equals box.
     */
    fun equalityKey(value: T): Any

    /**
     * Parse [value] as a [String] and apply [parser]. Upstream signature is
     * `fn parse<T: std::str::FromStr>(&self) -> Option<T>`; Kotlin has no `FromStr` trait, so
     * the call site supplies the parser closure (typically a stdlib helper such as
     * `String::toIntOrNull`).
     */
    fun <R> parse(value: T, parser: (String) -> R?): R? = asStr(value)?.let(parser)
}

object StrTextLike : TextLike<String> {
    override fun isEmpty(value: String): Boolean = value.isEmpty()

    override fun len(value: String): Int = value.length

    override fun startsWith(value: String, prefix: String): Boolean = value.startsWith(prefix)

    override fun endsWith(value: String, suffix: String): Boolean = value.endsWith(suffix)

    override fun stripPrefix(value: String, prefix: String): String? =
        if (value.startsWith(prefix)) value.substring(prefix.length) else null

    override fun stripSuffix(value: String, suffix: String): String? =
        if (value.endsWith(suffix)) value.substring(0, value.length - suffix.length) else null

    override fun splitAtExclusive(value: String, needle: String): Pair<String, String>? {
        val idx = value.indexOf(needle)
        if (idx < 0) return null
        return value.substring(0, idx) to value.substring(idx + needle.length)
    }

    override fun find(value: String, needle: String): Int? {
        val idx = value.indexOf(needle)
        return if (idx < 0) null else idx
    }

    override fun splitAt(value: String, mid: Int): Pair<String, String> =
        value.substring(0, mid) to value.substring(mid)

    override fun asStr(value: String): String = value

    override fun asBytes(value: String): ByteArray = value.encodeToByteArray()

    override fun lines(value: String): LineIter<String> = LineIter(this, value)

    override fun equalityKey(value: String): Any = value
}

object ByteTextLike : TextLike<ByteArray> {
    override fun isEmpty(value: ByteArray): Boolean = value.isEmpty()

    override fun len(value: ByteArray): Int = value.size

    override fun startsWith(value: ByteArray, prefix: String): Boolean {
        val prefixBytes = prefix.encodeToByteArray()
        if (prefixBytes.size > value.size) return false
        for (i in prefixBytes.indices) {
            if (value[i] != prefixBytes[i]) return false
        }
        return true
    }

    override fun endsWith(value: ByteArray, suffix: String): Boolean {
        val suffixBytes = suffix.encodeToByteArray()
        if (suffixBytes.size > value.size) return false
        val base = value.size - suffixBytes.size
        for (i in suffixBytes.indices) {
            if (value[base + i] != suffixBytes[i]) return false
        }
        return true
    }

    override fun stripPrefix(value: ByteArray, prefix: String): ByteArray? {
        val prefixBytes = prefix.encodeToByteArray()
        if (!startsWith(value, prefix)) return null
        return value.copyOfRange(prefixBytes.size, value.size)
    }

    override fun stripSuffix(value: ByteArray, suffix: String): ByteArray? {
        val suffixBytes = suffix.encodeToByteArray()
        if (!endsWith(value, suffix)) return null
        return value.copyOfRange(0, value.size - suffixBytes.size)
    }

    override fun splitAtExclusive(value: ByteArray, needle: String): Pair<ByteArray, ByteArray>? {
        val needleBytes = needle.encodeToByteArray()
        val idx = findBytes(value, needleBytes) ?: return null
        return value.copyOfRange(0, idx) to value.copyOfRange(idx + needleBytes.size, value.size)
    }

    override fun find(value: ByteArray, needle: String): Int? =
        findBytes(value, needle.encodeToByteArray())

    override fun splitAt(value: ByteArray, mid: Int): Pair<ByteArray, ByteArray> =
        value.copyOfRange(0, mid) to value.copyOfRange(mid, value.size)

    override fun asStr(value: ByteArray): String? =
        runCatching { value.decodeToString(throwOnInvalidSequence = true) }.getOrNull()

    override fun asBytes(value: ByteArray): ByteArray = value

    override fun lines(value: ByteArray): LineIter<ByteArray> = LineIter(this, value)

    override fun equalityKey(value: ByteArray): Any = ByteArrayKey(value)
}

private class ByteArrayKey(private val bytes: ByteArray) {
    private val cachedHash = bytes.contentHashCode()
    override fun hashCode(): Int = cachedHash
    override fun equals(other: Any?): Boolean =
        other is ByteArrayKey && bytes.contentEquals(other.bytes)
}

private fun findBytes(haystack: ByteArray, needle: ByteArray): Int? {
    when {
        needle.isEmpty() -> return 0
        needle.size == 1 -> return findByte(haystack, needle[0])
        needle.size > haystack.size -> return null
    }
    var offset = 0
    var view = haystack
    while (true) {
        val position = findByte(view, needle[0]) ?: return null
        offset += position

        val end = position + needle.size
        if (end > view.size) return null
        if (view.copyOfRange(position, end).contentEquals(needle)) return offset

        view = view.copyOfRange(position + 1, view.size)
        offset += 1
    }
}

// XXX Maybe use a Boyer-Moore / two-way style scanner?
private fun findByte(haystack: ByteArray, byte: Byte): Int? {
    for (i in haystack.indices) {
        if (haystack[i] == byte) return i
    }
    return null
}

/**
 * Returns `true` if a byte must be quoted in a diff filename.
 *
 * Matches git's behavior with all control characters
 * (0x00-0x1f), DEL (0x7f), double quote, and backslash.
 */
internal fun byteNeedsQuoting(b: Byte): Boolean {
    val unsigned = b.toInt() and 0xFF
    return unsigned < 0x20 || unsigned == 0x7f || b == '"'.code.toByte() || b == '\\'.code.toByte()
}

/**
 * Writes one byte in its escaped form to a [StringBuilder] or [Appendable].
 *
 * Named escapes are used where git defines them (`\a`, `\b`,
 * `\t`, `\n`, `\v`, `\f`, `\r`, `\\`, `\"`). Other bytes that
 * require quoting are emitted as 3-digit octal (`\NNN`).
 * Non-special bytes are written through unchanged.
 */
internal fun fmtEscapedByte(out: Appendable, b: Byte) {
    when (b.toInt()) {
        0x07 -> out.append("\\a")
        0x08 -> out.append("\\b")
        '\t'.code -> out.append("\\t")
        '\n'.code -> out.append("\\n")
        0x0b -> out.append("\\v")
        0x0c -> out.append("\\f")
        '\r'.code -> out.append("\\r")
        '"'.code -> out.append("\\\"")
        '\\'.code -> out.append("\\\\")
        else -> {
            val unsigned = b.toInt() and 0xFF
            if (unsigned < 0x20 || unsigned == 0x7f) {
                out.append("\\")
                out.append(('0'.code + (unsigned shr 6)).toChar())
                out.append(('0'.code + ((unsigned shr 3) and 7)).toChar())
                out.append(('0'.code + (unsigned and 7)).toChar())
            } else {
                out.append(b.toInt().toChar())
            }
        }
    }
}

/**
 * Decodes escape sequences in a quoted filename.
 *
 * See [byteNeedsQuoting] for the set of characters that
 * require quoting.
 */
internal fun escapedFilenameStr(filename: String): Result<String> {
    val withQuotesStripped = if (filename.startsWith("\"") && filename.endsWith("\"")) {
        filename.substring(1, filename.length - 1)
    } else null

    if (withQuotesStripped != null) {
        // Quoted filename: decode escape sequences
        val bytes = withQuotesStripped.encodeToByteArray()
        val decodedBytes = decodeEscaped(bytes)
        return when (decodedBytes) {
            null -> {
                // No escapes found, use the inner content directly
                Result.success(withQuotesStripped)
            }
            else -> {
                // Escapes were decoded; convert bytes back to String
                runCatching {
                    Result.success(decodedBytes.decodeToString(throwOnInvalidSequence = true))
                }.getOrElse {
                    Result.failure(
                        io.github.kotlinmania.diffy.patch.ParsePatchError.new(
                            io.github.kotlinmania.diffy.patch.ParsePatchErrorKind.InvalidUtf8Path
                        )
                    )
                }
            }
        }
    } else {
        // Unquoted filename: ensure it contains no characters that require quoting
        val bytes = filename.encodeToByteArray()
        if (bytes.any { byteNeedsQuoting(it) }) {
            return Result.failure(
                io.github.kotlinmania.diffy.patch.ParsePatchError.new(
                    io.github.kotlinmania.diffy.patch.ParsePatchErrorKind.InvalidCharInUnquotedFilename
                )
            )
        }
        return Result.success(filename)
    }
}

internal fun escapedFilenameBytes(filename: ByteArray): Result<ByteArray> {
    // Check if starts with " and ends with "
    val withQuotesStripped = if (filename.size >= 2 &&
        filename.first() == '"'.code.toByte() &&
        filename.last() == '"'.code.toByte()
    ) {
        filename.copyOfRange(1, filename.size - 1)
    } else null

    if (withQuotesStripped != null) {
        // Quoted filename: decode escape sequences
        val decodedBytes = decodeEscaped(withQuotesStripped)
        return when (decodedBytes) {
            null -> {
                // No escapes found, use the inner content directly
                Result.success(withQuotesStripped)
            }
            else -> {
                // Escapes were decoded
                Result.success(decodedBytes)
            }
        }
    } else {
        // Unquoted filename: ensure it contains no characters that require quoting
        if (filename.any { byteNeedsQuoting(it) }) {
            return Result.failure(
                io.github.kotlinmania.diffy.patch.ParsePatchError.new(
                    io.github.kotlinmania.diffy.patch.ParsePatchErrorKind.InvalidCharInUnquotedFilename
                )
            )
        }
        return Result.success(filename)
    }
}

/**
 * Decodes escape sequences in a byte array.
 *
 * Returns `null` if no escapes were found (the input is clean).
 * Returns the decoded bytes if escapes were processed.
 * Throws ParsePatchError if escape sequences are invalid.
 */
private fun decodeEscaped(bytes: ByteArray): ByteArray? {
    val result = mutableListOf<Byte>()
    var i = 0
    var lastCopy = 0
    var needsAllocation = false

    while (i < bytes.size) {
        if (bytes[i] == '\\'.code.toByte()) {
            needsAllocation = true
            result.addAll(bytes.copyOfRange(lastCopy, i).toList())

            i += 1
            if (i >= bytes.size) {
                throw io.github.kotlinmania.diffy.patch.ParsePatchError.new(
                    io.github.kotlinmania.diffy.patch.ParsePatchErrorKind.ExpectedEscapedChar
                )
            }

            val decoded = when (bytes[i]) {
                'a'.code.toByte() -> 0x07.toByte()
                'b'.code.toByte() -> 0x08.toByte()
                'n'.code.toByte() -> '\n'.code.toByte()
                't'.code.toByte() -> '\t'.code.toByte()
                'v'.code.toByte() -> 0x0b.toByte()
                'f'.code.toByte() -> 0x0c.toByte()
                'r'.code.toByte() -> '\r'.code.toByte()
                '"'.code.toByte() -> '"'.code.toByte()
                '\\'.code.toByte() -> '\\'.code.toByte()
                in '0'.code.toByte()..'3'.code.toByte() -> {
                    // 3-digit octal: \0xx through \3xx (values 0x00–0xFF)
                    val c = bytes[i]
                    if (i + 2 >= bytes.size) {
                        throw io.github.kotlinmania.diffy.patch.ParsePatchError.new(
                            io.github.kotlinmania.diffy.patch.ParsePatchErrorKind.InvalidEscapedChar
                        )
                    }
                    val d1 = bytes[i + 1]
                    val d2 = bytes[i + 2]
                    if (d1 !in '0'.code.toByte()..'7'.code.toByte() ||
                        d2 !in '0'.code.toByte()..'7'.code.toByte()
                    ) {
                        throw io.github.kotlinmania.diffy.patch.ParsePatchError.new(
                            io.github.kotlinmania.diffy.patch.ParsePatchErrorKind.InvalidEscapedChar
                        )
                    }
                    i += 2
                    val value = ((c - '0'.code.toByte()) shl 6) or
                            ((d1 - '0'.code.toByte()) shl 3) or
                            (d2 - '0'.code.toByte())
                    value.toByte()
                }
                else -> throw io.github.kotlinmania.diffy.patch.ParsePatchError.new(
                    io.github.kotlinmania.diffy.patch.ParsePatchErrorKind.InvalidEscapedChar
                )
            }
            result.add(decoded)
            i += 1
            lastCopy = i
        } else if (byteNeedsQuoting(bytes[i])) {
            throw io.github.kotlinmania.diffy.patch.ParsePatchError.new(
                io.github.kotlinmania.diffy.patch.ParsePatchErrorKind.InvalidUnescapedChar
            )
        } else {
            i += 1
        }
    }

    return if (needsAllocation) {
        result.addAll(bytes.copyOfRange(lastCopy, bytes.size).toList())
        result.toByteArray()
    } else {
        null
    }
}
