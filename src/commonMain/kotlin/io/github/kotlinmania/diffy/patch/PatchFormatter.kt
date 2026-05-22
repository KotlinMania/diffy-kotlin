// port-lint: source src/patch/format.rs
package io.github.kotlinmania.diffy.patch

/**
 * Formats patches for display or writing into byte streams.
 *
 * ## Examples
 *
 * ```kotlin
 * import io.github.kotlinmania.diffy.patch.PatchFormatter
 * import io.github.kotlinmania.diffy.diff.createPatch
 *
 * val patch = createPatch("alpha\nbeta\n", "ALPHA\nbeta\n")
 * val formatter = PatchFormatter().missingNewlineMessage(false)
 *
 * println(formatter.fmtPatch(patch))
 * // Output:
 * // --- original
 * // +++ modified
 * // @@ -1,2 +1,2 @@
 * // -alpha
 * // +ALPHA
 * //  beta
 * ```
 */
class PatchFormatter(
    private val withMissingNewlineMessage: Boolean = true,
    private val suppressBlankEmpty: Boolean = true,
) {
    /**
     * Sets whether to format a patch with a "No newline at end of file" message.
     *
     * Default is `true`.
     *
     * Note: If this is disabled by setting to `false`, formatted patches will no longer contain
     * sufficient information to determine if a file ended with a newline character (`\n`) or not
     * and the patch will be formatted as if both the original and modified files ended with a
     * newline character (`\n`).
     */
    fun missingNewlineMessage(enable: Boolean): PatchFormatter =
        PatchFormatter(
            withMissingNewlineMessage = enable,
            suppressBlankEmpty = suppressBlankEmpty,
        )

    /**
     * Sets whether to suppress printing of a space before empty lines.
     *
     * Defaults to `true`.
     *
     * For more information you can refer to the [Omitting trailing blanks](https://www.gnu.org/software/diffutils/manual/html_node/Trailing-Blanks.html)
     * manual page of GNU diff or the [diff.suppressBlankEmpty](https://git-scm.com/docs/git-diff#Documentation/git-diff.txt-codediffsuppressBlankEmptycode)
     * config for `git-diff`.
     */
    fun suppressBlankEmpty(enable: Boolean): PatchFormatter =
        PatchFormatter(
            withMissingNewlineMessage = withMissingNewlineMessage,
            suppressBlankEmpty = enable,
        )

    /**
     * Returns a formatted string representation of a [Patch]
     */
    fun fmtPatch(patch: Patch<String>): String = buildString {
        if (patch.originalPath() != null || patch.modifiedPath() != null) {
            patch.originalPath()?.let { original ->
                append("--- ")
                append(original.toString())
                append('\n')
            }
            patch.modifiedPath()?.let { modified ->
                append("+++ ")
                append(modified.toString())
                append('\n')
            }
        }

        for (hunk in patch.hunks()) {
            append(fmtHunk(hunk))
        }
    }

    /**
     * Formats a patch as bytes
     */
    fun fmtPatchBytes(patch: Patch<ByteArray>): ByteArray {
        val sb = StringBuilder()
        if (patch.originalPath() != null || patch.modifiedPath() != null) {
            patch.originalPath()?.let { original ->
                sb.append("--- ")
                original.writeInto(sb, original.value)
                sb.append('\n')
            }
            patch.modifiedPath()?.let { modified ->
                sb.append("+++ ")
                modified.writeInto(sb, modified.value)
                sb.append('\n')
            }
        }

        for (hunk in patch.hunks()) {
            sb.append(fmtHunkBytes(hunk))
        }
        return sb.toString().encodeToByteArray()
    }

    private fun fmtHunk(hunk: Hunk<String>): String = buildString {
        append("@@ -")
        append(hunk.oldRange().toString())
        append(" +")
        append(hunk.newRange().toString())
        append(" @@")

        hunk.functionContext()?.let { ctx ->
            append(" ")
            append(ctx)
        }
        append('\n')

        for (line in hunk.lines()) {
            append(fmtLine(line))
        }
    }

    private fun fmtHunkBytes(hunk: Hunk<ByteArray>): String = buildString {
        append("@@ -")
        append(hunk.oldRange().toString())
        append(" +")
        append(hunk.newRange().toString())
        append(" @@")

        hunk.functionContext()?.let { ctx ->
            append(" ")
            append(ctx.decodeToString())
        }
        append('\n')

        for (line in hunk.lines()) {
            append(fmtLineBytes(line))
        }
    }

    private fun fmtLine(line: Line<String>): String = buildString {
        val (sign, content) = when (line) {
            is Line.Context -> ' ' to line.value
            is Line.Delete -> '-' to line.value
            is Line.Insert -> '+' to line.value
        }

        if (suppressBlankEmpty && sign == ' ' && content == "\n") {
            append(content)
        } else {
            append(sign)
            append(content)
        }

        if (!content.endsWith('\n')) {
            append('\n')
            if (withMissingNewlineMessage) {
                append(NO_NEWLINE_AT_EOF)
                append('\n')
            }
        }
    }

    private fun fmtLineBytes(line: Line<ByteArray>): String = buildString {
        val (sign, content) = when (line) {
            is Line.Context -> ' ' to line.value
            is Line.Delete -> '-' to line.value
            is Line.Insert -> '+' to line.value
        }

        val contentStr = content.decodeToString()
        if (suppressBlankEmpty && sign == ' ' && contentStr == "\n") {
            append(contentStr)
        } else {
            append(sign)
            append(contentStr)
        }

        if (!contentStr.endsWith('\n')) {
            append('\n')
            if (withMissingNewlineMessage) {
                append(NO_NEWLINE_AT_EOF)
                append('\n')
            }
        }
    }

    companion object {
        /**
         * Construct a new formatter with default settings
         */
        fun new(): PatchFormatter = PatchFormatter()
    }
}
