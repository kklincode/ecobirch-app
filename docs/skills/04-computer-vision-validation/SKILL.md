---
name: computer-vision-validation
description: Validate leaf contour segmentation, keypoint/vein landmark detection, and Gemini/CV models using end-to-end morphometric error rather than contour IoU alone.
---

# 04. Computer Vision Validation

## Purpose
Ensure automated on-device (`LeafLandmarkDetector`) and optional cloud (`GeminiLeafAnalyzer`) models are evaluated on final morphometric trait accuracy and measurement error, not just silhouette segmentation.

## Validation Requirements
1. **Why IoU/Dice Is Insufficient:**
   - A 0.96 mask IoU does not verify petiole-blade junction (`BASE`), 1st vs 2nd secondary vein origins (`VEIN_1/2_BASE`), or marginal vein endpoints (`VEIN_1/2_END`) needed for Traits $Y_2\text{–}Y_5$.
2. **Benchmark Protocol:**
   - Evaluate against a **blinded expert-annotated benchmark** of real *Betula pendula* photos (varied lighting, leaf orientation, petiole thickness, camera aspect ratios).
   - Compute per-landmark Euclidean error (% of midrib length) and final trait errors: $|\Delta Y_1| \dots |\Delta Y_5|$, $|\Delta Z|$, and Trait 5 angular MAE (degrees).
3. **Bias & Reproducibility Gate:**
   - Test invariance to left/right horizontal mirroring (predicted $Z$ on mirrored image must match unmirrored image within tolerance $\le 0.002$).
   - Verify perspective distortion limits and camera shooting protocol (flat A4 background, camera parallel to leaf blade, optional physical reference calibration).

## Rejection Criteria
- **Reject** claiming automated accuracy without paired expert landmark MAE and mirror-symmetry invariance checks.
