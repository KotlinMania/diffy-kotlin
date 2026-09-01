// port-lint: source lib.rs
package io.github.kotlinmania.diffy

import io.github.kotlinmania.diffy.apply.ApplyError as ApplyApplyError
import io.github.kotlinmania.diffy.apply.apply as applyApply
import io.github.kotlinmania.diffy.apply.applyBytes as applyApplyBytes
import io.github.kotlinmania.diffy.diff.Diff as DiffDiff
import io.github.kotlinmania.diffy.diff.DiffOptions as DiffDiffOptions
import io.github.kotlinmania.diffy.diff.createPatch as diffCreatePatch
import io.github.kotlinmania.diffy.diff.createPatchBytes as diffCreatePatchBytes
import io.github.kotlinmania.diffy.diff.diff as diffDiff
import io.github.kotlinmania.diffy.merge.ConflictStyle as MergeConflictStyle
import io.github.kotlinmania.diffy.merge.MergeOptions as MergeMergeOptions
import io.github.kotlinmania.diffy.merge.merge as mergeMerge
import io.github.kotlinmania.diffy.merge.mergeBytes as mergeMergeBytes
import io.github.kotlinmania.diffy.patch.Hunk as PatchHunk
import io.github.kotlinmania.diffy.patch.HunkRange as PatchHunkRange
import io.github.kotlinmania.diffy.patch.Line as PatchLine
import io.github.kotlinmania.diffy.patch.ParsePatchError as PatchParsePatchError
import io.github.kotlinmania.diffy.patch.Patch as PatchPatch
import io.github.kotlinmania.diffy.patch.PatchFormatter as PatchPatchFormatter

typealias ApplyError = ApplyApplyError
typealias DiffOptions = DiffDiffOptions
typealias Diff<T> = DiffDiff<T>
typealias ConflictStyle = MergeConflictStyle
typealias MergeOptions = MergeMergeOptions
typealias Hunk<T> = PatchHunk<T>
typealias HunkRange = PatchHunkRange
typealias Line<T> = PatchLine<T>
typealias ParsePatchError = PatchParsePatchError
typealias Patch<T> = PatchPatch<T>
typealias PatchFormatter = PatchPatchFormatter

/**
 * diffy module root.
 */
public object Lib


