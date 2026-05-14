// port-lint: source src/range.rs
package io.github.kotlinmania.diffy

import kotlin.test.Test
import kotlin.test.assertEquals

class RangeTests {
    @Test
    fun testCommonPrefix() {
        val text1a = Range.new(StrSliceLike, "abc", RangeFull)
        val text2a = Range.new(StrSliceLike, "xyz", RangeFull)
        assertEquals(0, text1a.commonPrefixLen(text2a), "Null case")
        val text1b = Range.new(ByteSliceLike, "abc".encodeToByteArray(), RangeFull)
        val text2b = Range.new(ByteSliceLike, "xyz".encodeToByteArray(), RangeFull)
        assertEquals(0, text1b.commonPrefixLen(text2b), "Null case")

        val text1c = Range.new(StrSliceLike, "1234abcdef", RangeFull)
        val text2c = Range.new(StrSliceLike, "1234xyz", RangeFull)
        assertEquals(4, text1c.commonPrefixLen(text2c), "Non-null case")
        val text1d = Range.new(ByteSliceLike, "1234abcdef".encodeToByteArray(), RangeFull)
        val text2d = Range.new(ByteSliceLike, "1234xyz".encodeToByteArray(), RangeFull)
        assertEquals(4, text1d.commonPrefixLen(text2d), "Non-null case")

        val text1e = Range.new(StrSliceLike, "1234", RangeFull)
        val text2e = Range.new(StrSliceLike, "1234xyz", RangeFull)
        assertEquals(4, text1e.commonPrefixLen(text2e), "Whole case")

        val text1f = Range.new(ByteSliceLike, "1234".encodeToByteArray(), RangeFull)
        val text2f = Range.new(ByteSliceLike, "1234xyz".encodeToByteArray(), RangeFull)
        assertEquals(4, text1f.commonPrefixLen(text2f), "Whole case")

        val snowman = "☃"
        val comet = "☄"
        val text1g = Range.new(StrSliceLike, snowman, RangeFull)
        val text2g = Range.new(StrSliceLike, comet, RangeFull)
        assertEquals(0, text1g.commonPrefixLen(text2g), "Unicode case")
        val text1h = Range.new(ByteSliceLike, snowman.encodeToByteArray(), RangeFull)
        val text2h = Range.new(ByteSliceLike, comet.encodeToByteArray(), RangeFull)
        assertEquals(2, text1h.commonPrefixLen(text2h), "Unicode case")
    }

    @Test
    fun testCommonSuffix() {
        val text1a = Range.new(StrSliceLike, "abc", RangeFull)
        val text2a = Range.new(StrSliceLike, "xyz", RangeFull)
        assertEquals(0, text1a.commonSuffixLen(text2a), "Null case")
        val text1b = Range.new(ByteSliceLike, "abc".encodeToByteArray(), RangeFull)
        val text2b = Range.new(ByteSliceLike, "xyz".encodeToByteArray(), RangeFull)
        assertEquals(0, text1b.commonSuffixLen(text2b), "Null case")

        val text1c = Range.new(StrSliceLike, "abcdef1234", RangeFull)
        val text2c = Range.new(StrSliceLike, "xyz1234", RangeFull)
        assertEquals(4, text1c.commonSuffixLen(text2c), "Non-null case")
        val text1d = Range.new(ByteSliceLike, "abcdef1234".encodeToByteArray(), RangeFull)
        val text2d = Range.new(ByteSliceLike, "xyz1234".encodeToByteArray(), RangeFull)
        assertEquals(4, text1d.commonSuffixLen(text2d), "Non-null case")

        val text1e = Range.new(StrSliceLike, "1234", RangeFull)
        val text2e = Range.new(StrSliceLike, "xyz1234", RangeFull)
        assertEquals(4, text1e.commonSuffixLen(text2e), "Whole case")
        val text1f = Range.new(ByteSliceLike, "1234".encodeToByteArray(), RangeFull)
        val text2f = Range.new(ByteSliceLike, "xyz1234".encodeToByteArray(), RangeFull)
        assertEquals(4, text1f.commonSuffixLen(text2f), "Whole case")
    }

    @Test
    fun testCommonOverlap() {
        val empty1 = Range.empty(StrSliceLike)
        val text2a = Range.new(StrSliceLike, "abcd", RangeFull)
        assertEquals(0, empty1.commonOverlapLen(text2a), "Null case")
        val empty2 = Range.empty(ByteSliceLike)
        val text2b = Range.new(ByteSliceLike, "abcd".encodeToByteArray(), RangeFull)
        assertEquals(0, empty2.commonOverlapLen(text2b), "Null case")

        val text1c = Range.new(StrSliceLike, "abcd", RangeFull)
        val text2c = Range.new(StrSliceLike, "abc", RangeFull)
        assertEquals(3, text1c.commonOverlapLen(text2c), "Whole case")
        val text1d = Range.new(ByteSliceLike, "abcd".encodeToByteArray(), RangeFull)
        val text2d = Range.new(ByteSliceLike, "abc".encodeToByteArray(), RangeFull)
        assertEquals(3, text1d.commonOverlapLen(text2d), "Whole case")

        val text1e = Range.new(StrSliceLike, "123456", RangeFull)
        val text2e = Range.new(StrSliceLike, "abcd", RangeFull)
        assertEquals(0, text1e.commonOverlapLen(text2e), "No overlap")
        val text1f = Range.new(ByteSliceLike, "123456".encodeToByteArray(), RangeFull)
        val text2f = Range.new(ByteSliceLike, "abcd".encodeToByteArray(), RangeFull)
        assertEquals(0, text1f.commonOverlapLen(text2f), "No overlap")

        val text1g = Range.new(StrSliceLike, "xxxabcd", RangeFull)
        val text2g = Range.new(StrSliceLike, "123456xxx", RangeFull)
        assertEquals(3, text1g.commonOverlapLen(text2g), "Overlap")
        val text1h = Range.new(ByteSliceLike, "xxxabcd".encodeToByteArray(), RangeFull)
        val text2h = Range.new(ByteSliceLike, "123456xxx".encodeToByteArray(), RangeFull)
        assertEquals(3, text1h.commonOverlapLen(text2h), "Overlap")

        // Some overly clever languages (C#) may treat ligatures as equal to their
        // component letters. E.g. U+FB01 == 'fi'
        val text1i = Range.new(StrSliceLike, "fi", RangeFull)
        val text2i = Range.new(StrSliceLike, "ﬁi", RangeFull)
        assertEquals(0, text1i.commonOverlapLen(text2i), "Unicode")
    }
}
