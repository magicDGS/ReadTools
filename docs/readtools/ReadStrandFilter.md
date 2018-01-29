---
title: ReadStrandFilter
summary: Keep only reads whose strand is as specified
permalink: ReadStrandFilter.html
last_updated: 29-03-2018 04:03:32
---

## Description

Keep only reads whose strand is either forward (not 0x10) or reverse (0x10), as specified.

 <p>By default the filter keeps only forward reads (not 0x10).</p>

## Arguments

### Required Arguments

| Argument name(s) | Type | Description |
| :--------------- | :--: | :------ |
| `--keepReverse`<br/>`-keepReverse` | Boolean | Keep only reads on the reverse strand |


