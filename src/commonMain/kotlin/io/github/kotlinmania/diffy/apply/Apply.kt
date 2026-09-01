// port-lint: source apply.rs
package io.github.kotlinmania.diffy.apply

import io.github.kotlinmania.diffy.ByteTextLike
import io.github.kotlinmania.diffy.LineIter
import io.github.kotlinmania.diffy.StrTextLike
import io.github.kotlinmania.diffy.patch.Hunk
import io.github.kotlinmania.diffy.patch.Line
import io.github.kotlinmania.diffy.patch.Patch
import kotlin.math.min

/**
 * An error returned when applying a [Patch] fails.
 */
class ApplyError(
    val hunkIndex: Int,
) : Exception("error applying hunk #$hunkIndex") {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as ApplyError
        return hunkIndex == other.hunkIndex
    }

    override fun hashCode(): Int = hunkIndex.hashCode()
}

private sealed class ImageLine<T> {
    abstract val inner: T
    abstract val isPatched: Boolean

    data class Unpatched<T>(
        override val inner: T,
    ) : ImageLine<T>() {
        override val isPatched: Boolean get() = false
    }

    data class Patched<T>(
        override val inner: T,
    ) : ImageLine<T>() {
        override val isPatched: Boolean get() = true
    }
}

/**
 * Apply a [Patch] to a base image.
 */
fun apply(baseImage: String, patch: Patch<String>): Result<String> {
    val image =
        LineIter(StrTextLike, baseImage)
            .asSequence()
            .map { ImageLine.Unpatched(it) }
            .toMutableList<ImageLine<String>>()

    for ((i, hunk) in patch.hunks().withIndex()) {
        if (!applyHunk(image, hunk)) {
            return Result.failure(ApplyError(i + 1))
        }
    }

    val result = image.joinToString("") { it.inner }
    return Result.success(result)
}

/**
 * Apply a non-utf8 [Patch] to a base image.
 */
fun applyBytes(baseImage: ByteArray, patch: Patch<ByteArray>): Result<ByteArray> {
    val image =
        LineIter(ByteTextLike, baseImage)
            .asSequence()
            .map { ImageLine.Unpatched(it) }
            .toMutableList<ImageLine<ByteArray>>()

    for ((i, hunk) in patch.hunks().withIndex()) {
        if (!applyHunkBytes(image, hunk)) {
            return Result.failure(ApplyError(i + 1))
        }
    }

    val totalBytes = image.sumOf { it.inner.size }
    val result = ByteArray(totalBytes)
    var offset = 0
    for (line in image) {
        line.inner.copyInto(result, offset)
        offset += line.inner.size
    }
    return Result.success(result)
}

private fun applyHunk(
    image: MutableList<ImageLine<String>>,
    hunk: Hunk<String>,
): Boolean {
    val pos = findPosition(image, hunk) ?: return false
    val preCount = preImageLineCount(hunk.lines())

    val replacement =
        postImage(hunk.lines())
            .map { ImageLine.Patched(it) }

    for (k in 0 until preCount) {
        image.removeAt(pos)
    }
    image.addAll(pos, replacement)
    return true
}

private fun applyHunkBytes(
    image: MutableList<ImageLine<ByteArray>>,
    hunk: Hunk<ByteArray>,
): Boolean {
    val pos = findPositionBytes(image, hunk) ?: return false
    val preCount = preImageLineCount(hunk.lines())

    val replacement =
        postImage(hunk.lines())
            .map { ImageLine.Patched(it) }

    for (k in 0 until preCount) {
        image.removeAt(pos)
    }
    image.addAll(pos, replacement)
    return true
}

private fun findPosition(
    image: List<ImageLine<String>>,
    hunk: Hunk<String>,
): Int? {
    val pos = min((hunk.newRange().start() - 1).coerceAtLeast(0), image.size)

    val backward = (pos - 1 downTo 0).toList()
    val forward = (pos + 1 until image.size).toList()

    val sequence = sequenceOf(pos) + interleave(backward, forward)
    return sequence.firstOrNull { p -> matchFragment(image, hunk.lines(), p) }
}

private fun findPositionBytes(
    image: List<ImageLine<ByteArray>>,
    hunk: Hunk<ByteArray>,
): Int? {
    val pos = min((hunk.newRange().start() - 1).coerceAtLeast(0), image.size)

    val backward = (pos - 1 downTo 0).toList()
    val forward = (pos + 1 until image.size).toList()

    val sequence = sequenceOf(pos) + interleave(backward, forward)
    return sequence.firstOrNull { p -> matchFragmentBytes(image, hunk.lines(), p) }
}

private fun <T> preImageLineCount(lines: List<Line<T>>): Int =
    lines.count { it is Line.Context || it is Line.Delete }

private fun <T> postImage(lines: List<Line<T>>): List<T> =
    lines.mapNotNull { line ->
        when (line) {
            is Line.Context -> line.value
            is Line.Insert -> line.value
            is Line.Delete -> null
        }
    }

private fun <T> preImage(lines: List<Line<T>>): List<T> =
    lines.mapNotNull { line ->
        when (line) {
            is Line.Context -> line.value
            is Line.Delete -> line.value
            is Line.Insert -> null
        }
    }

private fun matchFragment(
    image: List<ImageLine<String>>,
    lines: List<Line<String>>,
    pos: Int,
): Boolean {
    val expected = preImage(lines)
    val len = expected.size

    if (pos + len > image.size) return false

    for (i in 0 until len) {
        val item = image[pos + i]
        if (item.isPatched) return false
        if (item.inner != expected[i]) return false
    }
    return true
}

private fun matchFragmentBytes(
    image: List<ImageLine<ByteArray>>,
    lines: List<Line<ByteArray>>,
    pos: Int,
): Boolean {
    val expected = preImage(lines)
    val len = expected.size

    if (pos + len > image.size) return false

    for (i in 0 until len) {
        val item = image[pos + i]
        if (item.isPatched) return false
        if (!item.inner.contentEquals(expected[i])) return false
    }
    return true
}

private fun <T> interleave(a: List<T>, b: List<T>): Sequence<T> =
    sequence {
        var i = 0
        var j = 0
        var takeA = true
        while (i < a.size || j < b.size) {
            if (takeA) {
                if (i < a.size) {
                    yield(a[i++])
                } else if (j < b.size) {
                    yield(b[j++])
                }
            } else {
                if (j < b.size) {
                    yield(b[j++])
                } else if (i < a.size) {
                    yield(a[i++])
                }
            }
            takeA = !takeA
        }
    }
