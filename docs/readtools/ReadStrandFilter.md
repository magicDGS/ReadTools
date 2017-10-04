---
title: ReadStrandFilter
summary: Keep only reads whose strand is as specified
permalink: ReadStrandFilter.html
last_updated: 04-49-2017 12:49:37
---

## Description

Keep only reads whose strand is either forward (not 0x10) or reverse (0x10), as specified. By default the filter keeps only forward reads (not 0x10).

## Arguments

### Required Arguments

| Argument name(s) | Type | Description |
| :--------------- | :--: | :------ |
| `--keepReverse`<br/>`-keepReverse` | Boolean | Keep only reads on the reverse strand |


