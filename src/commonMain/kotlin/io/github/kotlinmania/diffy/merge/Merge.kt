// port-lint: source merge/mod.rs
package io.github.kotlinmania.diffy.merge

import io.github.kotlinmania.diffy.ByteTextLike
import io.github.kotlinmania.diffy.Classifier
import io.github.kotlinmania.diffy.DiffRange
import io.github.kotlinmania.diffy.Range
import io.github.kotlinmania.diffy.RangeTo
import io.github.kotlinmania.diffy.StrTextLike
import io.github.kotlinmania.diffy.diff.DiffOptions
import kotlin.math.min

private const val DEFAULT_CONFLICT_MARKER_LENGTH: Int = 7

/**
 * An exception thrown when a merge has conflicts.
 */
class MergeConflictException(
    val conflictOutput: String,
) : Exception(conflictOutput)

/**
 * An exception thrown when a byte merge has conflicts.
 */
class MergeConflictBytesException(
    val conflictOutput: ByteArray,
) : Exception("Merge conflict in bytes")

private sealed class Diff3Range<T> {
    data class Equal<T>(
        val ancestor: Range<T>,
        val ours: Range<T>,
        val theirs: Range<T>,
    ) : Diff3Range<T>()

    data class Ancestor<T>(
        val ancestor: Range<T>,
    ) : Diff3Range<T>()

    data class AncestorOurs<T>(
        val ancestor: Range<T>,
        val ours: Range<T>,
    ) : Diff3Range<T>()

    data class AncestorTheirs<T>(
        val ancestor: Range<T>,
        val theirs: Range<T>,
    ) : Diff3Range<T>()

    data class Ours<T>(
        val ours: Range<T>,
    ) : Diff3Range<T>()

    data class Theirs<T>(
        val theirs: Range<T>,
    ) : Diff3Range<T>()
}

private sealed class MergeRange<T> {
    data class Equal<T>(
        val ancestor: Range<T>,
        val ours: Range<T>,
        val theirs: Range<T>,
    ) : MergeRange<T>()

    data class Conflict<T>(
        val ancestor: Range<T>,
        val ours: Range<T>,
        val theirs: Range<T>,
    ) : MergeRange<T>()

    data class Ours<T>(
        val ours: Range<T>,
    ) : MergeRange<T>()

    data class Theirs<T>(
        val theirs: Range<T>,
    ) : MergeRange<T>()

    data class Both<T>(
        val ours: Range<T>,
        val theirs: Range<T>,
    ) : MergeRange<T>()
}

/**
 * Style used when rendering a conflict.
 */
enum class ConflictStyle {
    /**
     * Renders conflicting lines from both files, separated by conflict markers.
     */
    Merge,

    /**
     * Renders conflicting lines from both files including lines from the original files,
     * separated by conflict markers.
     */
    Diff3,
}

/**
 * A collection of options for modifying the way a merge is performed.
 */
class MergeOptions(
    private var conflictMarkerLength: Int = DEFAULT_CONFLICT_MARKER_LENGTH,
    private var style: ConflictStyle = ConflictStyle.Diff3,
) {
    /**
     * Set the length of the conflict markers used when displaying a merge conflict.
     */
    fun setConflictMarkerLength(conflictMarkerLength: Int): MergeOptions =
        apply {
            this.conflictMarkerLength = conflictMarkerLength
        }

    /**
     * Set the conflict style used when displaying a merge conflict.
     */
    fun setConflictStyle(style: ConflictStyle): MergeOptions =
        apply {
            this.style = style
        }

    /**
     * Merge two files, given a common ancestor, based on the configured options.
     */
    fun merge(
        ancestor: String,
        ours: String,
        theirs: String,
    ): Result<String> {
        val classifier = Classifier(StrTextLike)
        val (ancestorLines, ancestorIds) = classifier.classifyLines(ancestor)
        val (ourLines, ourIds) = classifier.classifyLines(ours)
        val (theirLines, theirIds) = classifier.classifyLines(theirs)

        val opts = DiffOptions()
        val ourSolution = opts.diffSlice(ancestorIds, ourIds)
        val theirSolution = opts.diffSlice(ancestorIds, theirIds)

        val merged = mergeSolutions(ourSolution, theirSolution)
        val mergeRanges = diff3RangeToMergeRange(merged)

        cleanupConflicts(mergeRanges)

        return outputResult(
            ancestor = ancestorLines,
            ours = ourLines,
            theirs = theirLines,
            merge = mergeRanges,
            markerLen = conflictMarkerLength,
            style = style,
        )
    }

    /**
     * Perform a 3-way merge between potentially non-utf8 texts.
     */
    fun mergeBytes(
        ancestor: ByteArray,
        ours: ByteArray,
        theirs: ByteArray,
    ): Result<ByteArray> {
        val classifier = Classifier(ByteTextLike)
        val (ancestorLines, ancestorIds) = classifier.classifyLines(ancestor)
        val (ourLines, ourIds) = classifier.classifyLines(ours)
        val (theirLines, theirIds) = classifier.classifyLines(theirs)

        val opts = DiffOptions()
        val ourSolution = opts.diffSlice(ancestorIds, ourIds)
        val theirSolution = opts.diffSlice(ancestorIds, theirIds)

        val merged = mergeSolutions(ourSolution, theirSolution)
        val mergeRanges = diff3RangeToMergeRange(merged)

        cleanupConflicts(mergeRanges)

        return outputResultBytes(
            ancestor = ancestorLines,
            ours = ourLines,
            theirs = theirLines,
            merge = mergeRanges,
            markerLen = conflictMarkerLength,
            style = style,
        )
    }

    companion object {
        fun new(): MergeOptions = MergeOptions()
        fun default(): MergeOptions = MergeOptions()
    }
}

/**
 * Merge two files given a common ancestor.
 */
fun merge(
    ancestor: String,
    ours: String,
    theirs: String,
): Result<String> = MergeOptions.default().merge(ancestor, ours, theirs)

/**
 * Perform a 3-way merge between potentially non-utf8 texts.
 */
fun mergeBytes(
    ancestor: ByteArray,
    ours: ByteArray,
    theirs: ByteArray,
): Result<ByteArray> = MergeOptions.default().mergeBytes(ancestor, ours, theirs)

private fun <T> mergeSolutions(
    ourSolution: List<DiffRange<List<T>>>,
    theirSolution: List<DiffRange<List<T>>>,
): List<Diff3Range<List<T>>> {
    val ourIter = ourSolution.iterator()
    val theirIter = theirSolution.iterator()

    var ours: DiffRange<List<T>>? = if (ourIter.hasNext()) ourIter.next().copy() else null
    var theirs: DiffRange<List<T>>? = if (theirIter.hasNext()) theirIter.next().copy() else null

    val solution = mutableListOf<Diff3Range<List<T>>>()

    while (ours != null || theirs != null) {
        val currOurs = ours
        val currTheirs = theirs

        val mergeRange: Diff3Range<List<T>> =
            when {
                currOurs is DiffRange.Insert -> {
                    ours = null
                    Diff3Range.Ours(currOurs.range)
                }
                currTheirs is DiffRange.Insert -> {
                    theirs = null
                    Diff3Range.Theirs(currTheirs.range)
                }
                currOurs is DiffRange.Equal && currTheirs is DiffRange.Equal -> {
                    require(currOurs.left.offset == currTheirs.left.offset)
                    val len = min(currOurs.left.len, currTheirs.left.len)
                    val ancestorSlice = currOurs.left.slice(RangeTo(len))
                    val ourSlice = currOurs.right.slice(RangeTo(len))
                    val theirSlice = currTheirs.right.slice(RangeTo(len))

                    currOurs.shrinkFront(len)
                    currTheirs.shrinkFront(len)

                    Diff3Range.Equal(ancestorSlice, ourSlice, theirSlice)
                }
                currOurs is DiffRange.Equal && currTheirs is DiffRange.Delete -> {
                    require(currOurs.left.offset == currTheirs.range.offset)
                    val len = min(currOurs.left.len, currTheirs.range.len)
                    val ancestorSlice = currOurs.left.slice(RangeTo(len))
                    val ourSlice = currOurs.right.slice(RangeTo(len))

                    currOurs.shrinkFront(len)
                    currTheirs.shrinkFront(len)

                    Diff3Range.AncestorOurs(ancestorSlice, ourSlice)
                }
                currOurs is DiffRange.Delete && currTheirs is DiffRange.Equal -> {
                    require(currOurs.range.offset == currTheirs.left.offset)
                    val len = min(currOurs.range.len, currTheirs.left.len)
                    val ancestorSlice = currTheirs.left.slice(RangeTo(len))
                    val theirSlice = currTheirs.right.slice(RangeTo(len))

                    currOurs.shrinkFront(len)
                    currTheirs.shrinkFront(len)

                    Diff3Range.AncestorTheirs(ancestorSlice, theirSlice)
                }
                currOurs is DiffRange.Delete && currTheirs is DiffRange.Delete -> {
                    require(currOurs.range.offset == currTheirs.range.offset)
                    val len = min(currOurs.range.len, currTheirs.range.len)
                    val ancestorSlice = currOurs.range.slice(RangeTo(len))

                    currOurs.shrinkFront(len)
                    currTheirs.shrinkFront(len)

                    Diff3Range.Ancestor(ancestorSlice)
                }
                else -> throw IllegalStateException("Equal/Delete should match up: ours=$currOurs, theirs=$currTheirs")
            }

        solution.add(mergeRange)

        if (ours == null || ours.isEmpty()) {
            ours = if (ourIter.hasNext()) ourIter.next().copy() else null
        }
        if (theirs == null || theirs.isEmpty()) {
            theirs = if (theirIter.hasNext()) theirIter.next().copy() else null
        }
    }

    return solution
}

private fun <T> diff3RangeToMergeRange(
    solution: List<Diff3Range<List<T>>>,
): MutableList<MergeRange<List<T>>> {
    var ancestor: Range<List<T>>? = null
    var ours: Range<List<T>>? = null
    var theirs: Range<List<T>>? = null

    val merge = mutableListOf<MergeRange<List<T>>>()

    for (diff3 in solution) {
        when (diff3) {
            is Diff3Range.Equal -> {
                createMergeRange(ancestor, ours, theirs)?.let { merge.add(it) }
                ancestor = null
                ours = null
                theirs = null
                merge.add(MergeRange.Equal(diff3.ancestor, diff3.ours, diff3.theirs))
            }
            is Diff3Range.Ancestor -> {
                ancestor = setOrMergeRange(ancestor, diff3.ancestor)
                ours = setOrMergeRange(ours, Range.empty(diff3.ancestor.sliceLike))
                theirs = setOrMergeRange(theirs, Range.empty(diff3.ancestor.sliceLike))
            }
            is Diff3Range.AncestorOurs -> {
                ancestor = setOrMergeRange(ancestor, diff3.ancestor)
                ours = setOrMergeRange(ours, diff3.ours)
            }
            is Diff3Range.AncestorTheirs -> {
                ancestor = setOrMergeRange(ancestor, diff3.ancestor)
                theirs = setOrMergeRange(theirs, diff3.theirs)
            }
            is Diff3Range.Ours -> {
                ours = setOrMergeRange(ours, diff3.ours)
            }
            is Diff3Range.Theirs -> {
                theirs = setOrMergeRange(theirs, diff3.theirs)
            }
        }
    }

    createMergeRange(ancestor, ours, theirs)?.let { merge.add(it) }

    return merge
}

private fun <T> setOrMergeRange(
    range1: Range<T>?,
    range2: Range<T>,
): Range<T> {
    if (range1 != null) {
        if (range1.isEmpty()) {
            return range2.copy()
        } else if (!range2.isEmpty()) {
            require(range1.offset + range1.len == range2.offset)
            range1.growDown(range2.len)
            return range1
        }
        return range1
    } else {
        return range2.copy()
    }
}

private fun <T> createMergeRange(
    ancestor: Range<T>?,
    ours: Range<T>?,
    theirs: Range<T>?,
): MergeRange<T>? =
    when {
        ancestor != null && ours != null && theirs != null ->
            MergeRange.Conflict(ancestor, ours, theirs)
        ancestor == null && ours != null && theirs != null ->
            MergeRange.Conflict(Range.empty(ours.sliceLike), ours, theirs)
        ancestor == null && ours != null && theirs == null ->
            MergeRange.Ours(ours)
        ancestor == null && ours == null && theirs != null ->
            MergeRange.Theirs(theirs)
        ancestor != null && ours == null && theirs != null ->
            MergeRange.Conflict(ancestor, Range.empty(theirs.sliceLike), theirs)
        ancestor != null && ours != null && theirs == null ->
            MergeRange.Conflict(ancestor, ours, Range.empty(ours.sliceLike))
        else -> null
    }

private fun <T> cleanupConflicts(
    solution: MutableList<MergeRange<List<T>>>,
) {
    for (i in solution.indices) {
        val merge = solution[i]
        if (merge is MergeRange.Conflict) {
            val ancestorSlice = merge.ancestor.asSlice()
            val ourSlice = merge.ours.asSlice()
            val theirSlice = merge.theirs.asSlice()

            if (ourSlice == theirSlice) {
                solution[i] = MergeRange.Both(merge.ours, merge.theirs)
            } else if (ancestorSlice == ourSlice) {
                solution[i] = MergeRange.Theirs(merge.theirs)
            } else if (ancestorSlice == theirSlice) {
                solution[i] = MergeRange.Ours(merge.ours)
            }
        }
    }
}

private fun outputResult(
    ancestor: List<String>,
    ours: List<String>,
    theirs: List<String>,
    merge: List<MergeRange<List<Long>>>,
    markerLen: Int,
    style: ConflictStyle,
): Result<String> {
    var conflicts = 0
    val output = StringBuilder()

    for (mergeRange in merge) {
        when (mergeRange) {
            is MergeRange.Equal -> {
                for (i in mergeRange.ancestor.range()) {
                    output.append(ancestor[i])
                }
            }
            is MergeRange.Conflict -> {
                addConflictMarker(output, '<', markerLen, "ours")
                for (i in mergeRange.ours.range()) {
                    output.append(ours[i])
                }

                if (style == ConflictStyle.Diff3) {
                    addConflictMarker(output, '|', markerLen, "original")
                    for (i in mergeRange.ancestor.range()) {
                        output.append(ancestor[i])
                    }
                }

                addConflictMarker(output, '=', markerLen, null)
                for (i in mergeRange.theirs.range()) {
                    output.append(theirs[i])
                }
                addConflictMarker(output, '>', markerLen, "theirs")
                conflicts += 1
            }
            is MergeRange.Ours -> {
                for (i in mergeRange.ours.range()) {
                    output.append(ours[i])
                }
            }
            is MergeRange.Theirs -> {
                for (i in mergeRange.theirs.range()) {
                    output.append(theirs[i])
                }
            }
            is MergeRange.Both -> {
                for (i in mergeRange.ours.range()) {
                    output.append(ours[i])
                }
            }
        }
    }

    return if (conflicts != 0) {
        Result.failure(MergeConflictException(output.toString()))
    } else {
        Result.success(output.toString())
    }
}

private fun addConflictMarker(
    output: StringBuilder,
    marker: Char,
    markerLen: Int,
    filename: String?,
) {
    for (i in 0 until markerLen) {
        output.append(marker)
    }
    if (filename != null) {
        output.append(' ')
        output.append(filename)
    }
    output.append('\n')
}

private fun outputResultBytes(
    ancestor: List<ByteArray>,
    ours: List<ByteArray>,
    theirs: List<ByteArray>,
    merge: List<MergeRange<List<Long>>>,
    markerLen: Int,
    style: ConflictStyle,
): Result<ByteArray> {
    var conflicts = 0
    val byteList = mutableListOf<ByteArray>()

    for (mergeRange in merge) {
        when (mergeRange) {
            is MergeRange.Equal -> {
                for (i in mergeRange.ancestor.range()) {
                    byteList.add(ancestor[i])
                }
            }
            is MergeRange.Conflict -> {
                addConflictMarkerBytes(byteList, '<'.code.toByte(), markerLen, "ours".encodeToByteArray())
                for (i in mergeRange.ours.range()) {
                    byteList.add(ours[i])
                }

                if (style == ConflictStyle.Diff3) {
                    addConflictMarkerBytes(byteList, '|'.code.toByte(), markerLen, "original".encodeToByteArray())
                    for (i in mergeRange.ancestor.range()) {
                        byteList.add(ancestor[i])
                    }
                }

                addConflictMarkerBytes(byteList, '='.code.toByte(), markerLen, null)
                for (i in mergeRange.theirs.range()) {
                    byteList.add(theirs[i])
                }
                addConflictMarkerBytes(byteList, '>'.code.toByte(), markerLen, "theirs".encodeToByteArray())
                conflicts += 1
            }
            is MergeRange.Ours -> {
                for (i in mergeRange.ours.range()) {
                    byteList.add(ours[i])
                }
            }
            is MergeRange.Theirs -> {
                for (i in mergeRange.theirs.range()) {
                    byteList.add(theirs[i])
                }
            }
            is MergeRange.Both -> {
                for (i in mergeRange.ours.range()) {
                    byteList.add(ours[i])
                }
            }
        }
    }

    val totalSize = byteList.sumOf { it.size }
    val outBytes = ByteArray(totalSize)
    var offset = 0
    for (slice in byteList) {
        slice.copyInto(outBytes, offset)
        offset += slice.size
    }

    return if (conflicts != 0) {
        Result.failure(MergeConflictBytesException(outBytes))
    } else {
        Result.success(outBytes)
    }
}

private fun addConflictMarkerBytes(
    byteList: MutableList<ByteArray>,
    marker: Byte,
    markerLen: Int,
    filename: ByteArray?,
) {
    val markerBytes = ByteArray(markerLen) { marker }
    byteList.add(markerBytes)
    if (filename != null) {
        byteList.add(byteArrayOf(' '.code.toByte()))
        byteList.add(filename)
    }
    byteList.add(byteArrayOf('\n'.code.toByte()))
}
