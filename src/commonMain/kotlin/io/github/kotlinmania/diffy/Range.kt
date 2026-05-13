// port-lint: source src/range.rs
package io.github.kotlinmania.diffy

import kotlin.math.min

// Range type inspired by the Range type used in [dissimilar](https://docs.rs/dissimilar)
class Range<T>(
    val sliceLike: SliceLike<T>,
    val inner: T,
    var offset: Int,
    var len: Int,
) {
    fun isEmpty(): Boolean = len == 0

    fun inner(): T = inner

    fun len(): Int = len

    fun offset(): Int = offset

    fun range(): IntRange = offset until offset + len

    fun growUp(adjust: Int) {
        offset -= adjust
        len += adjust
    }

    fun growDown(adjust: Int) {
        len += adjust
    }

    fun shrinkFront(adjust: Int) {
        offset += adjust
        len -= adjust
    }

    fun shrinkBack(adjust: Int) {
        len -= adjust
    }

    fun shiftUp(adjust: Int) {
        offset -= adjust
    }

    fun shiftDown(adjust: Int) {
        offset += adjust
    }

    fun slice(bounds: RangeBounds): Range<T> {
        val (boundsOffset, boundsLen) = bounds.index(len)
        return Range(sliceLike, inner, offset + boundsOffset, boundsLen)
    }

    fun get(bounds: RangeBounds): Range<T>? {
        val (boundsOffset, boundsLen) = bounds.tryIndex(len) ?: return null
        return Range(sliceLike, inner, offset + boundsOffset, boundsLen)
    }

    fun splitAt(mid: Int): Pair<Range<T>, Range<T>> =
        slice(RangeTo(mid)) to slice(RangeFrom(mid))

    fun copy(): Range<T> = Range(sliceLike, inner, offset, len)

    fun asSlice(): T = sliceLike.asSlice(inner, offset, offset + len)

    fun commonPrefixLen(other: Range<T>): Int =
        sliceLike.commonPrefixLen(asSlice(), other.asSlice())

    fun commonSuffixLen(other: Range<T>): Int =
        sliceLike.commonSuffixLen(asSlice(), other.asSlice())

    fun commonOverlapLen(other: Range<T>): Int =
        sliceLike.commonOverlapLen(asSlice(), other.asSlice())

    fun startsWith(prefix: Range<T>): Boolean =
        sliceLike.startsWith(asSlice(), prefix.asSlice())

    fun endsWith(suffix: Range<T>): Boolean =
        sliceLike.endsWith(asSlice(), suffix.asSlice())

    override fun toString(): String = "Range { inner: $inner, offset: $offset, len: $len }"

    companion object {
        fun <T> new(sliceLike: SliceLike<T>, inner: T, bounds: RangeBounds): Range<T> {
            val (offset, len) = bounds.index(sliceLike.len(inner))
            return Range(sliceLike, inner, offset, len)
        }

        fun <T> empty(sliceLike: SliceLike<T>): Range<T> =
            Range(sliceLike, sliceLike.empty(), 0, 0)
    }
}

interface RangeBounds {
    // Returns (offset, len).
    fun tryIndex(len: Int): Pair<Int, Int>?

    fun index(len: Int): Pair<Int, Int> =
        tryIndex(len) ?: error("index out of range, index=$this, len=$len")
}

data class RangeStartEnd(val start: Int, val endExclusive: Int) : RangeBounds {
    override fun tryIndex(len: Int): Pair<Int, Int>? =
        if (start <= endExclusive && endExclusive <= len) {
            start to (endExclusive - start)
        } else {
            null
        }
}

data class RangeFrom(val start: Int) : RangeBounds {
    override fun tryIndex(len: Int): Pair<Int, Int>? =
        if (start <= len) start to (len - start) else null
}

data class RangeTo(val endExclusive: Int) : RangeBounds {
    override fun tryIndex(len: Int): Pair<Int, Int>? =
        if (endExclusive <= len) 0 to endExclusive else null
}

object RangeFull : RangeBounds {
    override fun tryIndex(len: Int): Pair<Int, Int> = 0 to len
}

interface SliceLike<T> {
    fun len(value: T): Int
    fun empty(): T
    fun asSlice(value: T, start: Int, endExclusive: Int): T
    fun commonPrefixLen(a: T, b: T): Int
    fun commonSuffixLen(a: T, b: T): Int
    fun commonOverlapLen(a: T, b: T): Int
    fun startsWith(value: T, prefix: T): Boolean
    fun endsWith(value: T, suffix: T): Boolean
}

object StrSliceLike : SliceLike<String> {
    override fun len(value: String): Int = value.length

    override fun empty(): String = ""

    override fun asSlice(value: String, start: Int, endExclusive: Int): String =
        value.substring(start, endExclusive)

    override fun commonPrefixLen(a: String, b: String): Int {
        val limit = min(a.length, b.length)
        var i = 0
        while (i < limit) {
            val ca = a[i]
            val cb = b[i]
            if (ca != cb) return i
            i++
        }
        return limit
    }

    override fun commonSuffixLen(a: String, b: String): Int {
        val limit = min(a.length, b.length)
        var i = 0
        while (i < limit) {
            val ca = a[a.length - 1 - i]
            val cb = b[b.length - 1 - i]
            if (ca != cb) return i
            i++
        }
        return limit
    }

    // returns length of overlap of prefix of `this` with suffix of `other`
    override fun commonOverlapLen(a: String, b: String): Int {
        var lhs = a
        var rhs = b
        // Eliminate the null case
        if (lhs.isEmpty() || rhs.isEmpty()) {
            return 0
        }

        when {
            lhs.length > rhs.length -> {
                var end = rhs.length
                while (!lhs.isCharBoundaryAt(end)) {
                    end -= 1
                }
                lhs = lhs.substring(0, end)
            }
            lhs.length < rhs.length -> {
                var start = rhs.length - lhs.length
                while (!rhs.isCharBoundaryAt(start)) {
                    start += 1
                }
                rhs = rhs.substring(start)
            }
            else -> {}
        }

        // Quick check for the worst case.
        if (lhs == rhs) {
            return lhs.length
        }

        // Start by looking for a single character match
        // and increase length until no match is found.
        // Performance analysis: https://neil.fraser.name/news/2010/11/04/
        var best = 0
        var length = 0
        var i = rhs.length
        while (i > 0) {
            val cpStart = rhs.codePointStartBefore(i)
            val pattern = rhs.substring(cpStart)
            val found = lhs.indexOf(pattern)
            if (found == -1) {
                return best
            }

            length += i - cpStart
            if (found == 0) {
                best = length
            }
            i = cpStart
        }

        return best
    }

    override fun startsWith(value: String, prefix: String): Boolean = value.startsWith(prefix)

    override fun endsWith(value: String, suffix: String): Boolean = value.endsWith(suffix)
}

object ByteSliceLike : SliceLike<ByteArray> {
    override fun len(value: ByteArray): Int = value.size

    override fun empty(): ByteArray = EMPTY_BYTES

    override fun asSlice(value: ByteArray, start: Int, endExclusive: Int): ByteArray =
        value.copyOfRange(start, endExclusive)

    override fun commonPrefixLen(a: ByteArray, b: ByteArray): Int {
        val limit = min(a.size, b.size)
        var i = 0
        while (i < limit) {
            if (a[i] != b[i]) return i
            i++
        }
        return limit
    }

    override fun commonSuffixLen(a: ByteArray, b: ByteArray): Int {
        val limit = min(a.size, b.size)
        var i = 0
        while (i < limit) {
            if (a[a.size - 1 - i] != b[b.size - 1 - i]) return i
            i++
        }
        return limit
    }

    // returns length of overlap of prefix of `this` with suffix of `other`
    // TODO make a more efficient solution
    override fun commonOverlapLen(a: ByteArray, b: ByteArray): Int {
        var len = min(a.size, b.size)
        while (len > 0) {
            if (a.regionEquals(0, b, b.size - len, len)) break
            len -= 1
        }
        return len
    }

    override fun startsWith(value: ByteArray, prefix: ByteArray): Boolean {
        if (prefix.size > value.size) return false
        for (i in prefix.indices) {
            if (value[i] != prefix[i]) return false
        }
        return true
    }

    override fun endsWith(value: ByteArray, suffix: ByteArray): Boolean {
        if (suffix.size > value.size) return false
        val base = value.size - suffix.size
        for (i in suffix.indices) {
            if (value[base + i] != suffix[i]) return false
        }
        return true
    }

    private val EMPTY_BYTES = ByteArray(0)

    private fun ByteArray.regionEquals(thisOffset: Int, other: ByteArray, otherOffset: Int, length: Int): Boolean {
        for (i in 0 until length) {
            if (this[thisOffset + i] != other[otherOffset + i]) return false
        }
        return true
    }
}

sealed class DiffRange<T> {
    class Equal<T>(val left: Range<T>, val right: Range<T>) : DiffRange<T>()
    class Delete<T>(val range: Range<T>) : DiffRange<T>()
    class Insert<T>(val range: Range<T>) : DiffRange<T>()

    fun inner(): Range<T> = when (this) {
        is Equal -> left
        is Delete -> range
        is Insert -> range
    }

    fun isEmpty(): Boolean = inner().isEmpty()

    fun len(): Int = inner().len()

    fun growUp(adjust: Int) = forEach { it.growUp(adjust) }

    fun growDown(adjust: Int) = forEach { it.growDown(adjust) }

    fun shrinkFront(adjust: Int) = forEach { it.shrinkFront(adjust) }

    fun shrinkBack(adjust: Int) = forEach { it.shrinkBack(adjust) }

    fun shiftUp(adjust: Int) = forEach { it.shiftUp(adjust) }

    fun shiftDown(adjust: Int) = forEach { it.shiftDown(adjust) }

    private inline fun forEach(f: (Range<T>) -> Unit) {
        when (this) {
            is Equal -> {
                f(left)
                f(right)
            }
            is Delete -> f(range)
            is Insert -> f(range)
        }
    }

    override fun toString(): String = when (this) {
        is Equal -> "DiffRange::Equal($left, $right)"
        is Delete -> "DiffRange::Delete($range)"
        is Insert -> "DiffRange::Insert($range)"
    }
}

fun DiffRange<ByteArray>.toStr(text1: String, text2: String): DiffRange<String> {
    fun boundaryDown(text: String, pos: Int): Int {
        var adjust = 0
        while (!text.isCharBoundaryAtUtf8(pos - adjust)) {
            adjust += 1
        }
        return adjust
    }

    fun boundaryUp(text: String, pos: Int): Int {
        var adjust = 0
        while (!text.isCharBoundaryAtUtf8(pos + adjust)) {
            adjust += 1
        }
        return adjust
    }

    return when (this) {
        is DiffRange.Equal -> {
            var offset1 = left.offset()
            var len1 = left.len()
            var offset2 = right.offset()
            var len2 = right.len()

            val adjustUp = boundaryUp(text1, offset1)
            offset1 += adjustUp
            len1 -= adjustUp
            offset2 += adjustUp
            len2 -= adjustUp
            val adjustDown = boundaryDown(text1, offset1 + len1)
            len1 -= adjustDown
            len2 -= adjustDown

            val charOffset1 = text1.utf8ByteIndexToCharIndex(offset1)
            val charLen1 = text1.utf8ByteIndexToCharIndex(offset1 + len1) - charOffset1
            val charOffset2 = text2.utf8ByteIndexToCharIndex(offset2)
            val charLen2 = text2.utf8ByteIndexToCharIndex(offset2 + len2) - charOffset2

            DiffRange.Equal(
                Range(StrSliceLike, text1, charOffset1, charLen1),
                Range(StrSliceLike, text2, charOffset2, charLen2),
            )
        }
        is DiffRange.Delete -> {
            var offset = range.offset()
            var len = range.len()
            val adjustDown = boundaryDown(text1, offset)
            offset -= adjustDown
            len += adjustDown
            val adjustUp = boundaryUp(text1, offset + len)
            len += adjustUp
            val charOffset = text1.utf8ByteIndexToCharIndex(offset)
            val charLen = text1.utf8ByteIndexToCharIndex(offset + len) - charOffset
            DiffRange.Delete(Range(StrSliceLike, text1, charOffset, charLen))
        }
        is DiffRange.Insert -> {
            var offset = range.offset()
            var len = range.len()
            val adjustDown = boundaryDown(text2, offset)
            offset -= adjustDown
            len += adjustDown
            val adjustUp = boundaryUp(text2, offset + len)
            len += adjustUp
            val charOffset = text2.utf8ByteIndexToCharIndex(offset)
            val charLen = text2.utf8ByteIndexToCharIndex(offset + len) - charOffset
            DiffRange.Insert(Range(StrSliceLike, text2, charOffset, charLen))
        }
    }
}

private fun String.isCharBoundaryAt(pos: Int): Boolean {
    if (pos == 0 || pos == length) return true
    if (pos < 0 || pos > length) return false
    val prev = this[pos - 1]
    return !prev.isHighSurrogate()
}

private fun String.codePointStartBefore(pos: Int): Int {
    if (pos <= 0) return 0
    val prev = this[pos - 1]
    if (prev.isLowSurrogate() && pos >= 2 && this[pos - 2].isHighSurrogate()) {
        return pos - 2
    }
    return pos - 1
}

private fun Int.utf8ByteWidth(): Int = when {
    this < 0x80 -> 1
    this < 0x800 -> 2
    this < 0x10000 -> 3
    else -> 4
}

private fun String.codePointAtKmp(index: Int): Int {
    val high = this[index].code
    if (high in 0xD800..0xDBFF && index + 1 < length) {
        val low = this[index + 1].code
        if (low in 0xDC00..0xDFFF) {
            return 0x10000 + ((high - 0xD800) shl 10) + (low - 0xDC00)
        }
    }
    return high
}

private fun String.isCharBoundaryAtUtf8(byteIdx: Int): Boolean {
    if (byteIdx == 0) return true
    var byteCount = 0
    var charIdx = 0
    while (charIdx < length) {
        if (byteCount == byteIdx) return true
        if (byteCount > byteIdx) return false
        val cp = codePointAtKmp(charIdx)
        byteCount += cp.utf8ByteWidth()
        charIdx += if (cp >= 0x10000) 2 else 1
    }
    return byteCount == byteIdx
}

private fun String.utf8ByteIndexToCharIndex(byteIdx: Int): Int {
    var byteCount = 0
    var charIdx = 0
    while (charIdx < length && byteCount < byteIdx) {
        val cp = codePointAtKmp(charIdx)
        val width = cp.utf8ByteWidth()
        if (byteCount + width > byteIdx) break
        byteCount += width
        charIdx += if (cp >= 0x10000) 2 else 1
    }
    return charIdx
}
