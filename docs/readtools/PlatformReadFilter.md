---
title: PlatformReadFilter
summary: Keep only reads that match th PL attribute.
permalink: PlatformReadFilter.html
last_updated: 19-58-2017 02:58:19
---

## Description

Keep only reads that match th PL attribute.
 Matching is done by case-insensitive substring matching
 (checking if the read's platform tag contains the given string).

## Arguments

### Required Arguments

| Argument name(s) | Type | Description |
| :--------------- | :--: | :------ |
| `--PLFilterName`<br/>`-PLFilterName` | Set[String] | Keep reads with RG:PL attribute containing this string |


