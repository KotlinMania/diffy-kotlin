// port-lint: source src/diff/myers.rs
package io.github.kotlinmania.diffy.diff

import io.github.kotlinmania.diffy.DiffRange
import io.github.kotlinmania.diffy.Range
import io.github.kotlinmania.diffy.RangeFrom
import io.github.kotlinmania.diffy.RangeStartEnd
import io.github.kotlinmania.diffy.RangeTo
import io.github.kotlinmania.diffy.SliceLike
import kotlin.math.abs

// A D-path is a path which starts at (0,0) that has exactly D non-diagonal edges. All D-paths
// consist of a (D - 1)-path followed by a non-diagonal edge and then a possibly empty sequence of
// diagonal edges called a snake.

// `V` contains the endpoints of the furthest reaching `D-paths`. For each recorded endpoint
// `(x,y)` in diagonal `k`, we only need to retain `x` because `y` can be computed from `x - k`.
// In other words, `V` is an array of integers where `V[k]` contains the row index of the endpoint
// of the furthest reaching path in diagonal `k`.
//
// We can't use a traditional List to represent `V` since we use `k` as an index and it can take
// on negative values. So instead `V` is represented as a light-weight wrapper around an IntArray
// plus an `offset` which is the maximum value `k` can take on in order to map negative `k`'s back
// to a value >= 0.
internal class V(maxD: Int) {
    val offset: Int = maxD
    // Look into initializing this to -1 and storing signed values
    val v: IntArray = IntArray(2 * maxD)

    fun len(): Int = v.size

    operator fun get(index: Int): Int = v[index + offset]

    operator fun set(index: Int, value: Int) {
        v[index + offset] = value
    }
}

// A `Snake` is a sequence of diagonal edges in the edit graph. It is possible for a snake to have
// a length of zero, meaning the start and end points are the same.
internal class Snake(
    val xStart: Int,
    val yStart: Int,
    val xEnd: Int,
    val yEnd: Int,
) {
    override fun toString(): String = "($xStart, $yStart) -> ($xEnd, $yEnd)"
}

internal fun maxD(len1: Int, len2: Int): Int {
    // XXX look into reducing the need to have the additional '+ 1'
    return (len1 + len2 + 1) / 2 + 1
}

// The divide part of a divide-and-conquer strategy. A D-path has D+1 snakes some of which may
// be empty. The divide step requires finding the ceil(D/2) + 1 or middle snake of an optimal
// D-path. The idea for doing so is to simultaneously run the basic algorithm in both the
// forward and reverse directions until furthest reaching forward and reverse paths starting at
// opposing corners 'overlap'.
internal fun <S> findMiddleSnake(
    old: Range<S>,
    new: Range<S>,
    vf: V,
    vb: V,
): Pair<Int, Snake> {
    val n = old.len()
    val m = new.len()

    // By Lemma 1 in the paper, the optimal edit script length is odd or even as `delta` is odd
    // or even.
    val delta = n - m
    val odd = (delta and 1) == 1

    // The initial point at (0, -1)
    vf[1] = 0
    // The initial point at (N, M+1)
    vb[1] = 0

    // We only need to explore ceil(D/2) + 1
    val dMax = maxD(n, m)
    check(vf.len() >= dMax)
    check(vb.len() >= dMax)

    var d = 0
    while (d < dMax) {
        // Forward path
        run {
            var k = d
            while (k >= -d) {
                var x = if (k == -d || (k != d && vf[k - 1] < vf[k + 1])) {
                    vf[k + 1]
                } else {
                    vf[k - 1] + 1
                }
                var y = x - k

                // The coordinate of the start of a snake
                val x0 = x
                val y0 = y
                //  While these sequences are identical, keep moving through the graph with no cost
                val s1 = old.get(RangeFrom(x))
                val s2 = new.get(RangeFrom(y))
                if (s1 != null && s2 != null) {
                    val advance = s1.commonPrefixLen(s2)
                    x += advance
                    y += advance
                }

                // This is the new best x value
                vf[k] = x
                // Only check for connections from the forward search when N - M is odd
                // and when there is a reciprocal k line coming from the other direction.
                if (odd && abs(k - delta) <= (d - 1)) {
                    // TODO optimize this so we don't have to compare against n
                    if (vf[k] + vb[-(k - delta)] >= n) {
                        // Return the snake
                        val snake = Snake(
                            xStart = x0,
                            yStart = y0,
                            xEnd = x,
                            yEnd = y,
                        )
                        // Edit distance to this snake is `2 * d - 1`
                        return (2 * d - 1) to snake
                    }
                }

                k -= 2
            }
        }

        // Backward path
        run {
            var k = d
            while (k >= -d) {
                var x = if (k == -d || (k != d && vb[k - 1] < vb[k + 1])) {
                    vb[k + 1]
                } else {
                    vb[k - 1] + 1
                }
                var y = x - k

                // The coordinate of the start of a snake
                val x0 = x
                val y0 = y
                if (x < n && y < m) {
                    val advance = old.slice(RangeTo(n - x))
                        .commonSuffixLen(new.slice(RangeTo(m - y)))
                    x += advance
                    y += advance
                }

                // This is the new best x value
                vb[k] = x

                if (!odd && abs(k - delta) <= d) {
                    // TODO optimize this so we don't have to compare against n
                    if (vb[k] + vf[-(k - delta)] >= n) {
                        // Return the snake
                        val snake = Snake(
                            xStart = n - x,
                            yStart = m - y,
                            xEnd = n - x0,
                            yEnd = m - y0,
                        )
                        // Edit distance to this snake is `2 * d`
                        return (2 * d) to snake
                    }
                }

                k -= 2
            }
        }

        // TODO: Maybe there's an opportunity to optimize and bail early?
        d += 1
    }

    error("unable to find a middle snake")
}

internal fun <S> conquer(
    oldIn: Range<S>,
    newIn: Range<S>,
    vf: V,
    vb: V,
    solution: MutableList<DiffRange<S>>,
) {
    var old = oldIn
    var new = newIn

    // Check for common prefix
    val commonPrefixLen = old.commonPrefixLen(new)
    if (commonPrefixLen > 0) {
        val commonPrefix = DiffRange.Equal(
            old.slice(RangeTo(commonPrefixLen)),
            new.slice(RangeTo(commonPrefixLen)),
        )
        solution.add(commonPrefix)
    }

    old = old.slice(RangeStartEnd(commonPrefixLen, old.len()))
    new = new.slice(RangeStartEnd(commonPrefixLen, new.len()))

    // Check for common suffix
    val commonSuffixLen = old.commonSuffixLen(new)
    val commonSuffix = DiffRange.Equal(
        old.slice(RangeFrom(old.len() - commonSuffixLen)),
        new.slice(RangeFrom(new.len() - commonSuffixLen)),
    )
    old = old.slice(RangeTo(old.len() - commonSuffixLen))
    new = new.slice(RangeTo(new.len() - commonSuffixLen))

    if (old.isEmpty() && new.isEmpty()) {
        // Do nothing
    } else if (old.isEmpty()) {
        // Inserts
        solution.add(DiffRange.Insert(new))
    } else if (new.isEmpty()) {
        // Deletes
        solution.add(DiffRange.Delete(old))
    } else {
        // Divide & Conquer
        val (_shortestEditScriptLen, snake) = findMiddleSnake(old, new, vf, vb)

        val (oldA, oldB) = old.splitAt(snake.xStart)
        val (newA, newB) = new.splitAt(snake.yStart)

        conquer(oldA, newA, vf, vb, solution)
        conquer(oldB, newB, vf, vb, solution)
    }

    if (commonSuffixLen > 0) {
        solution.add(commonSuffix)
    }
}

fun <S> diff(
    sliceLike: SliceLike<S>,
    old: S,
    new: S,
): List<DiffRange<S>> {
    val oldRecs = Range.new(sliceLike, old, io.github.kotlinmania.diffy.RangeFull)
    val newRecs = Range.new(sliceLike, new, io.github.kotlinmania.diffy.RangeFull)

    val solution = mutableListOf<DiffRange<S>>()

    // The arrays that hold the 'best possible x values' in search from:
    // `vf`: top left to bottom right
    // `vb`: bottom right to top left
    val maxD = maxD(sliceLike.len(old), sliceLike.len(new))
    val vf = V(maxD)
    val vb = V(maxD)

    conquer(oldRecs, newRecs, vf, vb, solution)

    return solution
}
