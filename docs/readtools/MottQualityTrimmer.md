---
title: MottQualityTrimmer
summary: Trims low quality ends using the Mott's algorithm.
permalink: MottQualityTrimmer.html
last_updated: 19-58-2017 02:58:19
---

## Description

Trims low quality ends by computing quality drops under a certain threshold using a modified
 version of the <a href="http://www.phrap.org/phredphrap/phred.html">Mott's algorithm.</a>.

## Arguments

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--mottQualityThreshold`<br/>`-mottQual` | int | 20 | The quality threshold to use for trimming. |


