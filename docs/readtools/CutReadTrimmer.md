---
title: CutReadTrimmer
summary: Crops a concrete number of bases at the end of the read.
permalink: CutReadTrimmer.html
last_updated: 19-58-2017 02:58:19
---

## Description

Crops a concrete number of bases in one or both sides of the read.

{% include warning.html content='It will be applied only if the reads are not further trimmed before the same
 number of bases.' %}

## Arguments

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--cut3primeBases`<br/>`-c3p` | Integer | null | The number of bases from the 3 prime of the read to trim. |
| `--cut5primeBases`<br/>`-c5p` | Integer | null | The number of bases from the 5 prime of the read to trim. |


