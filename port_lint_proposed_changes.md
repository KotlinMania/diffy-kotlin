# port-lint Proposed Changes

**Generated:** 2026-05-24
**Source:** tmp/diffy/src
**Target:** src/commonMain/kotlin/io/github/kotlinmania/diffy

These are review proposals only. They are emitted when a Rust -> Kotlin pair matches only after fallback normalization, so the existing `port-lint` header is not an exact provenance match.

| Target file | Current header | Proposed header | Source path | Reason |
|-------------|----------------|-----------------|-------------|--------|
| `src/commonMain/kotlin/io/github/kotlinmania/diffy/patch/Parse.kt` | `// port-lint: source src/patch/parse.rs` | `// port-lint: source patch/parse.rs` | `patch/parse.rs` | `port-lint provenance header matched only after fallback normalization: 'src/patch/parse.rs' vs expected 'patch/parse.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/diffy/patch/PatchFormatter.kt` | `// port-lint: source src/patch/format.rs` | `// port-lint: source patch/format.rs` | `patch/format.rs` | `port-lint provenance header matched only after fallback normalization: 'src/patch/format.rs' vs expected 'patch/format.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/diffy/diff/Diff.kt` | `// port-lint: source src/diff/mod.rs` | `// port-lint: source diff/mod.rs` | `diff/mod.rs` | `port-lint provenance header matched only after fallback normalization: 'src/diff/mod.rs' vs expected 'diff/mod.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/diffy/patch/Patch.kt` | `// port-lint: source src/patch/mod.rs` | `// port-lint: source patch/mod.rs` | `patch/mod.rs` | `port-lint provenance header matched only after fallback normalization: 'src/patch/mod.rs' vs expected 'patch/mod.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/diffy/diff/Myers.kt` | `// port-lint: source src/diff/myers.rs` | `// port-lint: source diff/myers.rs` | `diff/myers.rs` | `port-lint provenance header matched only after fallback normalization: 'src/diff/myers.rs' vs expected 'diff/myers.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/diffy/diff/MyersTests.kt` | `// port-lint: source src/diff/myers.rs` | `// port-lint: source diff/myers.rs` | `diff/myers.rs` | `port-lint provenance header matched only after fallback normalization: 'src/diff/myers.rs' vs expected 'diff/myers.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/diffy/Utils.kt` | `// port-lint: source src/utils.rs` | `// port-lint: source utils.rs` | `utils.rs` | `port-lint provenance header matched only after fallback normalization: 'src/utils.rs' vs expected 'utils.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/diffy/diff/Cleanup.kt` | `// port-lint: source src/diff/cleanup.rs` | `// port-lint: source diff/cleanup.rs` | `diff/cleanup.rs` | `port-lint provenance header matched only after fallback normalization: 'src/diff/cleanup.rs' vs expected 'diff/cleanup.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/diffy/Lib.kt` | `// port-lint: source src/lib.rs` | `// port-lint: source lib.rs` | `lib.rs` | `port-lint provenance header matched only after fallback normalization: 'src/lib.rs' vs expected 'lib.rs'` |
