---
title: PlatformUnitReadFilter
summary: Filter out reads with matching platform unit attribute
permalink: PlatformUnitReadFilter.html
last_updated: 30-57-2018 12:57:55
---


## Description

Filter out reads where the the platform unit attribute (PU tag) contains the given string.

 <p>Note: Matching is done by exact case-sensitive text matching.</p>

## Arguments

### Required Arguments

| Argument name(s) | Type | Description |
| :--------------- | :--: | :------ |
| `--blackListedLanes` | Set[String] | Platform unit (PU) to filter out |


