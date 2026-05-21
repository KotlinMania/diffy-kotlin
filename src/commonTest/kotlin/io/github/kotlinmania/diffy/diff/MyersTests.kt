// port-lint: source src/diff/myers.rs
package io.github.kotlinmania.diffy.diff

import io.github.kotlinmania.diffy.ByteSliceLike
import io.github.kotlinmania.diffy.Range
import io.github.kotlinmania.diffy.RangeFull
import kotlin.test.Test

class MyersTests {
    @Test
    fun testFindMiddleSnake() {
        val a = Range.new(ByteSliceLike, "ABCABBA".encodeToByteArray(), RangeFull)
        val b = Range.new(ByteSliceLike, "CBABAC".encodeToByteArray(), RangeFull)
        val maxD = maxD(a.len(), b.len())
        val vf = V(maxD)
        val vb = V(maxD)
        findMiddleSnake(a, b, vf, vb)
    }
}
