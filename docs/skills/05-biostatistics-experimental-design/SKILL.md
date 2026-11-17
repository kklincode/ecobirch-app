---
name: biostatistics-experimental-design
description: Design hierarchical sampling plans (Site -> Tree -> Leaf -> Replicate), compute tree-level variance, measurement error (ME%), statistical power, and blind measurement protocols.
---

# 05. Biostatistics & Experimental Design

## Purpose
Eliminate pseudoreplication, quantify measurement error ($ME$), and prevent confirmation bias during morphological data collection and site comparison.

## Design & Analysis Rules
1. **Hierarchical Sampling Structure:**
   - Track `Site (Territory) -> Tree (treeId) -> Leaf (leafId) -> Replicate (replicateOfLeafId)`.
   - Leaves within a single tree are non-independent subsamples; site-level means and 95% CIs must be computed over **tree means** ($df = n_{\text{trees}} - 1$) once $n_{\text{trees}} \ge 2$.
   - Flag pseudoreplication risk whenever $n_{\text{trees}} < 5$ even if $n_{\text{leaves}} \ge 10$.
2. **Measurement Error (ME) & Blind Replicates:**
   - Re-measure a random subset of leaves **blindly** (masked site, masked prior points, masked $Z$).
   - Compute $\sigma^2_{\text{ME}} = \frac{1}{2k}\sum (Z_1 - Z_2)^2$ and report $\text{ME}\% = 100 \cdot \sigma^2_{\text{ME}} / \sigma^2_{\text{total}}$. Require $\text{ME}\% < 25\%$ before interpreting small inter-site differences.
3. **Statistical Power & Variance:**
   - Do not treat fixed numbers (e.g., 30 trees × 100 leaves or 10 × 10) as universal magic formulas; update required $n_{\text{trees}}$ based on observed between-tree SD and target effect size $\Delta Z$.
   - Keep replicate records strictly separated from primary sample counts ($n_{\text{primary}}$).

## Rejection Criteria
- **Reject** pooling multiple leaves from 1–2 trees as an independent site estimate or counting duplicate ME measurements toward sample size $n$.
