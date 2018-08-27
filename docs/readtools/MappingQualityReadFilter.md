---
title: MappingQualityReadFilter
summary: Keep only reads with mapping qualities within a specified range
permalink: MappingQualityReadFilter.html
last_updated: 27-49-2018 03:49:16
---


## Description

Keep only reads with mapping qualities within a specified range.

 <p>Note: this filter is not designed to handle the unavailable mapping quality (255).
 Use MappingQualityAvailableReadFilter to explicitly filter out reads with unavailable quality.</p>

<i>See additional information in the following pages:</i>

- [MappingQualityAvailableReadFilter](MappingQualityAvailableReadFilter.html)

## Arguments

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--maximumMappingQuality` | Integer | null | Maximum mapping quality to keep (inclusive) |
| `--minimumMappingQuality` | int | 10 | Minimum mapping quality to keep (inclusive) |


