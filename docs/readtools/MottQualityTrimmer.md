---
title: MottQualityTrimmer
summary: Trims low quality ends using the Mott's algorithm.
permalink: MottQualityTrimmer.html
last_updated: 30-57-2018 12:57:55
---


## Description

Trims low quality ends by computing quality drops under a certain threshold using a modified
 version of the <a href="http://www.phrap.org/phredphrap/phred.html">Mott's algorithm.</a>.

## Arguments

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--mottQualityThreshold`<br/>`-mottQual` | int | 20 | Minimum average quality for the modified Mott algorithm. The threshold is used for calculating a score: <code>quality_at_base - threshold</code>. |


