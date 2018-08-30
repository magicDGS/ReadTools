---
title: ReadStrandFilter
summary: Keep only reads whose strand is as specified
permalink: ReadStrandFilter.html
last_updated: 30-57-2018 12:57:55
---


## Description

Keep only reads whose strand is either forward (not 0x10) or reverse (0x10), as specified.

 <p>By default the filter keeps only forward reads (not 0x10).</p>

## Arguments

### Required Arguments

| Argument name(s) | Type | Description |
| :--------------- | :--: | :------ |
| `--keepReverseStrandOnly` | Boolean | Keep only reads on the reverse strand |


