---
name: fa-methodology-auditor
description: Audit mathematical formulas, trait definitions, signed vs unsigned asymmetry handling, DA/AS screening, and scale threshold provenance in morphometry codebases.
---

# 02. FA Methodology Auditor

## Purpose
Verify that the mathematical engine implements clean geometry, separates raw relative asymmetry indices from validated fluctuating asymmetry (FA), and avoids misrepresenting unvalidated thresholds.

## Audit Checklist
1. **Formulas & Units:**
   - Confirm $Y_i = |X_{Li} - X_{Ri}| / (X_{Li} + X_{Ri})$ is labeled *"Relative Asymmetry Index"* ($Z$), not unconditionally labeled *"Fluctuating Asymmetry (FA)"*.
   - Confirm signed differences ($d_i = X_{Li} - X_{Ri}$ and $A_{si} = (X_{Li}-X_{Ri})/(X_{Li}+X_{Ri})$) are preserved alongside $|Y_i|$.
   - Confirm isotropic aspect-ratio handling and absence of hardcoded pixel-to-mm magic constants.
2. **Pre-Interpretation Statistical Screening:**
   - **Directional Asymmetry (DA):** Check $H_0: \mu(A_{si}) = 0$ via one-sample $t$-test per trait.
   - **Antisymmetry (AS):** Check excess kurtosis ($\gamma_2 < -1.0$) / bimodality per trait.
   - **Size Dependence:** Check correlation between $|A_{si}|$ and leaf midrib axis length.
   - **Measurement Error (ME):** Require replicate pairs before claiming subtle between-site differences.
3. **Threshold Provenance & Configurability:**
   - Verify whether classification thresholds (e.g. Zakharov 2000 `<0.040 / 0.040–0.044 / 0.045–0.049 / 0.050–0.054 / >0.054`) are explicitly cited, optional/toggleable, and disabled or caveated when statistical checks fail.

## Output & Rejection Criteria
- **Reject** any code path that labels a single leaf with an "environmental pollution score" or aggregates $Y_1..Y_5$ into a site "FA" score without exposing DA/AS/ME diagnostics.
