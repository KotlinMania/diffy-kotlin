# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 9/13 (69.2%)
- **Function parity:** 104/179 matched (target 208) — 58.1%
- **Class/type parity:** 17/31 matched (target 38) — 54.8%
- **Combined symbol parity:** 121/210 matched (target 246) — 57.6%
- **Average inline-code cosine:** 0.07 (function body across 4 matched files)
- **Average documentation cosine:** 0.37 (doc text across 4 matched files)
- **Cheat-zeroed Files:** 8
- **Critical Issues:** 9 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. patch.parse

- **Target:** `patch.Parse [PROVENANCE-FALLBACK]`
- **Similarity:** 0.27
- **Dependents:** 0
- **Priority Score:** 142607.3
- **Functions:** 11/23 matched (target 24)
- **Missing functions:** `new`, `fmt`, `convert_cow_to_str`, `patch_header`, `parse_filename`, `is_quoted`, `unescaped_filename`, `escaped_filename`, `range`, `test_escaped_filenames`, `test_missing_filename_header`, `adjacent_hunks_correctly_parse`
- **Types:** 1/3 matched
- **Missing types:** `Result`, `ParsePatchError`
- **Tests:** 0/3 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/patch/parse.rs` vs expected `patch/parse.rs`
- **Proposed provenance header:** `// port-lint: source patch/parse.rs` (current: `// port-lint: source src/patch/parse.rs`)
- **Lint issues:** 1

### 2. patch.format

- **Target:** `patch.PatchFormatter [ZERO] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 101710.0
- **Functions:** 6/13 matched (target 9)
- **Missing functions:** `with_color`, `write_patch_into`, `write_hunk_into`, `write_line_into`, `default`, `write_into`, `fmt`
- **Types:** 1/4 matched (target 1)
- **Missing types:** `PatchDisplay`, `HunkDisplay`, `LineDisplay`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/patch/format.rs` vs expected `patch/format.rs`
- **Proposed provenance header:** `// port-lint: source patch/format.rs` (current: `// port-lint: source src/patch/format.rs`)
- **Lint issues:** 1

### 3. diff.mod

- **Target:** `diff.Diff [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 92010.0
- **Functions:** 9/17 matched (target 19)
- **Missing functions:** `clone`, `from`, `new`, `set_compact`, `diff`, `cow_str_to_bytes`, `default`, `set_original_and_modified_filenames`
- **Types:** 2/3 matched
- **Missing types:** `Diff`
- **Tests:** 0/1 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/diff/mod.rs` vs expected `diff/mod.rs`
- **Proposed provenance header:** `// port-lint: source diff/mod.rs` (current: `// port-lint: source src/diff/mod.rs`)
- **Lint issues:** 1

### 4. patch.mod

- **Target:** `patch.Patch [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 73110.0
- **Functions:** 19/25 matched (target 32)
- **Missing functions:** `to_bytes`, `clone`, `fmt`, `needs_to_be_escaped`, `as_ref`, `deref`
- **Types:** 5/6 matched (target 8)
- **Missing types:** `Target`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/patch/mod.rs` vs expected `patch/mod.rs`
- **Proposed provenance header:** `// port-lint: source patch/mod.rs` (current: `// port-lint: source src/patch/mod.rs`)
- **Lint issues:** 1

### 5. diff.myers

- **Target:** `diff.Myers [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 51310.0
- **Functions:** 6/10 matched (target 9)
- **Missing functions:** `new`, `index`, `index_mut`, `fmt`
- **Types:** 2/3 matched
- **Missing types:** `Output`
- **Tests:** 1/1 matched
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/diff/myers.rs` vs expected `diff/myers.rs`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/diff/myers.rs` vs expected `diff/myers.rs`
- **Proposed provenance header:** `// port-lint: source diff/myers.rs` (current: `// port-lint: source src/diff/myers.rs`)
- **Proposed provenance header:** `// port-lint: source diff/myers.rs` (current: `// port-lint: source src/diff/myers.rs`)
- **TODOs:** 1
- **Lint issues:** 2

### 6. utils

- **Target:** `diffy.Utils [ZERO] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 42410.0
- **Functions:** 18/20 matched (target 40)
- **Missing functions:** `default`, `new`
- **Types:** 2/4 matched (target 6)
- **Missing types:** `Item`, `Text`
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/utils.rs` vs expected `utils.rs`
- **Proposed provenance header:** `// port-lint: source utils.rs` (current: `// port-lint: source src/utils.rs`)
- **Lint issues:** 1

### 7. range

- **Target:** `diffy.Range [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 3610.0
- **Functions:** 32/32 matched (target 72)
- **Missing functions:** _none_
- **Types:** 4/4 matched (target 14)
- **Missing types:** _none_
- **Tests:** 3/3 matched

### 8. diff.cleanup

- **Target:** `diff.Cleanup [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 310.0
- **Functions:** 3/3 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/diff/cleanup.rs` vs expected `diff/cleanup.rs`
- **Proposed provenance header:** `// port-lint: source diff/cleanup.rs` (current: `// port-lint: source src/diff/cleanup.rs`)
- **Lint issues:** 1

### 9. lib

- **Target:** `diffy.Lib [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only after fallback normalization: `src/lib.rs` vs expected `lib.rs`
- **Proposed provenance header:** `// port-lint: source lib.rs` (current: `// port-lint: source src/lib.rs`)
- **Lint issues:** 1

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `merge.mod` | `merge.Mod` | 0 | `merge/mod.rs` | `merge/Mod.kt` |

