// port-lint: source patch/mod.rs
package io.github.kotlinmania.diffy.patch

import io.github.kotlinmania.diffy.byteNeedsQuoting
import io.github.kotlinmania.diffy.fmtEscapedByte

internal const val NO_NEWLINE_AT_EOF: String = "\\ No newline at end of file"

/**
 * Representation of all the differences between two files
 *
 * ## Display and formatting
 *
 * Use [PatchFormatter] to format a patch for display in unified diff format.
 *
 * For parsing multi-file patches, see the patch set module.
 */
class Patch<T> private constructor(
    private val original: Filename<T>?,
    private val modified: Filename<T>?,
    private val hunks: List<Hunk<T>>,
) {
    /**
     * Return the name of the old file
     */
    fun original(): T? = original?.value

    /**
     * Return the name of the new file
     */
    fun modified(): T? = modified?.value

    /**
     * Returns the hunks in the patch
     */
    fun hunks(): List<Hunk<T>> = hunks

    /**
     * Returns a patch that transforms the modified text back into the original text.
     */
    fun reverse(): Patch<T> {
        val reversedHunks = hunks.map { it.reverse() }
        return Patch(
            original = modified,
            modified = original,
            hunks = reversedHunks,
        )
    }

    /**
     * Convert a `Patch` into bytes
     *
     * This is the equivalent of the `toString` function but for
     * potentially non-utf8 patches.
     */
    fun toBytes(): ByteArray {
        val origVal = original?.value
        val modVal = modified?.value
        val lineVal =
            hunks.firstOrNull()?.lines()?.firstOrNull()?.let {
                when (it) {
                    is Line.Context -> it.value
                    is Line.Delete -> it.value
                    is Line.Insert -> it.value
                }
            }
        val isBytes = origVal is ByteArray || modVal is ByteArray || lineVal is ByteArray
        return if (isBytes) {
            @Suppress("UNCHECKED_CAST")
            PatchFormatter.new().fmtPatchBytes(this as Patch<ByteArray>)
        } else {
            toString().encodeToByteArray()
        }
    }

    internal fun originalPath(): Filename<T>? = original

    internal fun modifiedPath(): Filename<T>? = modified

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as Patch<*>
        if (original != other.original) return false
        if (modified != other.modified) return false
        if (hunks != other.hunks) return false
        return true
    }

    override fun hashCode(): Int {
        var result = original.hashCode()
        result = 31 * result + modified.hashCode()
        result = 31 * result + hunks.hashCode()
        return result
    }

    override fun toString(): String {
        val origVal = original?.value
        val modVal = modified?.value
        val lineVal =
            hunks.firstOrNull()?.lines()?.firstOrNull()?.let {
                when (it) {
                    is Line.Context -> it.value
                    is Line.Delete -> it.value
                    is Line.Insert -> it.value
                }
            }
        val isBytes = origVal is ByteArray || modVal is ByteArray || lineVal is ByteArray
        return if (isBytes) {
            @Suppress("UNCHECKED_CAST")
            PatchFormatter.new().fmtPatchBytes(this as Patch<ByteArray>).decodeToString()
        } else {
            @Suppress("UNCHECKED_CAST")
            PatchFormatter.new().fmtPatch(this as Patch<String>)
        }
    }

    companion object {
        internal fun <T> new(
            original: T?,
            modified: T?,
            hunks: List<Hunk<T>>,
        ): Patch<T> =
            Patch(
                original = original?.let { Filename(it) },
                modified = modified?.let { Filename(it) },
                hunks = hunks,
            )

        /**
         * Parse a [Patch] from a string
         *
         * ```
         * val s = """
         * --- a/ideals
         * +++ b/ideals
         * @@ -1,4 +1,6 @@
         *  First:
         *      Life before death,
         *      strength before weakness,
         *      journey before destination.
         * +Second:
         * +    I will protect those who cannot protect themselves.
         * """
         *
         * val patch = Patch.fromStr(s).getOrThrow()
         * ```
         *
         * ## Parsing behavior
         *
         * [fromStr] and [fromBytes] follow `git apply` behavior:
         * trailing non-patch content after a complete hunk is ignored,
         * but orphaned hunk headers hidden behind trailing content are rejected.
         *
         * For parsing multi-file patches, see the patch set module.
         */
        fun fromStr(s: String): Result<Patch<String>> = parse(s)

        /**
         * Parse a [Patch] from bytes
         */
        fun fromBytes(bytes: ByteArray): Result<Patch<ByteArray>> = parseBytes(bytes)
    }
}

internal class Filename<T>(
    val value: T,
) {
    fun needsToBeEscapedBytes(bytes: ByteArray): Boolean =
        bytes.any { byteNeedsQuoting(it) }

    fun writeInto(out: Appendable, bytes: ByteArray) {
        if (needsToBeEscapedBytes(bytes)) {
            out.append("\"")
            for (b in bytes) {
                fmtEscapedByte(out, b)
            }
            out.append("\"")
        } else {
            out.append(bytes.decodeToString())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as Filename<*>
        return when {
            value is String && other.value is String -> value == other.value
            value is ByteArray && other.value is ByteArray -> value.contentEquals(other.value)
            else -> value == other.value
        }
    }

    override fun hashCode(): Int =
        when (value) {
            is ByteArray -> value.contentHashCode()
            else -> value.hashCode()
        }

    override fun toString(): String =
        when (value) {
            is String -> {
                val bytes = value.encodeToByteArray()
                if (needsToBeEscapedBytes(bytes)) {
                    buildString {
                        append('\"')
                        for (b in bytes) {
                            fmtEscapedByte(this, b)
                        }
                        append('\"')
                    }
                } else {
                    value
                }
            }
            is ByteArray -> {
                if (needsToBeEscapedBytes(value)) {
                    buildString {
                        append('\"')
                        for (b in value) {
                            fmtEscapedByte(this, b)
                        }
                        append('\"')
                    }
                } else {
                    value.decodeToString()
                }
            }
            else -> value.toString()
        }
}

/**
 * Represents a group of differing lines between two files
 */
data class Hunk<T>(
    private val oldRange: HunkRange,
    private val newRange: HunkRange,
    private val functionContext: T?,
    private val lines: List<Line<T>>,
) {
    init {
        val (oldCount, newCount) = hunkLinesCount(lines)
        check(oldRange.len == oldCount) { "oldRange.len != oldCount" }
        check(newRange.len == newCount) { "newRange.len != newCount" }
    }

    /**
     * Returns the corresponding range for the old file in the hunk
     */
    fun oldRange(): HunkRange = oldRange

    /**
     * Returns the corresponding range for the new file in the hunk
     */
    fun newRange(): HunkRange = newRange

    /**
     * Returns the function context (if any) for the hunk
     */
    fun functionContext(): T? = functionContext

    /**
     * Returns the lines in the hunk
     */
    fun lines(): List<Line<T>> = lines

    /**
     * Creates a reverse patch for the hunk. This is equivalent to what
     * XDL_PATCH_REVERSE would apply in libxdiff.
     */
    fun reverse(): Hunk<T> {
        val reversedLines = lines.map { it.reverse() }
        return Hunk(
            oldRange = newRange,
            newRange = oldRange,
            functionContext = functionContext,
            lines = reversedLines,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Hunk<*>) return false
        if (oldRange != other.oldRange) return false
        if (newRange != other.newRange) return false
        val fc1 = functionContext
        val fc2 = other.functionContext
        val fcEquals =
            when {
                fc1 is ByteArray && fc2 is ByteArray -> fc1.contentEquals(fc2)
                else -> fc1 == fc2
            }
        if (!fcEquals) return false
        if (lines != other.lines) return false
        return true
    }

    override fun hashCode(): Int {
        var result = oldRange.hashCode()
        result = 31 * result + newRange.hashCode()
        result = 31 * result + (if (functionContext is ByteArray) functionContext.contentHashCode() else (functionContext?.hashCode() ?: 0))
        result = 31 * result + lines.hashCode()
        return result
    }

    companion object {
        internal fun <T> new(
            oldRange: HunkRange,
            newRange: HunkRange,
            functionContext: T?,
            lines: List<Line<T>>,
        ): Hunk<T> = Hunk(oldRange, newRange, functionContext, lines)
    }
}

private fun <T> hunkLinesCount(lines: List<Line<T>>): Pair<Int, Int> =
    lines.fold(0 to 0) { (oldCount, newCount), line ->
        when (line) {
            is Line.Context -> (oldCount + 1) to (newCount + 1)
            is Line.Delete -> (oldCount + 1) to newCount
            is Line.Insert -> oldCount to (newCount + 1)
        }
    }

/**
 * The range of lines in a file for a particular [Hunk].
 */
data class HunkRange(
    private val start: Int,
    val len: Int,
) {
    /**
     * Returns the range as an [IntRange]
     */
    fun range(): IntRange = start until end()

    /**
     * Returns the starting line number of the range (inclusive)
     */
    fun start(): Int = start

    /**
     * Returns the ending line number of the range (exclusive)
     */
    fun end(): Int = start + len

    /**
     * Returns `true` if the range is empty (has a length of `0`)
     */
    fun isEmpty(): Boolean = len == 0

    override fun toString(): String =
        if (len != 1) {
            "$start,$len"
        } else {
            "$start"
        }

    companion object {
        internal fun new(start: Int, len: Int): HunkRange = HunkRange(start, len)
    }
}

/**
 * A line in either the old file, new file, or both.
 *
 * A [Line] contains the terminating newline character `\n` unless it is the final
 * line in the file and the file does not end with a newline character.
 */
sealed class Line<out T> {
    /**
     * A line providing context in the diff which is present in both the old and new file
     */
    class Context<T>(
        val value: T,
    ) : Line<T>() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Context<*>) return false
            val v1 = value
            val v2 = other.value
            return if (v1 is ByteArray && v2 is ByteArray) {
                v1.contentEquals(v2)
            } else {
                v1 == v2
            }
        }

        override fun hashCode(): Int =
            if (value is ByteArray) value.contentHashCode() else value.hashCode()

        override fun toString(): String = "Context(value=$value)"
    }

    /**
     * A line deleted from the old file
     */
    class Delete<T>(
        val value: T,
    ) : Line<T>() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Delete<*>) return false
            val v1 = value
            val v2 = other.value
            return if (v1 is ByteArray && v2 is ByteArray) {
                v1.contentEquals(v2)
            } else {
                v1 == v2
            }
        }

        override fun hashCode(): Int =
            if (value is ByteArray) value.contentHashCode() else value.hashCode()

        override fun toString(): String = "Delete(value=$value)"
    }

    /**
     * A line inserted to the new file
     */
    class Insert<T>(
        val value: T,
    ) : Line<T>() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Insert<*>) return false
            val v1 = value
            val v2 = other.value
            return if (v1 is ByteArray && v2 is ByteArray) {
                v1.contentEquals(v2)
            } else {
                v1 == v2
            }
        }

        override fun hashCode(): Int =
            if (value is ByteArray) value.contentHashCode() else value.hashCode()

        override fun toString(): String = "Insert(value=$value)"
    }

    /**
     * Reverses the direction of this diff line.
     *
     * * Context lines are unchanged
     * * Insertions become deletions
     * * Deletions become insertions
     */
    fun reverse(): Line<T> =
        when (this) {
            is Context -> this
            is Delete -> Insert(value)
            is Insert -> Delete(value)
        }
}
