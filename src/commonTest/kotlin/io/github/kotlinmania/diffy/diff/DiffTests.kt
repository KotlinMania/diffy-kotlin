// port-lint: source diff/tests.rs
package io.github.kotlinmania.diffy.diff

import io.github.kotlinmania.diffy.ByteSliceLike
import io.github.kotlinmania.diffy.DiffRange
import io.github.kotlinmania.diffy.Range
import io.github.kotlinmania.diffy.RangeStartEnd
import io.github.kotlinmania.diffy.StrSliceLike
import io.github.kotlinmania.diffy.apply.apply
import io.github.kotlinmania.diffy.apply.applyBytes
import io.github.kotlinmania.diffy.patch.Line
import io.github.kotlinmania.diffy.patch.Patch
import io.github.kotlinmania.diffy.patch.PatchFormatter
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffTests {
    private fun diffRangeList(vararg items: Pair<String, String>): MutableList<DiffRange<String>> {
        val text1Builder = StringBuilder()
        val text2Builder = StringBuilder()
        for ((kind, text) in items) {
            when (kind) {
                "Insert" -> text2Builder.append(text)
                "Delete" -> text1Builder.append(text)
                "Equal" -> {
                    text1Builder.append(text)
                    text2Builder.append(text)
                }
            }
        }
        val text1 = text1Builder.toString()
        val text2 = text2Builder.toString()
        var offset1 = 0
        var offset2 = 0
        val result = mutableListOf<DiffRange<String>>()
        for ((kind, text) in items) {
            when (kind) {
                "Insert" -> {
                    val r = Range.new(StrSliceLike, text2, RangeStartEnd(offset2, offset2 + text.length))
                    offset2 += text.length
                    result.add(DiffRange.Insert(r))
                }
                "Delete" -> {
                    val r = Range.new(StrSliceLike, text1, RangeStartEnd(offset1, offset1 + text.length))
                    offset1 += text.length
                    result.add(DiffRange.Delete(r))
                }
                "Equal" -> {
                    val r1 = Range.new(StrSliceLike, text1, RangeStartEnd(offset1, offset1 + text.length))
                    val r2 = Range.new(StrSliceLike, text2, RangeStartEnd(offset2, offset2 + text.length))
                    offset1 += text.length
                    offset2 += text.length
                    result.add(DiffRange.Equal(r1, r2))
                }
            }
        }
        return result
    }

    private fun assertDiffRange(
        expected: List<Diff<String>>,
        actual: List<DiffRange<String>>,
        msg: String = "",
    ) {
        assertEquals(expected.size, actual.size, "$msg: size mismatch")
        for (i in expected.indices) {
            val exp = expected[i]
            val act = actual[i]
            when (exp) {
                is Diff.Equal -> {
                    assertTrue(act is DiffRange.Equal, "$msg: at index $i expected Equal but got $act")
                    assertEquals(exp.value, act.left.asSlice(), "$msg: at index $i left slice mismatch")
                    assertEquals(exp.value, act.right.asSlice(), "$msg: at index $i right slice mismatch")
                }
                is Diff.Delete -> {
                    assertTrue(act is DiffRange.Delete, "$msg: at index $i expected Delete but got $act")
                    assertEquals(exp.value, act.range.asSlice(), "$msg: at index $i slice mismatch")
                }
                is Diff.Insert -> {
                    assertTrue(act is DiffRange.Insert, "$msg: at index $i expected Insert but got $act")
                    assertEquals(exp.value, act.range.asSlice(), "$msg: at index $i slice mismatch")
                }
            }
        }
    }

    private fun assertPatch(
        options: DiffOptions = DiffOptions.default(),
        old: String,
        new: String,
        expected: String,
    ) {
        val patch = options.createPatch(old, new)
        val bpatch = options.createPatchBytes(old.encodeToByteArray(), new.encodeToByteArray())
        val patchStr = patch.toString()
        val patchBytes = bpatch.toBytes()

        assertEquals(expected, patchStr)
        assertContentEquals(patchStr.encodeToByteArray(), patchBytes)
        assertContentEquals(expected.encodeToByteArray(), patchBytes)

        val parsedPatch = Patch.fromStr(expected).getOrThrow()
        assertEquals(patch, parsedPatch)

        val parsedPatchFromStr = Patch.fromStr(patchStr).getOrThrow()
        assertEquals(patch, parsedPatchFromStr)

        val parsedBytes = Patch.fromBytes(expected.encodeToByteArray()).getOrThrow()
        assertEquals(bpatch, parsedBytes)

        val parsedBytesFromPatch = Patch.fromBytes(patchBytes).getOrThrow()
        assertEquals(bpatch, parsedBytesFromPatch)

        assertEquals(new, apply(old, patch).getOrThrow())
        assertContentEquals(new.encodeToByteArray(), applyBytes(old.encodeToByteArray(), bpatch).getOrThrow())
    }

    @Test
    fun testDiffStr() {
        val a1 = "ABCABBA"
        val b1 = "CBABAC"
        val solution1 = diff(a1, b1)
        assertEquals(
            listOf(
                Diff.Delete("AB"),
                Diff.Equal("C"),
                Diff.Delete("A"),
                Diff.Equal("B"),
                Diff.Insert("A"),
                Diff.Equal("BA"),
                Diff.Insert("C"),
            ),
            solution1,
        )

        val a2 = "abgdef"
        val b2 = "gh"
        val solution2 = diff(a2, b2)
        assertEquals(
            listOf(
                Diff.Delete("ab"),
                Diff.Equal("g"),
                Diff.Delete("def"),
                Diff.Insert("h"),
            ),
            solution2,
        )

        val a3 = "bat"
        val b3 = "map"
        val solution3 = diff(a3, b3)
        assertEquals(
            listOf(
                Diff.Delete("b"),
                Diff.Insert("m"),
                Diff.Equal("a"),
                Diff.Delete("t"),
                Diff.Insert("p"),
            ),
            solution3,
        )

        val a4 = "ACZBDZ"
        val b4 = "ACBCBDEFD"
        val solution4 = diff(a4, b4)
        assertEquals(
            listOf(
                Diff.Equal("AC"),
                Diff.Delete("Z"),
                Diff.Equal("B"),
                Diff.Insert("CBDEF"),
                Diff.Equal("D"),
                Diff.Delete("Z"),
            ),
            solution4,
        )

        val a5 = "1A "
        val b5 = "1A B A 2"
        val solution5 = diff(a5, b5)
        assertEquals(
            listOf(
                Diff.Equal("1A "),
                Diff.Insert("B A 2"),
            ),
            solution5,
        )

        val a6 = "ACBD"
        val b6 = "ACBCBDEFD"
        val solution6 = diff(a6, b6)
        assertEquals(
            listOf(
                Diff.Equal("ACB"),
                Diff.Insert("CBDEF"),
                Diff.Equal("D"),
            ),
            solution6,
        )

        val a7 = "abc"
        val b7 = "def"
        val solution7 = diff(a7, b7)
        assertEquals(
            listOf(
                Diff.Delete("abc"),
                Diff.Insert("def"),
            ),
            solution7,
        )
    }

    @Test
    fun testDiffSlice() {
        val a = "bat".encodeToByteArray()
        val b = "map".encodeToByteArray()
        val solution = io.github.kotlinmania.diffy.diff.diff(ByteSliceLike, a, b)
        val expected =
            listOf(
                "Delete" to "b",
                "Insert" to "m",
                "Equal" to "a",
                "Delete" to "t",
                "Insert" to "p",
            )
        assertEquals(expected.size, solution.size)
    }

    @Test
    fun testUnicode() {
        val snowman = "\u2603"
        val comet = "\u2604"
        val d = diff(snowman, comet)
        assertEquals(listOf(Diff.Delete(snowman), Diff.Insert(comet)), d)
    }

    @Test
    fun testCompact() {
        val solution0 = diffRangeList()
        compact(solution0)
        assertDiffRange(emptyList(), solution0, "Null case")

        val solution1 = diffRangeList("Equal" to "a", "Delete" to "b", "Insert" to "c")
        compact(solution1)
        assertDiffRange(
            listOf(Diff.Equal("a"), Diff.Delete("b"), Diff.Insert("c")),
            solution1,
            "No change case",
        )

        val solution2 = diffRangeList("Delete" to "a", "Delete" to "b", "Delete" to "c")
        compact(solution2)
        assertDiffRange(listOf(Diff.Delete("abc")), solution2, "Compact deletions")

        val solution3 = diffRangeList("Insert" to "a", "Insert" to "b", "Insert" to "c")
        compact(solution3)
        assertDiffRange(listOf(Diff.Insert("abc")), solution3, "Compact Insertions")

        val solution4 =
            diffRangeList(
                "Delete" to "a",
                "Insert" to "b",
                "Delete" to "c",
                "Insert" to "d",
                "Equal" to "ef",
            )
        compact(solution4)
        assertDiffRange(
            listOf(Diff.Delete("ac"), Diff.Insert("bd"), Diff.Equal("ef")),
            solution4,
            "Compact interweave",
        )

        val solution5 =
            diffRangeList(
                "Equal" to "a",
                "Delete" to "b",
                "Equal" to "c",
                "Delete" to "ac",
                "Equal" to "x",
            )
        compact(solution5)
        assertDiffRange(
            listOf(Diff.Equal("a"), Diff.Delete("bca"), Diff.Equal("cx")),
            solution5,
            "Slide edit left",
        )

        val solution6 =
            diffRangeList(
                "Equal" to "x",
                "Delete" to "ca",
                "Equal" to "c",
                "Delete" to "b",
                "Equal" to "a",
            )
        compact(solution6)
        assertDiffRange(
            listOf(Diff.Equal("xca"), Diff.Delete("cba")),
            solution6,
            "Slide edit right",
        )

        val solution7 = diffRangeList("Equal" to "", "Insert" to "a", "Equal" to "b")
        compact(solution7)
        assertDiffRange(
            listOf(Diff.Insert("a"), Diff.Equal("b")),
            solution7,
            "Empty equality",
        )

        val solution8 =
            diffRangeList(
                "Equal" to "1",
                "Insert" to "A B ",
                "Equal" to "A ",
                "Insert" to "2",
            )
        compact(solution8)
        assertDiffRange(
            listOf(Diff.Equal("1A "), Diff.Insert("B A 2")),
            solution8,
        )

        val solution9 =
            diffRangeList(
                "Equal" to "AC",
                "Insert" to "BC",
                "Equal" to "BD",
                "Insert" to "EFD",
            )
        compact(solution9)
        assertDiffRange(
            listOf(Diff.Equal("ACB"), Diff.Insert("CBDEF"), Diff.Equal("D")),
            solution9,
        )

        val solution10 =
            diffRangeList(
                "Equal" to "AC",
                "Delete" to "Z",
                "Insert" to "BC",
                "Equal" to "BD",
                "Delete" to "Z",
                "Insert" to "EFD",
            )
        compact(solution10)
        assertDiffRange(
            listOf(
                Diff.Equal("AC"),
                Diff.Delete("Z"),
                Diff.Equal("B"),
                Diff.Insert("CBDEF"),
                Diff.Equal("D"),
                Diff.Delete("Z"),
            ),
            solution10,
            "Compact Inserts",
        )

        val solution11 =
            diffRangeList(
                "Equal" to "AC",
                "Insert" to "Z",
                "Delete" to "BC",
                "Equal" to "BD",
                "Insert" to "Z",
                "Delete" to "EFD",
            )
        compact(solution11)
        assertDiffRange(
            listOf(
                Diff.Equal("AC"),
                Diff.Insert("Z"),
                Diff.Equal("B"),
                Diff.Delete("CBDEF"),
                Diff.Equal("D"),
                Diff.Insert("Z"),
            ),
            solution11,
            "Compact Deletions",
        )
    }

    @Test
    fun diffStrPatch() {
        val a = "A\nB\nC\nA\nB\nB\nA\n"
        val b = "C\nB\nA\nB\nA\nC\n"
        val expected =
            """
--- original
+++ modified
@@ -1,7 +1,6 @@
-A
-B
 C
-A
 B
+A
 B
 A
+C
""".trimStart()

        assertPatch(old = a, new = b, expected = expected)
    }

    @Test
    fun sample() {
        val opts = DiffOptions.default()
        val lao =
            """
The Way that can be told of is not the eternal Way;
The name that can be named is not the eternal name.
The Nameless is the origin of Heaven and Earth;
The Named is the mother of all things.
Therefore let there always be non-being,
  so we may see their subtlety,
And let there always be being,
  so we may see their outcome.
The two are the same,
But after they are produced,
  they have different names.
""".trimStart()

        val tzu =
            """
The Nameless is the origin of Heaven and Earth;
The named is the mother of all things.

Therefore let there always be non-being,
  so we may see their subtlety,
And let there always be being,
  so we may see their outcome.
The two are the same,
But after they are produced,
  they have different names.
They both may be called deep and profound.
Deeper and more profound,
The door of all subtleties!
""".trimStart()

        val expected =
            """
--- original
+++ modified
@@ -1,7 +1,6 @@
-The Way that can be told of is not the eternal Way;
-The name that can be named is not the eternal name.
 The Nameless is the origin of Heaven and Earth;
-The Named is the mother of all things.
+The named is the mother of all things.
+
 Therefore let there always be non-being,
   so we may see their subtlety,
 And let there always be being,
@@ -9,3 +8,6 @@
 The two are the same,
 But after they are produced,
   they have different names.
+They both may be called deep and profound.
+Deeper and more profound,
+The door of all subtleties!
""".trimStart()

        assertPatch(options = opts, old = lao, new = tzu, expected = expected)

        val expectedContext0 =
            """
--- original
+++ modified
@@ -1,2 +0,0 @@
-The Way that can be told of is not the eternal Way;
-The name that can be named is not the eternal name.
@@ -4 +2,2 @@
-The Named is the mother of all things.
+The named is the mother of all things.
+
@@ -11,0 +11,3 @@
+They both may be called deep and profound.
+Deeper and more profound,
+The door of all subtleties!
""".trimStart()

        opts.setContextLen(0)
        assertPatch(options = opts, old = lao, new = tzu, expected = expectedContext0)

        val expectedContext1 =
            """
--- original
+++ modified
@@ -1,5 +1,4 @@
-The Way that can be told of is not the eternal Way;
-The name that can be named is not the eternal name.
 The Nameless is the origin of Heaven and Earth;
-The Named is the mother of all things.
+The named is the mother of all things.
+
 Therefore let there always be non-being,
@@ -11 +10,4 @@
   they have different names.
+They both may be called deep and profound.
+Deeper and more profound,
+The door of all subtleties!
""".trimStart()

        opts.setContextLen(1)
        assertPatch(options = opts, old = lao, new = tzu, expected = expectedContext1)
    }

    @Test
    fun noNewlineAtEof() {
        val old1 = "old line"
        val new1 = "new line"
        val expected1 =
            """
--- original
+++ modified
@@ -1 +1 @@
-old line
\ No newline at end of file
+new line
\ No newline at end of file
""".trimStart()
        assertPatch(old = old1, new = new1, expected = expected1)

        val old2 = "old line\n"
        val new2 = "new line"
        val expected2 =
            """
--- original
+++ modified
@@ -1 +1 @@
-old line
+new line
\ No newline at end of file
""".trimStart()
        assertPatch(old = old2, new = new2, expected = expected2)

        val old3 = "old line"
        val new3 = "new line\n"
        val expected3 =
            """
--- original
+++ modified
@@ -1 +1 @@
-old line
\ No newline at end of file
+new line
""".trimStart()
        assertPatch(old = old3, new = new3, expected = expected3)

        val old4 = "old line\ncommon line"
        val new4 = "new line\ncommon line"
        val expected4 =
            """
--- original
+++ modified
@@ -1,2 +1,2 @@
-old line
+new line
 common line
\ No newline at end of file
""".trimStart()
        assertPatch(old = old4, new = new4, expected = expected4)
    }

    @Test
    fun withoutNoNewlineAtEofMessage() {
        val old = "old line"
        val new = "new line"
        val expected =
            """
--- original
+++ modified
@@ -1 +1 @@
-old line
+new line
""".trimStart()

        val f = PatchFormatter.new().missingNewlineMessage(false)
        val patch = createPatch(old, new)
        val bpatch = createPatchBytes(old.encodeToByteArray(), new.encodeToByteArray())
        val patchStr = f.fmtPatch(patch)
        val patchBytes = f.fmtPatchBytes(bpatch)

        assertEquals(expected, patchStr)
        assertContentEquals(patchStr.encodeToByteArray(), patchBytes)
        assertContentEquals(expected.encodeToByteArray(), patchBytes)
        assertEquals(new, apply(old, patch).getOrThrow())
        assertContentEquals(new.encodeToByteArray(), applyBytes(old.encodeToByteArray(), bpatch).getOrThrow())
    }

    @Test
    fun myersDiffyVsGit() {
        val original =
            """
void Chunk_copy(Chunk *src, size_t src_start, Chunk *dst, size_t dst_start, size_t n)
{
    if (!Chunk_bounds_check(src, src_start, n)) return;
    if (!Chunk_bounds_check(dst, dst_start, n)) return;

    memcpy(dst->data + dst_start, src->data + src_start, n);
}

int Chunk_bounds_check(Chunk *chunk, size_t start, size_t n)
{
    if (chunk == NULL) return 0;

    return start <= chunk->length && n <= chunk->length - start;
}
""".trimStart()

        val a =
            """
int Chunk_bounds_check(Chunk *chunk, size_t start, size_t n)
{
    if (chunk == NULL) return 0;

    return start <= chunk->length && n <= chunk->length - start;
}

void Chunk_copy(Chunk *src, size_t src_start, Chunk *dst, size_t dst_start, size_t n)
{
    if (!Chunk_bounds_check(src, src_start, n)) return;
    if (!Chunk_bounds_check(dst, dst_start, n)) return;

    memcpy(dst->data + dst_start, src->data + src_start, n);
}
""".trimStart()

        val expectedGit =
            """
--- original
+++ modified
@@ -1,14 +1,14 @@
-void Chunk_copy(Chunk *src, size_t src_start, Chunk *dst, size_t dst_start, size_t n)
+int Chunk_bounds_check(Chunk *chunk, size_t start, size_t n)
 {
-    if (!Chunk_bounds_check(src, src_start, n)) return;
-    if (!Chunk_bounds_check(dst, dst_start, n)) return;
+    if (chunk == NULL) return 0;

-    memcpy(dst->data + dst_start, src->data + src_start, n);
+    return start <= chunk->length && n <= chunk->length - start;
 }

-int Chunk_bounds_check(Chunk *chunk, size_t start, size_t n)
+void Chunk_copy(Chunk *src, size_t src_start, Chunk *dst, size_t dst_start, size_t n)
 {
-    if (chunk == NULL) return 0;
+    if (!Chunk_bounds_check(src, src_start, n)) return;
+    if (!Chunk_bounds_check(dst, dst_start, n)) return;

-    return start <= chunk->length && n <= chunk->length - start;
+    memcpy(dst->data + dst_start, src->data + src_start, n);
 }
""".trimStart()

        val gitPatch = Patch.fromStr(expectedGit).getOrThrow()
        assertEquals(a, apply(original, gitPatch).getOrThrow())

        val expectedDiffy =
            """
--- original
+++ modified
@@ -1,3 +1,10 @@
+int Chunk_bounds_check(Chunk *chunk, size_t start, size_t n)
+{
+    if (chunk == NULL) return 0;
+
+    return start <= chunk->length && n <= chunk->length - start;
+}
+
 void Chunk_copy(Chunk *src, size_t src_start, Chunk *dst, size_t dst_start, size_t n)
 {
     if (!Chunk_bounds_check(src, src_start, n)) return;
@@ -5,10 +12,3 @@

     memcpy(dst->data + dst_start, src->data + src_start, n);
 }
-
-int Chunk_bounds_check(Chunk *chunk, size_t start, size_t n)
-{
-    if (chunk == NULL) return 0;
-
-    return start <= chunk->length && n <= chunk->length - start;
-}
""".trimStart()

        assertPatch(old = original, new = a, expected = expectedDiffy)
    }

    @Test
    fun suppressBlankEmpty() {
        val original = "1\n2\n3\n\n4\n"
        val modified = "1\n2\n3\n\n5\n"

        val expected = "--- original\n+++ modified\n@@ -2,4 +2,4 @@\n 2\n 3\n \n-4\n+5\n"

        val f1 = PatchFormatter.new().suppressBlankEmpty(false)
        val patch1 = createPatch(original, modified)
        val bpatch1 = createPatchBytes(original.encodeToByteArray(), modified.encodeToByteArray())
        val patchStr1 = f1.fmtPatch(patch1)
        val patchBytes1 = f1.fmtPatchBytes(bpatch1)

        assertEquals(expected, patchStr1)
        assertContentEquals(patchStr1.encodeToByteArray(), patchBytes1)
        assertContentEquals(expected.encodeToByteArray(), patchBytes1)
        assertEquals(modified, apply(original, patch1).getOrThrow())
        assertContentEquals(modified.encodeToByteArray(), applyBytes(original.encodeToByteArray(), bpatch1).getOrThrow())

        val expectedSuppressed =
            """
--- original
+++ modified
@@ -2,4 +2,4 @@
 2
 3

-4
+5
""".trimStart()

        val f2 = PatchFormatter.new().suppressBlankEmpty(true)
        val patch2 = createPatch(original, modified)
        val bpatch2 = createPatchBytes(original.encodeToByteArray(), modified.encodeToByteArray())
        val patchStr2 = f2.fmtPatch(patch2)
        val patchBytes2 = f2.fmtPatchBytes(bpatch2)

        assertEquals(expectedSuppressed, patchStr2)
        assertContentEquals(patchStr2.encodeToByteArray(), patchBytes2)
        assertContentEquals(expectedSuppressed.encodeToByteArray(), patchBytes2)
        assertEquals(modified, apply(original, patch2).getOrThrow())
        assertContentEquals(modified.encodeToByteArray(), applyBytes(original.encodeToByteArray(), bpatch2).getOrThrow())
    }

    @Test
    fun applyWithIncorrectHunkHasBoundedPerformance() {
        val patchStr =
            """
@@ -10,6 +1000000,8 @@
 First:
     Life before death,
     strength before weakness,
     journey before destination.
 Second:
-    I will put the law before all else.
+    I swear to seek justice,
+    to let it guide me,
+    until I find a more perfect Ideal.
""".trimStart()

        val original =
            """
First:
    Life before death,
    strength before weakness,
    journey before destination.
Second:
    I will put the law before all else.
""".trimStart()

        val expected =
            """
First:
    Life before death,
    strength before weakness,
    journey before destination.
Second:
    I swear to seek justice,
    to let it guide me,
    until I find a more perfect Ideal.
""".trimStart()

        val patch = Patch.fromStr(patchStr).getOrThrow()
        val result = apply(original, patch).getOrThrow()
        assertEquals(expected, result)
    }

    @Test
    fun reverseEmptyFile() {
        val p = createPatch("", "make it so")
        val reverse = p.reverse()

        for ((hunk, revHunk) in p.hunks().zip(reverse.hunks())) {
            for ((line, revLine) in hunk.lines().zip(revHunk.lines())) {
                when (line) {
                    is Line.Context -> assertEquals(line, revLine)
                    is Line.Delete -> assertTrue(revLine is Line.Insert && line.value == revLine.value)
                    is Line.Insert -> assertTrue(revLine is Line.Delete && line.value == revLine.value)
                }
            }
        }

        val reReverse = apply(apply("", p).getOrThrow(), reverse).getOrThrow()
        assertEquals("", reReverse)
    }

    @Test
    fun reverseMultiLineFile() {
        val original =
            """
Commander Worf
What do you want this time, Picard?!
Commander Worf how dare you speak to mean that way!
""".trimStart()

        val modified =
            """
Commander Worf
Yes, Captain Picard?
Commander Worf, you are a valued member of my crew
Why, thank you Captain.  As are you.  A true warrior. Kupluh!
Kupluh, Indeed
""".trimStart()

        val p = createPatch(original, modified)
        val reverse = p.reverse()

        val reReverse = apply(apply(original, p).getOrThrow(), reverse).getOrThrow()
        assertEquals(original, reReverse)
    }
}
