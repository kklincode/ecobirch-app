---
name: scientific-qa-reporting
description: Audit reproducibility of calculations, schema/export integrity, test coverage, limitation disclosures, and consistency across code, UI text, and documentation.
---

# 07. Scientific QA & Reporting

## Purpose
Verify end-to-end reproducibility across domain math, database schema, JSON exports, UI labels, tests, and scientific documentation before any build or manuscript release.

## QA Gate Checklist
1. **Reproducibility & Raw Data Completeness:**
   - Verify JSON exports include raw 12 `LeafPoints`, `aspectRatio`, `axisReferenceLengthMm`, `xLeft`, `xRight`, `treeId`, `replicateOfLeafId`, `isBlindMeasurement`, `leafIntegrityVerified`, and `detectionSource`.
   - Verify demo datasets are isolated (`isDemo = true`) and excluded from scientific exports by default.
2. **Code-to-Copy Consistency:**
   - Check that UI labels match domain math (no calling single-leaf $Z$ "FA" or displaying hidden calibration scores in blind mode).
   - Check that `MethodologyScreen`, `README.md`, and `SCIENTIFIC_SPEC_V3.md` cite accurate DOIs and identical thresholds.
3. **Automated Verification:**
   - Unit tests cover symmetry invariance, scale invariance, DA/AS detection, size-correlation screening, hierarchical tree-level stats, and measurement error ($ME\%$).
   - Integration tests cover Room v1->v2 migration & JSON round-trip.
   - UI E2E tests verify blind mode masking, replicate ME flow, tree selection, and Zakharov scale toggle.

## Output
Produce a pass/fail QA matrix across Domain Math, Biostatistics, Persistence/Export, UI Semantics, and Build/Lint before approving release artifacts.
