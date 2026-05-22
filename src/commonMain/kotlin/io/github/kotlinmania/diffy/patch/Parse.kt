// port-lint: source src/patch/parse.rs
package io.github.kotlinmania.diffy.patch

import io.github.kotlinmania.diffy.LineIter
import io.github.kotlinmania.diffy.StrTextLike
import io.github.kotlinmania.diffy.ByteTextLike
import io.github.kotlinmania.diffy.TextLike
import io.github.kotlinmania.diffy.escapedFilenameStr
import io.github.kotlinmania.diffy.escapedFilenameBytes

/**
 * Options that control parsing behavior.
 *
 * Defaults match the [parse]/[parseBytes] behavior.
 */
internal data class ParseOpts(
    val skipPreamble: Boolean = true,
    val rejectOrphanedHunks: Boolean = false,
) {
    /**
     * Don't skip preamble lines before `---`/`+++`/`@@`.
     *
     * Useful when the caller has already positioned the input
     * at the start of the patch content.
     */
    fun noSkipPreamble(): ParseOpts = copy(skipPreamble = false)

    /**
     * Reject orphaned `@@ ` hunk headers after parsed hunks,
     * matching `git apply` behavior.
     */
    fun rejectOrphanedHunks(): ParseOpts = copy(rejectOrphanedHunks = true)
}

internal class Parser<T>(
    private val textLike: TextLike<T>,
    input: T,
) {
    private val lines: Iterator<T>
    private var peeked: T? = null
    private var offset: Int = 0

    init {
        lines = LineIter(textLike, input)
    }

    fun peek(): T? {
        if (peeked == null && lines.hasNext()) {
            peeked = lines.next()
        }
        return peeked
    }

    fun offset(): Int = offset

    fun next(): Result<T> {
        val line = if (peeked != null) {
            val p = peeked
            peeked = null
            p!!
        } else {
            if (!lines.hasNext()) {
                return Result.failure(error(ParsePatchErrorKind.UnexpectedEof))
            }
            lines.next()
        }
        offset += textLike.len(line)
        return Result.success(line)
    }

    /** Creates an error with the current offset as span. */
    fun error(kind: ParsePatchErrorKind): ParsePatchError =
        ParsePatchError.new(kind, offset until offset)

    /** Creates an error with a specific offset as span. */
    fun errorAt(kind: ParsePatchErrorKind, atOffset: Int): ParsePatchError =
        ParsePatchError.new(kind, atOffset until atOffset)
}

internal fun parse(input: String): Result<Patch<String>> {
    val (result, _) = parseOneStr(input, ParseOpts().rejectOrphanedHunks())
    return result
}

internal fun parseBytes(input: ByteArray): Result<Patch<ByteArray>> {
    val (result, _) = parseOneBytes(input, ParseOpts().rejectOrphanedHunks())
    return result
}

/**
 * Parses one patch from String input.
 */
internal fun parseOneStr(
    input: String,
    opts: ParseOpts,
): Pair<Result<Patch<String>>, Int> {
    val parser = Parser(StrTextLike, input)

    val header = patchHeaderStr(parser, opts)
    if (header.isFailure) {
        return Result.failure<Patch<String>>(header.exceptionOrNull() as ParsePatchError) to parser.offset()
    }
    val (original, modified) = header.getOrThrow()

    val hunksResult = hunks(StrTextLike, parser)
    if (hunksResult.isFailure) {
        return Result.failure<Patch<String>>(hunksResult.exceptionOrNull() as ParsePatchError) to parser.offset()
    }
    val hunksList = hunksResult.getOrThrow()

    if (opts.rejectOrphanedHunks) {
        val orphanCheck = rejectOrphanedHunkHeaders(StrTextLike, parser)
        if (orphanCheck.isFailure) {
            return Result.failure<Patch<String>>(orphanCheck.exceptionOrNull() as ParsePatchError) to parser.offset()
        }
    }

    return Result.success(Patch.new(original, modified, hunksList)) to parser.offset()
}

/**
 * Parses one patch from ByteArray input.
 */
internal fun parseOneBytes(
    input: ByteArray,
    opts: ParseOpts,
): Pair<Result<Patch<ByteArray>>, Int> {
    val parser = Parser(ByteTextLike, input)

    val header = patchHeaderBytes(parser, opts)
    if (header.isFailure) {
        return Result.failure<Patch<ByteArray>>(header.exceptionOrNull() as ParsePatchError) to parser.offset()
    }
    val (original, modified) = header.getOrThrow()

    val hunksResult = hunks(ByteTextLike, parser)
    if (hunksResult.isFailure) {
        return Result.failure<Patch<ByteArray>>(hunksResult.exceptionOrNull() as ParsePatchError) to parser.offset()
    }
    val hunksList = hunksResult.getOrThrow()

    if (opts.rejectOrphanedHunks) {
        val orphanCheck = rejectOrphanedHunkHeaders(ByteTextLike, parser)
        if (orphanCheck.isFailure) {
            return Result.failure<Patch<ByteArray>>(orphanCheck.exceptionOrNull() as ParsePatchError) to parser.offset()
        }
    }

    return Result.success(Patch.new(original, modified, hunksList)) to parser.offset()
}

private fun patchHeaderStr(
    parser: Parser<String>,
    opts: ParseOpts,
): Result<Pair<String?, String?>> {
    if (opts.skipPreamble) {
        val skipResult = skipHeaderPreamble(StrTextLike, parser)
        if (skipResult.isFailure) {
            return Result.failure(skipResult.exceptionOrNull()!!)
        }
    }

    var filename1: String? = null
    var filename2: String? = null

    while (true) {
        val line = parser.peek() ?: break
        if (line.startsWith("--- ")) {
            if (filename1 != null) {
                return Result.failure(parser.error(ParsePatchErrorKind.MultipleOriginalHeaders))
            }
            val nextLine = parser.next()
            if (nextLine.isFailure) {
                return Result.failure(nextLine.exceptionOrNull()!!)
            }
            val parsed = parseFilenameStr("--- ", nextLine.getOrThrow())
            if (parsed.isFailure) {
                return Result.failure(parsed.exceptionOrNull()!!)
            }
            filename1 = parsed.getOrThrow()
        } else if (line.startsWith("+++ ")) {
            if (filename2 != null) {
                return Result.failure(parser.error(ParsePatchErrorKind.MultipleModifiedHeaders))
            }
            val nextLine = parser.next()
            if (nextLine.isFailure) {
                return Result.failure(nextLine.exceptionOrNull()!!)
            }
            val parsed = parseFilenameStr("+++ ", nextLine.getOrThrow())
            if (parsed.isFailure) {
                return Result.failure(parsed.exceptionOrNull()!!)
            }
            filename2 = parsed.getOrThrow()
        } else {
            break
        }
    }

    return Result.success(filename1 to filename2)
}

private fun patchHeaderBytes(
    parser: Parser<ByteArray>,
    opts: ParseOpts,
): Result<Pair<ByteArray?, ByteArray?>> {
    if (opts.skipPreamble) {
        val skipResult = skipHeaderPreamble(ByteTextLike, parser)
        if (skipResult.isFailure) {
            return Result.failure(skipResult.exceptionOrNull()!!)
        }
    }

    var filename1: ByteArray? = null
    var filename2: ByteArray? = null

    while (true) {
        val line = parser.peek() ?: break
        if (ByteTextLike.startsWith(line, "--- ")) {
            if (filename1 != null) {
                return Result.failure(parser.error(ParsePatchErrorKind.MultipleOriginalHeaders))
            }
            val nextLine = parser.next()
            if (nextLine.isFailure) {
                return Result.failure(nextLine.exceptionOrNull()!!)
            }
            val parsed = parseFilenameBytes("--- ", nextLine.getOrThrow())
            if (parsed.isFailure) {
                return Result.failure(parsed.exceptionOrNull()!!)
            }
            filename1 = parsed.getOrThrow()
        } else if (ByteTextLike.startsWith(line, "+++ ")) {
            if (filename2 != null) {
                return Result.failure(parser.error(ParsePatchErrorKind.MultipleModifiedHeaders))
            }
            val nextLine = parser.next()
            if (nextLine.isFailure) {
                return Result.failure(nextLine.exceptionOrNull()!!)
            }
            val parsed = parseFilenameBytes("+++ ", nextLine.getOrThrow())
            if (parsed.isFailure) {
                return Result.failure(parsed.exceptionOrNull()!!)
            }
            filename2 = parsed.getOrThrow()
        } else {
            break
        }
    }

    return Result.success(filename1 to filename2)
}

/**
 * Skip to the first filename header ("--- " or "+++ ") or hunk line,
 * skipping any preamble lines like "diff --git", etc.
 */
private fun <T> skipHeaderPreamble(
    textLike: TextLike<T>,
    parser: Parser<T>,
): Result<Unit> {
    while (true) {
        val line = parser.peek() ?: break
        if (textLike.startsWith(line, "--- ") ||
            textLike.startsWith(line, "+++ ") ||
            textLike.startsWith(line, "@@ ")
        ) {
            break
        }
        val nextResult = parser.next()
        if (nextResult.isFailure) {
            return Result.failure(nextResult.exceptionOrNull()!!)
        }
    }
    return Result.success(Unit)
}

private fun parseFilenameStr(
    prefix: String,
    line: String,
): Result<String> {
    val stripped = line.removePrefix(prefix).takeIf { it != line }
        ?: return Result.failure(ParsePatchError.new(ParsePatchErrorKind.InvalidFilename))

    val filename = stripped.substringBefore('\t').takeIf { it != stripped }
        ?: stripped.substringBefore('\n').takeIf { it != stripped }
        ?: return Result.failure(ParsePatchError.new(ParsePatchErrorKind.FilenameUnterminated))

    return escapedFilenameStr(filename)
}

private fun parseFilenameBytes(
    prefix: String,
    line: ByteArray,
): Result<ByteArray> {
    val prefixBytes = prefix.encodeToByteArray()
    val stripped = if (line.size >= prefixBytes.size && line.copyOfRange(0, prefixBytes.size).contentEquals(prefixBytes)) {
        line.copyOfRange(prefixBytes.size, line.size)
    } else {
        return Result.failure(ParsePatchError.new(ParsePatchErrorKind.InvalidFilename))
    }

    val tabIdx = stripped.indexOfFirst { it == '\t'.code.toByte() }
    val nlIdx = stripped.indexOfFirst { it == '\n'.code.toByte() }

    val filename = when {
        tabIdx >= 0 -> stripped.copyOfRange(0, tabIdx)
        nlIdx >= 0 -> stripped.copyOfRange(0, nlIdx)
        else -> return Result.failure(ParsePatchError.new(ParsePatchErrorKind.FilenameUnterminated))
    }

    return escapedFilenameBytes(filename)
}

private fun <T> verifyHunksInOrder(hunks: List<Hunk<T>>): Boolean {
    for (i in 0 until hunks.size - 1) {
        val current = hunks[i]
        val next = hunks[i + 1]
        if (current.oldRange().end() > next.oldRange().start() ||
            current.newRange().end() > next.newRange().start()
        ) {
            return false
        }
    }
    return true
}

/**
 * Scans remaining lines for orphaned `@@ ` hunk headers.
 *
 * In strict mode (git-apply behavior), trailing junk is allowed but
 * an `@@ ` line hiding behind that junk indicates a lost hunk.
 */
private fun <T> rejectOrphanedHunkHeaders(
    textLike: TextLike<T>,
    parser: Parser<T>,
): Result<Unit> {
    while (true) {
        val line = parser.peek() ?: break
        if (textLike.startsWith(line, "@@ ")) {
            return Result.failure(parser.error(ParsePatchErrorKind.OrphanedHunkHeader))
        }
        val nextResult = parser.next()
        if (nextResult.isFailure) {
            return Result.failure(nextResult.exceptionOrNull()!!)
        }
    }
    return Result.success(Unit)
}

private fun <T> hunks(
    textLike: TextLike<T>,
    parser: Parser<T>,
): Result<List<Hunk<T>>> {
    val hunksList = mutableListOf<Hunk<T>>()

    // Parse hunks while we see @@ headers.
    //
    // Following GNU patch behavior: stop at non-@@ content.
    // Any trailing content (including hidden @@ headers) is silently ignored.
    // This is more permissive than git apply, which errors on junk between hunks.
    while (parser.peek()?.let { textLike.startsWith(it, "@@ ") } == true) {
        val hunkResult = hunk(textLike, parser)
        if (hunkResult.isFailure) {
            return Result.failure(hunkResult.exceptionOrNull()!!)
        }
        hunksList.add(hunkResult.getOrThrow())
    }

    // check and verify that the Hunks are in sorted order and don't overlap
    if (!verifyHunksInOrder(hunksList)) {
        return Result.failure(parser.error(ParsePatchErrorKind.HunksOutOfOrder))
    }

    return Result.success(hunksList)
}

private fun <T> hunk(
    textLike: TextLike<T>,
    parser: Parser<T>,
): Result<Hunk<T>> {
    val hunkStart = parser.offset()
    val headerLineResult = parser.next()
    if (headerLineResult.isFailure) {
        return Result.failure(headerLineResult.exceptionOrNull()!!)
    }
    val headerLine = headerLineResult.getOrThrow()

    val headerResult = hunkHeader(textLike, headerLine)
    if (headerResult.isFailure) {
        val err = headerResult.exceptionOrNull() as ParsePatchError
        return Result.failure(parser.errorAt(err.kind, hunkStart))
    }
    val (range1, range2, functionContext) = headerResult.getOrThrow()

    val linesResult = hunkLines(textLike, parser, range1.len, range2.len, hunkStart)
    if (linesResult.isFailure) {
        return Result.failure(linesResult.exceptionOrNull()!!)
    }
    val lines = linesResult.getOrThrow()

    return Result.success(Hunk.new(range1, range2, functionContext, lines))
}

private data class HunkHeaderResult<T>(
    val range1: HunkRange,
    val range2: HunkRange,
    val functionContext: T?,
)

private fun <T> hunkHeader(
    textLike: TextLike<T>,
    input: T,
): Result<HunkHeaderResult<T>> {
    val stripped = textLike.stripPrefix(input, "@@ ")
        ?: return Result.failure(ParsePatchError.new(ParsePatchErrorKind.InvalidHunkHeader))

    val (ranges, afterRanges) = textLike.splitAtExclusive(stripped, " @@")
        ?: return Result.failure(ParsePatchError.new(ParsePatchErrorKind.HunkHeaderUnterminated))

    val functionContext = textLike.stripPrefix(afterRanges, " ")

    val (range1Str, range2Str) = textLike.splitAtExclusive(ranges, " ")
        ?: return Result.failure(ParsePatchError.new(ParsePatchErrorKind.InvalidHunkHeader))

    val range1WithoutMinus = textLike.stripPrefix(range1Str, "-")
        ?: return Result.failure(ParsePatchError.new(ParsePatchErrorKind.InvalidHunkHeader))

    val range1Result = parseRange(textLike, range1WithoutMinus)
    if (range1Result.isFailure) {
        return Result.failure(range1Result.exceptionOrNull()!!)
    }
    val range1 = range1Result.getOrThrow()

    val range2WithoutPlus = textLike.stripPrefix(range2Str, "+")
        ?: return Result.failure(ParsePatchError.new(ParsePatchErrorKind.InvalidHunkHeader))

    val range2Result = parseRange(textLike, range2WithoutPlus)
    if (range2Result.isFailure) {
        return Result.failure(range2Result.exceptionOrNull()!!)
    }
    val range2 = range2Result.getOrThrow()

    return Result.success(HunkHeaderResult(range1, range2, functionContext))
}

private fun <T> parseRange(
    textLike: TextLike<T>,
    s: T,
): Result<HunkRange> {
    val pair = textLike.splitAtExclusive(s, ",")
    val (start, len) = if (pair != null) {
        val startVal = textLike.parse(pair.first) { it.toIntOrNull() }
            ?: return Result.failure(ParsePatchError.new(ParsePatchErrorKind.InvalidRange))
        val lenVal = textLike.parse(pair.second) { it.toIntOrNull() }
            ?: return Result.failure(ParsePatchError.new(ParsePatchErrorKind.InvalidRange))
        startVal to lenVal
    } else {
        val startVal = textLike.parse(s) { it.toIntOrNull() }
            ?: return Result.failure(ParsePatchError.new(ParsePatchErrorKind.InvalidRange))
        startVal to 1
    }

    // reject ranges that overflow
    if (start < 0 || len < 0) {
        return Result.failure(ParsePatchError.new(ParsePatchErrorKind.InvalidRange))
    }
    val checkedAdd = start.toLong() + len.toLong()
    if (checkedAdd > Int.MAX_VALUE) {
        return Result.failure(ParsePatchError.new(ParsePatchErrorKind.InvalidRange))
    }

    return Result.success(HunkRange.new(start, len))
}

private fun <T> hunkLines(
    textLike: TextLike<T>,
    parser: Parser<T>,
    expectedOld: Int,
    expectedNew: Int,
    hunkStart: Int,
): Result<List<Line<T>>> {
    val lines = mutableListOf<Line<T>>()
    var noNewlineContext = false
    var noNewlineDelete = false
    var noNewlineInsert = false

    // Track current line counts (old = context + delete, new = context + insert)
    var oldCount = 0
    var newCount = 0

    while (true) {
        val peekedLine = parser.peek() ?: break

        // Check if hunk is complete
        val hunkComplete = oldCount >= expectedOld && newCount >= expectedNew

        val line: Line<T>? = when {
            textLike.startsWith(peekedLine, "@") -> {
                break
            }
            noNewlineContext -> {
                // After `\ No newline at end of file` on a context line,
                // only a new hunk header is valid. Any other line means
                // the hunk should be complete, or it's an error.
                if (hunkComplete) {
                    break
                }
                return Result.failure(parser.error(ParsePatchErrorKind.ExpectedEndOfHunk))
            }
            textLike.startsWith(peekedLine, " ") -> {
                if (hunkComplete) {
                    break
                }
                val stripped = textLike.stripPrefix(peekedLine, " ")!!
                Line.Context(stripped)
            }
            textLike.startsWith(peekedLine, "\n") -> {
                if (hunkComplete) {
                    break
                }
                Line.Context(peekedLine)
            }
            textLike.startsWith(peekedLine, "-") -> {
                if (noNewlineDelete) {
                    return Result.failure(parser.error(ParsePatchErrorKind.TooManyDeletedLines))
                }
                if (hunkComplete) {
                    break
                }
                val stripped = textLike.stripPrefix(peekedLine, "-")!!
                Line.Delete(stripped)
            }
            textLike.startsWith(peekedLine, "+") -> {
                if (noNewlineInsert) {
                    return Result.failure(parser.error(ParsePatchErrorKind.TooManyInsertedLines))
                }
                if (hunkComplete) {
                    break
                }
                val stripped = textLike.stripPrefix(peekedLine, "+")!!
                Line.Insert(stripped)
            }
            textLike.startsWith(peekedLine, NO_NEWLINE_AT_EOF) -> {
                // The `\ No newline at end of file` marker indicates
                // the previous line doesn't end with a newline.
                // It's not a content line itself.
                // Therefore, we
                //
                // * strip the newline character of the previous line
                // * don't increment line counts and continue to next directly
                if (lines.isEmpty()) {
                    return Result.failure(parser.error(ParsePatchErrorKind.UnexpectedNoNewlineMarker))
                }
                val lastLine = lines.removeLast()
                val stripResult = stripNewline(textLike, when (lastLine) {
                    is Line.Context -> lastLine.value
                    is Line.Delete -> lastLine.value
                    is Line.Insert -> lastLine.value
                })
                if (stripResult.isFailure) {
                    return Result.failure(stripResult.exceptionOrNull()!!)
                }
                val stripped = stripResult.getOrThrow()
                val modified = when (lastLine) {
                    is Line.Context -> {
                        noNewlineContext = true
                        Line.Context(stripped)
                    }
                    is Line.Delete -> {
                        noNewlineDelete = true
                        Line.Delete(stripped)
                    }
                    is Line.Insert -> {
                        noNewlineInsert = true
                        Line.Insert(stripped)
                    }
                }
                lines.add(modified)
                val nextResult = parser.next()
                if (nextResult.isFailure) {
                    return Result.failure(nextResult.exceptionOrNull()!!)
                }
                continue
            }
            else -> {
                // Non-hunk line encountered
                if (hunkComplete) {
                    // Hunk is complete, treat remaining content as garbage
                    break
                }
                return Result.failure(parser.error(ParsePatchErrorKind.UnexpectedHunkLine))
            }
        }

        if (line != null) {
            when (line) {
                is Line.Context -> {
                    oldCount += 1
                    newCount += 1
                }
                is Line.Delete -> {
                    oldCount += 1
                }
                is Line.Insert -> {
                    newCount += 1
                }
            }

            lines.add(line)
            val nextResult = parser.next()
            if (nextResult.isFailure) {
                return Result.failure(nextResult.exceptionOrNull()!!)
            }
        }
    }

    // Final check: ensure we got the expected number of lines
    if (oldCount != expectedOld || newCount != expectedNew) {
        return Result.failure(parser.errorAt(ParsePatchErrorKind.HunkMismatch, hunkStart))
    }

    return Result.success(lines)
}

private fun <T> stripNewline(
    textLike: TextLike<T>,
    s: T,
): Result<T> {
    val stripped = textLike.stripSuffix(s, "\n")
    if (stripped != null) {
        return Result.success(stripped)
    } else {
        return Result.failure(ParsePatchError.new(ParsePatchErrorKind.MissingNewline))
    }
}
