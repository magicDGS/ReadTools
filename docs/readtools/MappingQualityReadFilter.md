---
title: MappingQualityReadFilter
summary: Keep only reads with mapping qualities within a specified range
permalink: MappingQualityReadFilter.html
last_updated: 04-49-2017 12:49:37
---

## Description

Keep only reads with mapping qualities within a specified range.

 <p>Note: this filter is not designed to handle the unavailable mapping quality (255).
 Use MappingQualityAvailableReadFilter to explicitly filter out reads with unavailable quality.

<i>See additional information in the following pages:</i>

- [MappingQualityAvailableReadFilter](MappingQualityAvailableReadFilter.html)

## Arguments

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--maximumMappingQuality`<br/>`-maximumMappingQuality` | Integer | null | Maximum mapping quality to keep (inclusive) |
| `--minimumMappingQuality`<br/>`-minimumMappingQuality` | int | 10 | Minimum mapping quality to keep (inclusive) |


