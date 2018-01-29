---
title: AmbiguousBaseReadFilter
summary: Filters out reads that have greater than the threshold number of N bases
permalink: AmbiguousBaseReadFilter.html
last_updated: 29-03-2018 04:03:32
---

## Description

Filters out reads that have greater than the threshold number for unknown (N) bases.

## Arguments

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--ambigFilterBases` | Integer | null | Threshold number of ambiguous bases. If null, uses threshold fraction; otherwise, overrides threshold fraction. |
| `--ambigFilterFrac` | double | 0.05 | Threshold fraction of ambiguous bases |


