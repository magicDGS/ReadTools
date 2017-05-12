---
title: Trimmer Description
sidebar: home_sidebar
permalink: trimmers.html
---

## CutReadTrimmer
Trimmer for crop some bases in one or both sides of the read.

### Optional Arguments:

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :------ |
| --cut5primeBases,-cp5 | Integer | null | Number of bases (in bp) to cut in the 5 prime of the read. For disable, use 'null'. |
| --cut3primeBases,-cp3 | Integer | null | Number of bases (in bp) to cut in the 3 prime of the read. For disable, use 'null'. |

---

## MottQualityTrimmer
Computes trim points for quality drop under a certain threshold using the Mott algorithm.

### Optional Arguments:

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :------ |
| --mottQualityThreshold,-mottQual | Integer | 20 | Minimum average quality for the modified Mott algorithm. The threshold is used for calculating a score: quality_at_base - threshold. |

---

## TrailingNtrimmer
Trim trailing Ns on the read sequence.
