// port-lint: source diff/cleanup.rs
package io.github.kotlinmania.diffy.diff

import io.github.kotlinmania.diffy.DiffRange
import io.github.kotlinmania.diffy.RangeFrom
import io.github.kotlinmania.diffy.RangeTo

// Walks through all edits and shifts them up and then down, trying to see if they run into similar
// edits which can be merged
internal fun <T> compact(diffs: MutableList<DiffRange<T>>) {
    // First attempt to compact all Deletions
    var pointer = 0
    while (pointer < diffs.size) {
        val diff = diffs[pointer]
        if (diff is DiffRange.Delete) {
            pointer = shiftDiffUp(diffs, pointer)
            pointer = shiftDiffDown(diffs, pointer)
        }
        pointer += 1
    }

    // Then attempt to compact all Insertions
    pointer = 0
    while (pointer < diffs.size) {
        val diff = diffs[pointer]
        if (diff is DiffRange.Insert) {
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
    while (pointer > 0) {
        val prevDiff = diffs[pointer - 1]
        val thisDiff = diffs[pointer]

        when (thisDiff) {
            is DiffRange.Insert ->
                when (prevDiff) {
                    is DiffRange.Equal -> {
                        val prevDiff1 = prevDiff.left
                        val thisRange = thisDiff.range
                        val suffixLen = thisRange.commonSuffixLen(prevDiff1)
                        if (suffixLen != 0) {
                            if (pointer + 1 < diffs.size && diffs[pointer + 1] is DiffRange.Equal) {
                                diffs[pointer + 1].growUp(suffixLen)
                            } else {
                                diffs.add(
                                    pointer + 1,
                                    DiffRange.Equal(
                                        prevDiff1.slice(RangeFrom(prevDiff1.len - suffixLen)),
                                        thisRange.slice(RangeFrom(thisRange.len - suffixLen)),
                                    ),
                                )
                            }
                            diffs[pointer].shiftUp(suffixLen)
                            diffs[pointer - 1].shrinkBack(suffixLen)

                            if (diffs[pointer - 1].isEmpty()) {
                                diffs.removeAt(pointer - 1)
                                pointer -= 1
                            }
                        } else if (diffs[pointer - 1].isEmpty()) {
                            diffs.removeAt(pointer - 1)
                            pointer -= 1
                        } else {
                            break
                        }
                    }
                    is DiffRange.Insert -> {
                        diffs[pointer - 1].growDown(thisDiff.len())
                        diffs.removeAt(pointer)
                        pointer -= 1
                    }
                    is DiffRange.Delete -> {
                        val temp = diffs[pointer - 1]
                        diffs[pointer - 1] = diffs[pointer]
                        diffs[pointer] = temp
                        pointer -= 1
                    }
                }

            is DiffRange.Delete ->
                when (prevDiff) {
                    is DiffRange.Equal -> {
                        val prevDiff2 = prevDiff.right
                        val thisRange = thisDiff.range
                        val suffixLen = thisRange.commonSuffixLen(prevDiff2)
                        if (suffixLen != 0) {
                            if (pointer + 1 < diffs.size && diffs[pointer + 1] is DiffRange.Equal) {
                                diffs[pointer + 1].growUp(suffixLen)
                            } else {
                                diffs.add(
                                    pointer + 1,
                                    DiffRange.Equal(
                                        thisRange.slice(RangeFrom(thisRange.len - suffixLen)),
                                        prevDiff2.slice(RangeFrom(prevDiff2.len - suffixLen)),
                                    ),
                                )
                            }
                            diffs[pointer].shiftUp(suffixLen)
                            diffs[pointer - 1].shrinkBack(suffixLen)

                            if (diffs[pointer - 1].isEmpty()) {
                                diffs.removeAt(pointer - 1)
                                pointer -= 1
                            }
                        } else if (diffs[pointer - 1].isEmpty()) {
                            diffs.removeAt(pointer - 1)
                            pointer -= 1
                        } else {
                            break
                        }
                    }
                    is DiffRange.Delete -> {
                        diffs[pointer - 1].growDown(thisDiff.len())
                        diffs.removeAt(pointer)
                        pointer -= 1
                    }
                    is DiffRange.Insert -> {
                        val temp = diffs[pointer - 1]
                        diffs[pointer - 1] = diffs[pointer]
                        diffs[pointer] = temp
                        pointer -= 1
                    }
                }

            is DiffRange.Equal -> throw IllegalStateException("range to shift must be either Insert or Delete")
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
    while (pointer + 1 < diffs.size) {
        val thisDiff = diffs[pointer]
        val nextDiff = diffs[pointer + 1]

        when (thisDiff) {
            is DiffRange.Insert ->
                when (nextDiff) {
                    is DiffRange.Equal -> {
                        val nextDiff1 = nextDiff.left
                        val thisRange = thisDiff.range
                        val prefixLen = thisRange.commonPrefixLen(nextDiff1)
                        if (prefixLen != 0) {
                            if (pointer > 0 && diffs[pointer - 1] is DiffRange.Equal) {
                                diffs[pointer - 1].growDown(prefixLen)
                            } else {
                                diffs.add(
                                    pointer,
                                    DiffRange.Equal(
                                        nextDiff1.slice(RangeTo(prefixLen)),
                                        thisRange.slice(RangeTo(prefixLen)),
                                    ),
                                )
                                pointer += 1
                            }

                            diffs[pointer].shiftDown(prefixLen)
                            diffs[pointer + 1].shrinkFront(prefixLen)

                            if (diffs[pointer + 1].isEmpty()) {
                                diffs.removeAt(pointer + 1)
                            }
                        } else if (diffs[pointer + 1].isEmpty()) {
                            diffs.removeAt(pointer + 1)
                        } else {
                            break
                        }
                    }
                    is DiffRange.Delete -> {
                        val temp = diffs[pointer]
                        diffs[pointer] = diffs[pointer + 1]
                        diffs[pointer + 1] = temp
                        pointer += 1
                    }
                    is DiffRange.Insert -> {
                        diffs[pointer].growDown(nextDiff.len())
                        diffs.removeAt(pointer + 1)
                    }
                }

            is DiffRange.Delete ->
                when (nextDiff) {
                    is DiffRange.Equal -> {
                        val nextDiff2 = nextDiff.right
                        val thisRange = thisDiff.range
                        val prefixLen = thisRange.commonPrefixLen(nextDiff2)
                        if (prefixLen != 0) {
                            if (pointer > 0 && diffs[pointer - 1] is DiffRange.Equal) {
                                diffs[pointer - 1].growDown(prefixLen)
                            } else {
                                diffs.add(
                                    pointer,
                                    DiffRange.Equal(
                                        thisRange.slice(RangeTo(prefixLen)),
                                        nextDiff2.slice(RangeTo(prefixLen)),
                                    ),
                                )
                                pointer += 1
                            }

                            diffs[pointer].shiftDown(prefixLen)
                            diffs[pointer + 1].shrinkFront(prefixLen)

                            if (diffs[pointer + 1].isEmpty()) {
                                diffs.removeAt(pointer + 1)
                            }
                        } else if (diffs[pointer + 1].isEmpty()) {
                            diffs.removeAt(pointer + 1)
                        } else {
                            break
                        }
                    }
                    is DiffRange.Insert -> {
                        val temp = diffs[pointer]
                        diffs[pointer] = diffs[pointer + 1]
                        diffs[pointer + 1] = temp
                        pointer += 1
                    }
                    is DiffRange.Delete -> {
                        diffs[pointer].growDown(nextDiff.len())
                        diffs.removeAt(pointer + 1)
                    }
                }

            is DiffRange.Equal -> throw IllegalStateException("range to shift must be either Insert or Delete")
        }
    }

    return pointer
}
