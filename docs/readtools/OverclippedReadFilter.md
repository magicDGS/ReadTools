---
title: OverclippedReadFilter
summary: Filter out reads that are over-soft-clipped
permalink: OverclippedReadFilter.html
last_updated: 11-25-2018 03:25:45
---


## Description

Filter out reads where the number of bases without soft-clips (M, I, X, and = CIGAR operators) is lower than a threshold.

 <p>This filter is intended to filter out reads that are potentially from foreign organisms.
 From experience with sequencing of human DNA we have found cases of contamination by bacterial
 organisms; the symptoms of such contamination are a class of reads with only a small number
 of aligned bases and additionally many soft-clipped bases. This filter is intended
 to remove such reads.</p>

 <p>Note: Consecutive soft-clipped blocks are treated as a single block. For example, 1S2S10M1S2S is treated as 3S10M3S</p>

## Arguments

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--dontRequireSoftClipsBothEnds` | boolean | false | Allow a read to be filtered out based on having only 1 soft-clipped block. By default, both ends must have a soft-clipped block, setting this flag requires only 1 soft-clipped block |
| `--filterTooShort` | int | 30 | Minimum number of aligned bases |


