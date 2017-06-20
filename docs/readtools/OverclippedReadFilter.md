---
title: OverclippedReadFilter
summary: Filter out reads that are over-soft-clipped
permalink: OverclippedReadFilter.html
last_updated: 19-58-2017 02:58:19
---

## Description

Filter out reads that are over-soft-clipped

 <p>
     This filter is intended to filter out reads that are potentially from foreign organisms.
     From experience with sequencing of human DNA we have found cases of contamination by bacterial
     organisms; the symptoms of such contamination are a class of reads with only a small number
     of aligned bases and additionally many soft-clipped bases.  This filter is intended
     to remove such reads. Consecutive soft-clipped blocks are treated as a single block
 </p>

## Arguments

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--dontRequireSoftClipsBothEnds`<br/>`-dontRequireSoftClipsBothEnds` | boolean | false | Allow a read to be filtered out based on having only 1 soft-clipped block. By default, both ends must have a soft-clipped block, setting this flag requires only 1 soft-clipped block. |
| `--filterTooShort`<br/>`-filterTooShort` | int | 30 | Value for which reads with less than this number of aligned bases is considered too short |


