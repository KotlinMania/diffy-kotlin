// port-lint: source merge/tests.rs
package io.github.kotlinmania.diffy.merge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MergeTests {
    private fun assertMerge(
        original: String,
        ours: String,
        theirs: String,
        expectedSuccess: String?,
        expectedConflict: String?,
        msg: String,
    ) {
        val solution = merge(original, ours, theirs)
        if (expectedSuccess != null) {
            assertTrue(solution.isSuccess, "$msg: expected success")
            assertEquals(expectedSuccess, solution.getOrThrow(), msg)
        } else {
            assertTrue(solution.isFailure, "$msg: expected failure")
            val ex = solution.exceptionOrNull() as MergeConflictException
            assertEquals(expectedConflict, ex.conflictOutput, msg)
        }

        val solutionBytes =
            mergeBytes(
                original.encodeToByteArray(),
                ours.encodeToByteArray(),
                theirs.encodeToByteArray(),
            )
        if (expectedSuccess != null) {
            assertTrue(solutionBytes.isSuccess, "$msg (bytes): expected success")
            assertEquals(expectedSuccess, solutionBytes.getOrThrow().decodeToString(), "$msg (bytes)")
        } else {
            assertTrue(solutionBytes.isFailure, "$msg (bytes): expected failure")
            val ex = solutionBytes.exceptionOrNull() as MergeConflictBytesException
            assertEquals(expectedConflict, ex.conflictOutput.decodeToString(), "$msg (bytes)")
        }
    }

    @Test
    fun testMerge() {
        val original =
            """
carrots
garlic
onions
salmon
mushrooms
tomatoes
salt
""".trimStart()

        val a =
            """
carrots
salmon
mushrooms
tomatoes
garlic
onions
salt
""".trimStart()

        val b =
            """
carrots
salmon
garlic
onions
mushrooms
tomatoes
salt
""".trimStart()

        assertMerge(original, original, original, original, null, "Equal case #1")
        assertMerge(original, a, a, a, null, "Equal case #2")
        assertMerge(original, b, b, b, null, "Equal case #3")

        val expectedSingleConflict =
            """
carrots
<<<<<<< ours
salmon
||||||| original
garlic
onions
salmon
=======
salmon
garlic
onions
>>>>>>> theirs
mushrooms
tomatoes
garlic
onions
salt
""".trimStart()

        assertMerge(original, a, b, null, expectedSingleConflict, "Single Conflict case")

        val expectedReverseSingleConflict =
            """
carrots
<<<<<<< ours
salmon
garlic
onions
||||||| original
garlic
onions
salmon
=======
salmon
>>>>>>> theirs
mushrooms
tomatoes
garlic
onions
salt
""".trimStart()

        assertMerge(
            original,
            b,
            a,
            null,
            expectedReverseSingleConflict,
            "Reverse Single Conflict case",
        )

        val originalMulti =
            """
carrots
garlic
onions
salmon
tomatoes
salt
""".trimStart()

        val aMulti =
            """
carrots
salmon
tomatoes
garlic
onions
salt
""".trimStart()

        val bMulti =
            """
carrots
salmon
garlic
onions
tomatoes
salt
""".trimStart()

        val expectedMultipleConflict =
            """
carrots
<<<<<<< ours
salmon
tomatoes
||||||| original
=======
salmon
>>>>>>> theirs
garlic
onions
<<<<<<< ours
||||||| original
salmon
tomatoes
=======
tomatoes
>>>>>>> theirs
salt
""".trimStart()

        assertMerge(originalMulti, aMulti, bMulti, null, expectedMultipleConflict, "Multiple Conflict case")

        val expectedReverseMultipleConflict =
            """
carrots
<<<<<<< ours
salmon
||||||| original
=======
salmon
tomatoes
>>>>>>> theirs
garlic
onions
<<<<<<< ours
tomatoes
||||||| original
salmon
tomatoes
=======
>>>>>>> theirs
salt
""".trimStart()

        assertMerge(
            originalMulti,
            bMulti,
            aMulti,
            null,
            expectedReverseMultipleConflict,
            "Reverse Multiple Conflict case",
        )
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

        val b =
            """
void Chunk_copy(Chunk *src, size_t src_start, Chunk *dst, size_t dst_start, size_t n)
{
    if (!Chunk_bounds_check(src, src_start, n)) return;
    if (!Chunk_bounds_check(dst, dst_start, n)) return;

    // copy the bytes
    memcpy(dst->data + dst_start, src->data + src_start, n);
}

int Chunk_bounds_check(Chunk *chunk, size_t start, size_t n)
{
    if (chunk == NULL) return 0;

    return start <= chunk->length && n <= chunk->length - start;
}
""".trimStart()

        val expectedDiffy =
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

    // copy the bytes
    memcpy(dst->data + dst_start, src->data + src_start, n);
}
""".trimStart()

        assertMerge(original, a, b, expectedDiffy, null, "Myers diffy merge")
    }

    @Test
    fun correctRangeIsUsedForBothCase() {
        val base =
            """
class GithubCall(db.Model):

`url`: URL of request Example.`https://api.github.com`
"""

        val theirs =
            """
class GithubCall(db.Model):

`repo`: String field. Github repository fields. Example: `amitu/python`
"""

        val ours =
            """
class Call(models.Model):
`body`: String field. The payload of the webhook call from the github.

`repo`: String field. Github repository fields. Example: `amitu/python`
"""

        val expected =
            """
class Call(models.Model):
`body`: String field. The payload of the webhook call from the github.

`repo`: String field. Github repository fields. Example: `amitu/python`
"""

        assertMerge(base, ours, theirs, expected, null, "MergeRange::Both case")
    }

    @Test
    fun deleteAndInsertConflict() {
        val base =
            """
{
    int a = 2;
}
"""

        val ours =
            """
{
}
"""

        val theirs =
            """
{
    int a = 2;
    int b = 3;
}
"""

        val expected =
            """
{
<<<<<<< ours
||||||| original
    int a = 2;
=======
    int a = 2;
    int b = 3;
>>>>>>> theirs
}
"""

        assertMerge(
            base,
            ours,
            theirs,
            null,
            expected,
            "MergeRange (Ours::delete, Theirs::insert) conflict",
        )

        val expectedReverse =
            """
{
<<<<<<< ours
    int a = 2;
    int b = 3;
||||||| original
    int a = 2;
=======
>>>>>>> theirs
}
"""

        assertMerge(
            base,
            theirs,
            ours,
            null,
            expectedReverse,
            "MergeRange (Theirs::delete, Ours::insert) conflict",
        )
    }
}
