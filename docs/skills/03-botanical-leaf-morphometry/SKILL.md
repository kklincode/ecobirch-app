---
name: botanical-leaf-morphometry
description: Enforce botanical identification standards for Betula pendula Roth., short-shoot sampling, landmark placement rules, and rejection criteria for damaged or hybrid leaves.
---

# 03. Botanical Leaf Morphometry

## Purpose
Prevent morphological error caused by sampling the wrong species (*Betula pubescens*), epicormic/long-shoot leaves, damaged blades, or anatomically inconsistent vein landmarks.

## Protocol & Landmark Rules
1. **Species Verification (*Betula pendula* Roth.):**
   - Drooping twigs, glabrous reddish-brown young shoots covered with resinous warts ("verrucose"), rhombic/triangular-ovate leaf blade with acuminate tip, doubly serrate margin, broadly cuneate base.
   - Exclude *Betula pubescens* (pubescent young shoots, rounded blade base) and obvious hybrids.
2. **Shoot & Crown Position:**
   - Sample exclusively from **short shoots** (*brachyblasts*) on the lower crown (~1.5–2.0 m), avoiding sun/shade extremes and vigorous water-sprout/long shoots (*auxiblasts*).
3. **Leaf Integrity Rejection Criteria:**
   - Reject leaves with missing apex/margins, mechanical tears, gall mites, fungal spots affecting width/vein endpoints, or severe herbivory.
4. **12-Landmark Anatomical Definitions:**
   - `BASE`: junction of blade base and petiole on the central midrib.
   - `TIP`: true botanical apex along the midrib axis.
   - `LEFT/RIGHT_VEIN_1_BASE` & `END`: departure of the 1st pair of secondary lateral veins from the midrib to the serrated margin tooth.
   - `LEFT/RIGHT_VEIN_2_BASE` & `END`: departure of the 2nd pair of secondary lateral veins from the midrib to the margin tooth; Trait 5 measures the angle between `BASE -> TIP` and `VEIN_2_BASE -> VEIN_2_END`.
   - `LEFT/RIGHT_WIDTH_MAX`: widest lateral excursion of left/right blade halves perpendicular to `BASE -> TIP`.

## Output & Rejection Criteria
- Require explicit operator confirmation (`leafIntegrityVerified`) and tree identifier (`treeId`) before saving a field specimen.
