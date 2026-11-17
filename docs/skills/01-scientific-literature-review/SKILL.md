---
name: scientific-literature-review
description: Verify botanical, ecological, and biostatistical claims about fluctuating asymmetry (FA) and Betula pendula against peer-reviewed literature, DOIs, and replication evidence.
---

# 01. Scientific Literature Review

## Purpose
Ensure every biological claim, threshold, formula, and limitation stated in documentation or app UI is anchored to verified primary literature rather than secondary folklore.

## Execution Protocol
1. Search primary indexing databases (Crossref, OpenAlex, PubMed, Wiley/Springer/MDPI/PeerJ).
2. Extract exact bibliographic metadata for every cited study: authors, year, journal, volume/pages, DOI, species studied, sample sizes, and blinding/precision controls.
3. Explicitly cross-check **both supportive and critical/null studies** (never cherry-pick supportive local studies while omitting meta-analyses or blinded experiments).
4. Quarantine unverified claims, broken links, or secondary citations without primary-source verification.

## Baseline Reference Corpus for Betula pendula FA
- **Májeková et al. (2024)** — *Functional Ecology*, `10.1111/1365-2435.14564` (21 species, 80 populations; 51-study review showing 53% no effect vs 39% increase).
- **Graham (2021)** — *Symmetry*, `10.3390/sym13010009` (experimental design, ME, size scaling, DA/AS checks).
- **Kozlov (2015, 2017, 2025)** — `10.7717/peerj.1027`, `10.1134/S1067413617010106`, `10.1111/1365-2435.14713` (reproducibility = 0.074, confirmation bias, requirement for blind repeated measurements).
- **Erofeeva & Yakimov (2020)** — *Symmetry*, `10.3390/sym12050727` (shifts between FA, DA, and AS under pollution).
- **Adamovich & Olkova (2024)** — `10.12731/2658-6649-2024-16-3-840` (high FA 0.064–0.076 without pollutant exceedance; soil/pH associations).
- **Palmer & Strobeck (1986)** — *Annu. Rev. Ecol. Syst.*, 17:391–421 (statistical separation of FA, DA, AS, and ME).

## Output & Rejection Criteria
- **Reject** any citation lacking a verified DOI/journal or any claim extrapolating one local study as universal proof.
- Produce a Markdown verification table listing `Source | DOI | Sample/Species | Precision/Blinding | Takeaway & Caveats`.
