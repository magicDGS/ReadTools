---
title: PlatformUnitReadFilter
summary: Keep reads that do not have blacklisted platform unit tags.
permalink: PlatformUnitReadFilter.html
last_updated: 19-58-2017 02:58:19
---

## Description

Keep reads that do not have blacklisted platform unit tags.
 Matching is done by exact case-sensitive text matching.

## Arguments

### Required Arguments

| Argument name(s) | Type | Description |
| :--------------- | :--: | :------ |
| `--blackListedLanes`<br/>`-blackListedLanes` | Set[String] | Keep reads with platform units not on the list |


