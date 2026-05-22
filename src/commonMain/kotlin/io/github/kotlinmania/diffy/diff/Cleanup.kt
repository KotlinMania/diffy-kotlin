// port-lint: source src/diff/cleanup.rs
package io.github.kotlinmania.diffy.diff

import io.github.kotlinmania.diffy.DiffRange

// Walks through all edits and shifts them up and then down, trying to see if they run into similar
// edits which can be merged
internal fun <T> compact(diffs: MutableList<DiffRange<T>>) {
    // First attempt to compact all Deletions
    var pointer = 0
    while (pointer < diffs.size) {
        if (diffs[pointer] is DiffRange.Delete) {
            pointer = shiftDiffUp(diffs, pointer)
            pointer = shiftDiffDown(diffs, pointer)
        }
        pointer += 1
    }

    // TODO maybe able to merge these and do them in the same pass?
    // Then attempt to compact all Insertions
    pointer = 0
    while (pointer < diffs.size) {
        if (diffs[pointer] is DiffRange.Insert) {
            pointer = shiftDiffUp(diffs, pointer)
            pointer = shiftDiffDown(diffs, pointer)
        }
        pointer += 1
    }
}

// Attempts to shift the Insertion or Deletion at location `pointer` as far upwards as possible.
private fun <T> shiftDiffUp(
    diffs: MutableList<DiffRange<T>>,
    initialPointer: Int,
): Int {
    var pointer = initialPointer
    while (true) {
        val prevIndex = pointer - 1
        if (prevIndex < 0) break

        val prevDiff = diffs.getOrNull(prevIndex) ?: break

        when (val thisDiff = diffs[pointer]) {
            //
            // Shift Inserts Upwards
            //
            is DiffRange.Insert -> when (prevDiff) {
                is DiffRange.Equal -> {
                    // check common suffix for the amount we can shift
                    val suffixLen = thisDiff.range.commonSuffixLen(prevDiff.left)
                    if (suffixLen != 0) {
                        val nextIndex = pointer + 1
                        if (diffs.getOrNull(nextIndex) is DiffRange.Equal) {
                            diffs[nextIndex].growUp(suffixLen)
                        } else {
                            diffs.add(
                                nextIndex,
                                DiffRange.Equal(
                                    prevDiff.left.slice(io.github.kotlinmania.diffy.RangeFrom(prevDiff.left.len() - suffixLen)),
                                    thisDiff.range.slice(io.github.kotlinmania.diffy.RangeFrom(thisDiff.range.len() - suffixLen)),
                                )
                            )
                        }
                        diffs[pointer].shiftUp(suffixLen)
                        diffs[prevIndex].shrinkBack(suffixLen)

                        if (diffs[prevIndex].isEmpty()) {
                            diffs.removeAt(prevIndex)
                            pointer -= 1
                        }
                    } else if (diffs[prevIndex].isEmpty()) {
                        diffs.removeAt(prevIndex)
                        pointer -= 1
                    } else {
                        // We can't shift upwards anymore
                        break
                    }
                }
                is DiffRange.Insert -> {
                    // Merge the two ranges
                    diffs[prevIndex].growDown(thisDiff.len())
                    diffs.removeAt(pointer)
                    pointer -= 1
                }
                is DiffRange.Delete -> {
                    // Swap the Delete and Insert
                    diffs[prevIndex] = diffs[pointer].also { diffs[pointer] = diffs[prevIndex] }
                    pointer -= 1
                }
            }

            //
            // Shift Deletions Upwards
            //
            is DiffRange.Delete -> when (prevDiff) {
                is DiffRange.Equal -> {
                    // check common suffix for the amount we can shift
                    val suffixLen = thisDiff.range.commonSuffixLen(prevDiff.right)
                    if (suffixLen != 0) {
                        val nextIndex = pointer + 1
                        if (diffs.getOrNull(nextIndex) is DiffRange.Equal) {
                            diffs[nextIndex].growUp(suffixLen)
                        } else {
                            diffs.add(
                                nextIndex,
                                DiffRange.Equal(
                                    thisDiff.range.slice(io.github.kotlinmania.diffy.RangeFrom(thisDiff.range.len() - suffixLen)),
                                    prevDiff.right.slice(io.github.kotlinmania.diffy.RangeFrom(prevDiff.right.len() - suffixLen)),
                                )
                            )
                        }
                        diffs[pointer].shiftUp(suffixLen)
                        diffs[prevIndex].shrinkBack(suffixLen)

                        if (diffs[prevIndex].isEmpty()) {
                            diffs.removeAt(prevIndex)
                            pointer -= 1
                        }
                    } else if (diffs[prevIndex].isEmpty()) {
                        diffs.removeAt(prevIndex)
                        pointer -= 1
                    } else {
                        // We can't shift upwards anymore
                        break
                    }
                }
                is DiffRange.Delete -> {
                    // Merge the two ranges
                    diffs[prevIndex].growDown(thisDiff.len())
                    diffs.removeAt(pointer)
                    pointer -= 1
                }
                is DiffRange.Insert -> {
                    // Swap the Delete and Insert
                    diffs[prevIndex] = diffs[pointer].also { diffs[pointer] = diffs[prevIndex] }
                    pointer -= 1
                }
            }

            is DiffRange.Equal -> error("range to shift must be either Insert or Delete")
        }
    }

    return pointer
}

// Attempts to shift the Insertion or Deletion at location `pointer` as far downwards as possible.
private fun <T> shiftDiffDown(
    diffs: MutableList<DiffRange<T>>,
    initialPointer: Int,
): Int {
    var pointer = initialPointer
    while (true) {
        val nextIndex = pointer + 1
        val nextDiff = diffs.getOrNull(nextIndex) ?: break

        when (val thisDiff = diffs[pointer]) {
            //
            // Shift Insert Downward
            //
            is DiffRange.Insert -> when (nextDiff) {
                is DiffRange.Equal -> {
                    // check common prefix for the amount we can shift
                    val prefixLen = thisDiff.range.commonPrefixLen(nextDiff.left)
                    if (prefixLen != 0) {
                        val prevIndex = pointer - 1
                        if (diffs.getOrNull(prevIndex) is DiffRange.Equal) {
                            diffs[prevIndex].growDown(prefixLen)
                        } else {
                            diffs.add(
                                pointer,
                                DiffRange.Equal(
                                    nextDiff.left.slice(io.github.kotlinmania.diffy.RangeTo(prefixLen)),
                                    thisDiff.range.slice(io.github.kotlinmania.diffy.RangeTo(prefixLen)),
                                )
                            )
                            pointer += 1
                        }

                        diffs[pointer].shiftDown(prefixLen)
                        diffs[nextIndex].shrinkFront(prefixLen)

                        if (diffs[nextIndex].isEmpty()) {
                            diffs.removeAt(nextIndex)
                        }
                    } else if (diffs[nextIndex].isEmpty()) {
                        diffs.removeAt(nextIndex)
                    } else {
                        // We can't shift downwards anymore
                        break
                    }
                }
                is DiffRange.Insert -> {
                    // Merge the two ranges
                    diffs[pointer].growDown(nextDiff.len())
                    diffs.removeAt(nextIndex)
                }
                is DiffRange.Delete -> {
                    // Swap the Delete and Insert
                    diffs[pointer] = diffs[nextIndex].also { diffs[nextIndex] = diffs[pointer] }
                    pointer += 1
                }
            }

            //
            // Shift Deletion Downward
            //
            is DiffRange.Delete -> when (nextDiff) {
                is DiffRange.Equal -> {
                    // check common prefix for the amount we can shift
                    val prefixLen = thisDiff.range.commonPrefixLen(nextDiff.right)
                    if (prefixLen != 0) {
                        val prevIndex = pointer - 1
                        if (diffs.getOrNull(prevIndex) is DiffRange.Equal) {
                            diffs[prevIndex].growDown(prefixLen)
                        } else {
                            diffs.add(
                                pointer,
                                DiffRange.Equal(
                                    thisDiff.range.slice(io.github.kotlinmania.diffy.RangeTo(prefixLen)),
                                    nextDiff.right.slice(io.github.kotlinmania.diffy.RangeTo(prefixLen)),
                                )
                            )
                            pointer += 1
                        }

                        diffs[pointer].shiftDown(prefixLen)
                        diffs[nextIndex].shrinkFront(prefixLen)

                        if (diffs[nextIndex].isEmpty()) {
                            diffs.removeAt(nextIndex)
                        }
                    } else if (diffs[nextIndex].isEmpty()) {
                        diffs.removeAt(nextIndex)
                    } else {
                        // We can't shift downwards anymore
                        break
                    }
                }
                is DiffRange.Delete -> {
                    // Merge the two ranges
                    diffs[pointer].growDown(nextDiff.len())
                    diffs.removeAt(nextIndex)
                }
                is DiffRange.Insert -> {
                    // Swap the Delete and Insert
                    diffs[pointer] = diffs[nextIndex].also { diffs[nextIndex] = diffs[pointer] }
                    pointer += 1
                }
            }

            is DiffRange.Equal -> error("range to shift must be either Insert or Delete")
        }
    }

    return pointer
}
