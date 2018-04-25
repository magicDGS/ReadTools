---
title: CutReadTrimmer
summary: Crops a concrete number of bases at the end of the read.
permalink: CutReadTrimmer.html
last_updated: 25-37-2018 02:37:45
---

## Description

Crops a concrete number of bases in one or both sides of the read.

{% include warning.html content='It will be applied only if the reads are not further trimmed before the same
 number of bases.' %}

## Arguments

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--cut3primeBases`<br/>`-c3p` | Integer | null | Number of bases (in bp) to cut in the 3 prime of the read. For disable, use 'null'. |
| `--cut5primeBases`<br/>`-c5p` | Integer | null | Number of bases (in bp) to cut in the 5 prime of the read. For disable, use 'null'. |


