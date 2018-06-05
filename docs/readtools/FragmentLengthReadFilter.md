---
title: FragmentLengthReadFilter
summary: Keep only read pairs with insert length less than or equal to the given value
permalink: FragmentLengthReadFilter.html
last_updated: 05-39-2018 02:39:16
---


## Description

Keep only read pairs (0x1) with absolute insert length less than or equal to the given value.

 <p>Taking absolute values allows inclusion of pairs where the mate of the read being considered is at a smaller genomic coordinate.
 Insert length is the difference between the 5' outer ends of mates, akin to a SAM record's TLEN (column 9).
 Length is zero for single-end reads or when the information is unavailable.

## Arguments

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--maxFragmentLength` | int | 1000000 | Maximum length of fragment (insert size) |


