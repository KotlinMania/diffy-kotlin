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
 * [String] texts (e.g. [createPatch]) and one for working with bytes [ByteArray]
 * which may or may not be utf8 (e.g. [createPatchBytes]).
 *
 * ## Creating a Patch
 *
 * A [io.github.kotlinmania.diffy.patch.Patch] between two texts can be created by doing the following:
 *
 * ```kotlin
 * import io.github.kotlinmania.diffy.createPatch
 *
 * val original = "The Way of Kings\nWords of Radiance\n"
 * val modified = "The Way of Kings\nWords of Radiance\nOathbringer\n"
 *
 * val patch = createPatch(original, modified)
 * ```
 */

// Re-export public types from submodules
typealias Patch<T> = io.github.kotlinmania.diffy.patch.Patch<T>
typealias Hunk<T> = io.github.kotlinmania.diffy.patch.Hunk<T>
typealias HunkRange = io.github.kotlinmania.diffy.patch.HunkRange
typealias Line<T> = io.github.kotlinmania.diffy.patch.Line<T>
typealias ParsePatchError = io.github.kotlinmania.diffy.patch.ParsePatchError
typealias ParsePatchErrorKind = io.github.kotlinmania.diffy.patch.ParsePatchErrorKind

typealias DiffOptions = io.github.kotlinmania.diffy.diff.DiffOptions

// Re-export public functions as top-level functions
fun createPatch(original: String, modified: String): Patch<String> =
    io.github.kotlinmania.diffy.diff.createPatch(original, modified)

fun createPatchBytes(original: ByteArray, modified: ByteArray): Patch<ByteArray> =
    io.github.kotlinmania.diffy.diff.createPatchBytes(original, modified)
