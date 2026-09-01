# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 13/13 (100.0%)
- **Function parity:** 148/197 matched (target 279) — 75.1%
- **Class/type parity:** 25/35 matched (target 99) — 71.4%
- **Combined symbol parity:** 173/232 matched (target 378) — 74.6%
- **Average inline-code cosine:** 0.55 (function body across 10 matched files)
- **Average documentation cosine:** 0.20 (doc text across 10 matched files)
- **Cheat-zeroed Files:** 3
- **Critical Issues:** 10 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. patch.parse

- **Target:** `patch.Error`
- **Similarity:** 0.29
- **Dependents:** 0
- **Priority Score:** 122607.1
- **Functions:** 12/23 matched (target 29)
- **Missing functions:** `fmt`, `convert_cow_to_str`, `patch_header`, `parse_filename`, `is_quoted`, `unescaped_filename`, `escaped_filename`, `range`, `test_escaped_filenames`, `test_missing_filename_header`, `adjacent_hunks_correctly_parse`
- **Types:** 2/3 matched (target 28)
- **Missing types:** `Result`
- **Tests:** 0/3 matched

### 2. patch.format

- **Target:** `patch.PatchFormatter`
- **Similarity:** 0.22
- **Dependents:** 0
- **Priority Score:** 101707.8
- **Functions:** 6/13 matched (target 9)
- **Missing functions:** `with_color`, `write_patch_into`, `write_hunk_into`, `write_line_into`, `default`, `write_into`, `fmt`
- **Types:** 1/4 matched (target 1)
- **Missing types:** `PatchDisplay`, `HunkDisplay`, `LineDisplay`

### 3. apply

- **Target:** `apply.Apply`
- **Similarity:** 0.44
- **Dependents:** 0
- **Priority Score:** 81905.6
- **Functions:** 9/15 matched (target 14)
- **Missing functions:** `fmt`, `inner`, `into_inner`, `is_patched`, `clone`, `next`
- **Types:** 2/4 matched
- **Missing types:** `Interleave`, `Item`

### 4. patch.mod

- **Target:** `patch.Patch [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 73110.0
- **Functions:** 19/25 matched (target 43)
- **Missing functions:** `clone`, `fmt`, `needs_to_be_escaped`, `as_ref`, `deref`, `len`
- **Types:** 5/6 matched (target 8)
- **Missing types:** `Target`

### 5. diff.myers

- **Target:** `diff.Myers`
- **Similarity:** 0.46
- **Dependents:** 0
- **Priority Score:** 51305.4
- **Functions:** 6/10 matched (target 9)
- **Missing functions:** `new`, `index`, `index_mut`, `fmt`
- **Types:** 2/3 matched
- **Missing types:** `Output`
- **Tests:** 1/1 matched

### 6. utils

- **Target:** `diffy.Utils`
- **Similarity:** 0.46
- **Dependents:** 0
- **Priority Score:** 42405.4
- **Functions:** 18/20 matched (target 40)
- **Missing functions:** `default`, `new`
- **Types:** 2/4 matched (target 6)
- **Missing types:** `Item`, `Text`

### 7. diff.mod

- **Target:** `diff.Diff [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 42010.0
- **Functions:** 13/17 matched (target 25)
- **Missing functions:** `clone`, `from`, `cow_str_to_bytes`, `set_original_and_modified_filenames`
- **Types:** 3/3 matched (target 7)
- **Missing types:** _none_
- **Tests:** 0/1 matched

### 8. merge.mod

- **Target:** `merge.Merge [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 32210.0
- **Functions:** 15/18 matched (target 17)
- **Missing functions:** `fmt`, `clone`, `shrink_front`
- **Types:** 4/4 matched (target 14)
- **Missing types:** _none_

### 9. diff.tests

- **Target:** `diff.DiffTests`
- **Similarity:** 0.55
- **Dependents:** 0
- **Priority Score:** 31504.5
- **Functions:** 12/15 matched (target 16)
- **Missing functions:** `range`, `same_diffs`, `diff_str`
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Tests:** 12/13 matched

### 10. merge.tests

- **Target:** `merge.MergeTests`
- **Similarity:** 0.50
- **Dependents:** 0
- **Priority Score:** 20605.0
- **Functions:** 4/6 matched (target 5)
- **Missing functions:** `same_merge`, `same_merge_bytes`
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Tests:** 4/4 matched

### 11. range

- **Target:** `diffy.Range`
- **Similarity:** 0.66
- **Dependents:** 0
- **Priority Score:** 13603.4
- **Functions:** 31/32 matched (target 69)
- **Missing functions:** `offset`
- **Types:** 4/4 matched (target 14)
- **Missing types:** _none_
- **Tests:** 3/3 matched

### 12. diff.cleanup

- **Target:** `diff.Cleanup`
- **Similarity:** 0.95
- **Dependents:** 0
- **Priority Score:** 300.5
- **Functions:** 3/3 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 13. lib

- **Target:** `diffy.Lib`
- **Similarity:** 1.00
- **Dependents:** 0
- **Priority Score:** 0.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 12)
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

