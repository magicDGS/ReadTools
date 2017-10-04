---
title: FragmentLengthReadFilter
summary: Keep only read pairs with insert length less than or equal to the given value
permalink: FragmentLengthReadFilter.html
last_updated: 04-49-2017 12:49:37
---

## Description

Keep only read pairs (0x1) with absolute insert length less than or equal to the given value.
 Taking absolute values allows inclusion of pairs where the considered read's mate is at a smaller genomic coordinate.
 Insert length is the difference between the 5' outer ends of mates, akin to a SAM record's TLEN (column 9).
 Length is zero for single-end reads or when the information is unavailable.

## Arguments

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--maxFragmentLength`<br/>`-maxFragmentLength` | int | 1000000 | Maximum length of fragment (insert size) |


