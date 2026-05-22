// port-lint: source src/patch/error.rs
package io.github.kotlinmania.diffy.patch

/**
 * An error returned when parsing a [Patch] using [Patch.fromStr] fails.
 */
@ConsistentCopyVisibility
data class ParsePatchError internal constructor(
    internal val kind: ParsePatchErrorKind,
    val span: IntRange?,
) : Exception() {
    override val message: String
        get() = if (span != null) {
            "error parsing patch at byte ${span.first}: $kind"
        } else {
            "error parsing patch: $kind"
        }

    override fun toString(): String = message

    companion object {
        internal fun new(kind: ParsePatchErrorKind, span: IntRange): ParsePatchError =
            ParsePatchError(kind, span)

        internal fun new(kind: ParsePatchErrorKind): ParsePatchError =
            ParsePatchError(kind, null)
    }
}

/**
 * The kind of error that occurred when parsing a patch.
 */
sealed class ParsePatchErrorKind {
    /** Unexpected end of input. */
    data object UnexpectedEof : ParsePatchErrorKind()

    /** Multiple `---` lines found where only one expected. */
    data object MultipleOriginalHeaders : ParsePatchErrorKind()

    /** Multiple `+++` lines found where only one expected. */
    data object MultipleModifiedHeaders : ParsePatchErrorKind()

    /** Unable to parse filename from header line. */
    data object InvalidFilename : ParsePatchErrorKind()

    /** Filename line missing newline or tab terminator. */
    data object FilenameUnterminated : ParsePatchErrorKind()

    /** Invalid character in unquoted filename. */
    data object InvalidCharInUnquotedFilename : ParsePatchErrorKind()

    /** Expected escaped character in quoted filename. */
    data object ExpectedEscapedChar : ParsePatchErrorKind()

    /** Invalid escaped character in quoted filename. */
    data object InvalidEscapedChar : ParsePatchErrorKind()

    /** Invalid unescaped character in quoted filename. */
    data object InvalidUnescapedChar : ParsePatchErrorKind()

    /** Unable to parse hunk header (`@@ ... @@`). */
    data object InvalidHunkHeader : ParsePatchErrorKind()

    /** Hunk header missing closing `@@`. */
    data object HunkHeaderUnterminated : ParsePatchErrorKind()

    /** Unable to parse range in hunk header. */
    data object InvalidRange : ParsePatchErrorKind()

    /** Hunks are not in order or overlap. */
    data object HunksOutOfOrder : ParsePatchErrorKind()

    /** Hunk header line counts don't match actual content. */
    data object HunkMismatch : ParsePatchErrorKind()

    /** Expected end of hunk after `\ No newline at end of file`. */
    data object ExpectedEndOfHunk : ParsePatchErrorKind()

    /** Expected no more deleted lines in hunk. */
    data object TooManyDeletedLines : ParsePatchErrorKind()

    /** Expected no more inserted lines in hunk. */
    data object TooManyInsertedLines : ParsePatchErrorKind()

    /** Unexpected `\ No newline at end of file` marker. */
    data object UnexpectedNoNewlineMarker : ParsePatchErrorKind()

    /** Unexpected line in hunk body. */
    data object UnexpectedHunkLine : ParsePatchErrorKind()

    /** Missing newline at end of line. */
    data object MissingNewline : ParsePatchErrorKind()

    /** Orphaned hunk header found after trailing content. */
    data object OrphanedHunkHeader : ParsePatchErrorKind()

    /** Filename contains invalid UTF-8 when parsing as text. */
    data object InvalidUtf8Path : ParsePatchErrorKind()

    override fun toString(): String = when (this) {
        UnexpectedEof -> "unexpected EOF"
        MultipleOriginalHeaders -> "multiple '---' lines"
        MultipleModifiedHeaders -> "multiple '+++' lines"
        InvalidFilename -> "unable to parse filename"
        FilenameUnterminated -> "filename unterminated"
        InvalidCharInUnquotedFilename -> "invalid char in unquoted filename"
        ExpectedEscapedChar -> "expected escaped character"
        InvalidEscapedChar -> "invalid escaped character"
        InvalidUnescapedChar -> "invalid unescaped character"
        InvalidHunkHeader -> "unable to parse hunk header"
        HunkHeaderUnterminated -> "hunk header unterminated"
        InvalidRange -> "can't parse range"
        HunksOutOfOrder -> "hunks not in order or overlap"
        HunkMismatch -> "hunk header does not match hunk"
        ExpectedEndOfHunk -> "expected end of hunk"
        TooManyDeletedLines -> "expected no more deleted lines"
        TooManyInsertedLines -> "expected no more inserted lines"
        UnexpectedNoNewlineMarker -> "unexpected 'No newline at end of file' line"
        UnexpectedHunkLine -> "unexpected line in hunk body"
        MissingNewline -> "missing newline"
        OrphanedHunkHeader -> "orphaned hunk header after trailing content"
        InvalidUtf8Path -> "filename is not valid UTF-8"
    }
}
