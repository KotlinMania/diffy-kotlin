// port-lint: source src/diff/mod.rs
package io.github.kotlinmania.diffy.diff

import io.github.kotlinmania.diffy.Classifier
import io.github.kotlinmania.diffy.StrTextLike
import io.github.kotlinmania.diffy.ByteTextLike
import io.github.kotlinmania.diffy.patch.Hunk
import io.github.kotlinmania.diffy.patch.HunkRange
import io.github.kotlinmania.diffy.patch.Line
import io.github.kotlinmania.diffy.patch.Patch
import io.github.kotlinmania.diffy.DiffRange
import io.github.kotlinmania.diffy.StrSliceLike
import io.github.kotlinmania.diffy.ByteSliceLike
import io.github.kotlinmania.diffy.Range
import io.github.kotlinmania.diffy.RangeFull
import kotlin.math.min

/**
 * A collection of options for modifying the way a diff is performed
 *
 * Example:
 * ```kotlin
 * val options = DiffOptions()
 *     .setContextLen(1)
 *     .setOriginalFilename("before.txt")
 *     .setModifiedFilename("after.txt")
 *
 * val patch = options.createPatch(
 *     """
 *     alpha
 *     beta
 *     gamma
 *     """.trimIndent(),
 *     """
 *     alpha
 *     BETA
 *     gamma
 *     """.trimIndent()
 * )
 * ```
 */
class DiffOptions {
    private var compact: Boolean = true
    private var contextLen: Int = 3
    private var originalFilename: String? = "original"
    private var modifiedFilename: String? = "modified"

    /**
     * Set the number of context lines that should be used when producing a patch
     */
    fun setContextLen(contextLen: Int): DiffOptions = apply {
        this.contextLen = contextLen
    }

    /**
     * Set the filename to be used in the patch for the original text
     *
     * If not set, the default value is "original".
     */
    fun setOriginalFilename(filename: String): DiffOptions = apply {
        this.originalFilename = filename
    }

    /**
     * Set the filename to be used in the patch for the modified text
     *
     * If not set, the default value is "modified".
     */
    fun setModifiedFilename(filename: String): DiffOptions = apply {
        this.modifiedFilename = filename
    }

    /**
     * Produce a Patch between two texts based on the configured options
     */
    fun createPatch(original: String, modified: String): Patch<String> {
        val classifier = Classifier(StrTextLike)
        val (oldLines, oldIds) = classifier.classifyLines(original)
        val (newLines, newIds) = classifier.classifyLines(modified)

        val solution = diffSlice(oldIds, newIds)

        val hunks = toHunks(oldLines, newLines, solution, contextLen)
        return Patch.new(
            original = originalFilename,
            modified = modifiedFilename,
            hunks = hunks,
        )
    }

    /**
     * Create a patch between two potentially non-utf8 texts
     */
    fun createPatchBytes(original: ByteArray, modified: ByteArray): Patch<ByteArray> {
        val classifier = Classifier(ByteTextLike)
        val (oldLines, oldIds) = classifier.classifyLines(original)
        val (newLines, newIds) = classifier.classifyLines(modified)

        val solution = diffSlice(oldIds, newIds)

        val hunks = toHunks(oldLines, newLines, solution, contextLen)
        return Patch.new(
            original = originalFilename?.encodeToByteArray(),
            modified = modifiedFilename?.encodeToByteArray(),
            hunks = hunks,
        )
    }

    internal fun <T : Comparable<T>> diffSlice(old: List<T>, new: List<T>): List<DiffRange<List<T>>> {
        // Use myers diff on the comparable IDs
        // Create wrapper arrays to work with the array-based SliceLike
        val oldListWrapper = old
        val newListWrapper = new

        val oldRange = Range.new(ListSliceLike<T>(), oldListWrapper, RangeFull)
        val newRange = Range.new(ListSliceLike<T>(), newListWrapper, RangeFull)

        val solution = mutableListOf<DiffRange<List<T>>>()
        val maxD = io.github.kotlinmania.diffy.diff.maxD(old.size, new.size)
        val vf = io.github.kotlinmania.diffy.diff.V(maxD)
        val vb = io.github.kotlinmania.diffy.diff.V(maxD)

        io.github.kotlinmania.diffy.diff.conquer(oldRange, newRange, vf, vb, solution)

        if (compact) {
            compact(solution)
        }

        return solution
    }
}

// SliceLike implementation for List<T> where items are comparable
private class ListSliceLike<T> : io.github.kotlinmania.diffy.SliceLike<List<T>> {
    override fun len(value: List<T>): Int = value.size
    override fun empty(): List<T> = emptyList()
    override fun asSlice(value: List<T>, start: Int, endExclusive: Int): List<T> =
        value.subList(start, endExclusive)

    override fun commonPrefixLen(a: List<T>, b: List<T>): Int {
        val limit = min(a.size, b.size)
        var i = 0
        while (i < limit && a[i] == b[i]) i++
        return i
    }

    override fun commonSuffixLen(a: List<T>, b: List<T>): Int {
        val limit = min(a.size, b.size)
        var i = 0
        while (i < limit && a[a.size - 1 - i] == b[b.size - 1 - i]) i++
        return i
    }

    override fun commonOverlapLen(a: List<T>, b: List<T>): Int {
        var len = min(a.size, b.size)
        while (len > 0) {
            if (a.subList(0, len) == b.subList(b.size - len, b.size)) break
            len -= 1
        }
        return len
    }

    override fun startsWith(value: List<T>, prefix: List<T>): Boolean {
        if (prefix.size > value.size) return false
        for (i in prefix.indices) {
            if (value[i] != prefix[i]) return false
        }
        return true
    }

    override fun endsWith(value: List<T>, suffix: List<T>): Boolean {
        if (suffix.size > value.size) return false
        val base = value.size - suffix.size
        for (i in suffix.indices) {
            if (value[base + i] != suffix[i]) return false
        }
        return true
    }
}

/**
 * Create a patch between two texts.
 */
fun createPatch(original: String, modified: String): Patch<String> =
    DiffOptions().createPatch(original, modified)

/**
 * Create a patch between two potentially non-utf8 texts
 */
fun createPatchBytes(original: ByteArray, modified: ByteArray): Patch<ByteArray> =
    DiffOptions().createPatchBytes(original, modified)

private fun <T> toHunks(
    lines1: List<T>,
    lines2: List<T>,
    solution: List<DiffRange<*>>,
    contextLen: Int,
): List<Hunk<T>> {
    val editScript = buildEditScript(solution)

    val hunks = mutableListOf<Hunk<T>>()

    var idx = 0
    while (idx < editScript.size) {
        var script = editScript[idx]
        val start1 = (script.old.first - contextLen).coerceAtLeast(0)
        val start2 = (script.new.first - contextLen).coerceAtLeast(0)

        var (end1, end2) = calcEnd(
            contextLen,
            lines1.size,
            lines2.size,
            script.old.last + 1,
            script.new.last + 1,
        )

        val lines = mutableListOf<Line<T>>()

        // Pre-context
        for (i in start2 until script.new.first) {
            if (i < lines2.size) {
                lines.add(Line.Context(lines2[i]))
            }
        }

        while (true) {
            // Delete lines from text1
            for (i in script.old) {
                if (i < lines1.size) {
                    lines.add(Line.Delete(lines1[i]))
                }
            }

            // Insert lines from text2
            for (i in script.new) {
                if (i < lines2.size) {
                    lines.add(Line.Insert(lines2[i]))
                }
            }

            if (idx + 1 < editScript.size) {
                val s = editScript[idx + 1]
                // Check to see if we can merge the hunks
                val start1Next = min(s.old.first, lines1.size - 1).coerceAtLeast(0) - contextLen
                if (start1Next < end1) {
                    // Context lines between hunks
                    val oldEnd = script.old.last + 1
                    val newEnd = script.new.last + 1
                    for (i in newEnd until s.new.first) {
                        if (i < lines2.size) {
                            lines.add(Line.Context(lines2[i]))
                        }
                    }

                    // Calc the new end
                    val (e1, e2) = calcEnd(
                        contextLen,
                        lines1.size,
                        lines2.size,
                        s.old.last + 1,
                        s.new.last + 1,
                    )

                    end1 = e1
                    end2 = e2
                    script = s
                    idx += 1
                    continue
                }
            }

            break
        }

        // Post-context
        for (i in (script.new.last + 1) until end2) {
            if (i < lines2.size) {
                lines.add(Line.Context(lines2[i]))
            }
        }

        val len1 = end1 - start1
        val oldRange = HunkRange.new(if (len1 > 0) start1 + 1 else start1, len1)

        val len2 = end2 - start2
        val newRange = HunkRange.new(if (len2 > 0) start2 + 1 else start2, len2)

        hunks.add(Hunk.new(oldRange, newRange, null, lines))
        idx += 1
    }

    return hunks
}

private fun calcEnd(
    contextLen: Int,
    text1Len: Int,
    text2Len: Int,
    script1End: Int,
    script2End: Int,
): Pair<Int, Int> {
    val postContextLen = min(
        contextLen,
        min(
            text1Len - script1End,
            text2Len - script2End,
        ).coerceAtLeast(0),
    )

    val end1 = script1End + postContextLen
    val end2 = script2End + postContextLen

    return end1 to end2
}

private data class EditRange(
    val old: IntRange,
    val new: IntRange,
)

private fun buildEditScript(solution: List<DiffRange<*>>): List<EditRange> {
    var idxA = 0
    var idxB = 0

    val editScript = mutableListOf<EditRange>()
    var script: EditRange? = null

    for (diff in solution) {
        when (diff) {
            is DiffRange.Equal<*> -> {
                idxA += diff.left.len()
                idxB += diff.right.len()
                script?.let { editScript.add(it) }
                script = null
            }
            is DiffRange.Delete<*> -> {
                val len = diff.range.len()
                script = if (script != null) {
                    script.copy(old = script.old.first until (script.old.last + 1 + len))
                } else {
                    EditRange(idxA until (idxA + len), idxB until idxB)
                }
                idxA += len
            }
            is DiffRange.Insert<*> -> {
                val len = diff.range.len()
                script = if (script != null) {
                    script.copy(new = script.new.first until (script.new.last + 1 + len))
                } else {
                    EditRange(idxA until idxA, idxB until (idxB + len))
                }
                idxB += len
            }
        }
    }

    script?.let { editScript.add(it) }

    return editScript
}

