---
title: PlatformReadFilter
summary: Keep only reads with matching Read Group platform
permalink: PlatformReadFilter.html
last_updated: 25-37-2018 02:37:45
---

## Description

Keep only reads where the the Read Group platform attribute (RG:PL tag) contains the given string.

 <p>Note: Matching is done by case-insensitive substring matching.</p>

## Arguments

### Required Arguments

| Argument name(s) | Type | Description |
| :--------------- | :--: | :------ |
| `--PLFilterName`<br/>`-PLFilterName` | Set[String] | Platform attribute (PL) to match |


