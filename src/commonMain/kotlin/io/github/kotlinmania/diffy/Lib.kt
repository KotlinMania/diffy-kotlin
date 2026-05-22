// port-lint: source src/lib.rs
package io.github.kotlinmania.diffy

/**
 * Tools for finding and manipulating differences between files
 *
 * ## Overview
 *
 * This library is intended to be a collection of tools used to find and
 * manipulate differences between files. Version control systems like Git and Mercurial generally
 * communicate differences between two versions of a file using a `diff` or
 * `patch`.
 *
 * The current diff implementation is based on the Myers' diff algorithm.
 *
 * The documentation generally refers to "files" in many places but none of
 * the apis explicitly operate on on-disk files. Instead this library
 * requires that the text being operated on resides in-memory and as such if
 * you want to perform operations on files, it is up to the user to load the
 * contents of those files into memory before passing their contents to the
 * apis provided by this library.
 *
 * ## UTF-8 and Non-UTF-8
 *
 * This library has support for working with both utf8 and non-utf8 texts.
 * Most of the APIs have two different variants, one for working with utf8
 * [String] texts (e.g. [io.github.kotlinmania.diffy.diff.createPatch]) and one for working with bytes [ByteArray]
 * which may or may not be utf8 (e.g. [io.github.kotlinmania.diffy.diff.createPatchBytes]).
 *
 * ## Creating a Patch
 *
 * A [io.github.kotlinmania.diffy.patch.Patch] between two texts can be created by doing the following:
 *
 * ```kotlin
 * import io.github.kotlinmania.diffy.diff.createPatch
 *
 * val original = "The Way of Kings\nWords of Radiance\n"
 * val modified = "The Way of Kings\nWords of Radiance\nOathbringer\n"
 *
 * val patch = createPatch(original, modified)
 * ```
 */

// Upstream re-exports from src/lib.rs:
// pub use apply::ApplyError;
// pub use apply::apply;
// pub use apply::apply_bytes;
// pub use diff::DiffOptions;
// pub use diff::create_patch;
// pub use diff::create_patch_bytes;
// pub use merge::ConflictStyle;
// pub use merge::MergeOptions;
// pub use merge::merge;
// pub use merge::merge_bytes;
// pub use patch::Hunk;
// pub use patch::HunkRange;
// pub use patch::Line;
// pub use patch::ParsePatchError;
// pub use patch::Patch;
// pub use patch::PatchFormatter;
//
// Users should import from original packages:
// - io.github.kotlinmania.diffy.diff.* for diff operations
// - io.github.kotlinmania.diffy.patch.* for patch types
// - io.github.kotlinmania.diffy.merge.* for merge operations (when ported)
// - io.github.kotlinmania.diffy.apply.* for apply operations (when ported)

// Cross-repo Rust callers requiring migration (from RUST_CALLERS.md):
// - codex-kotlin/codex-tui src/diff_render.rs:1 — use diffy::Hunk

// Callers migrated:
// (none yet - cannot migrate cross-repo callers; documented above for future work)
