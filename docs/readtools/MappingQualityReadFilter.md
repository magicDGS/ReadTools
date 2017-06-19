---
title: MappingQualityReadFilter
summary: Keep reads with mapping qualities within a specified range.
permalink: MappingQualityReadFilter.html
last_updated: 19-58-2017 02:58:19
---

## Description

Keep reads with mapping qualities within a specified range.

 Note: this filter does not handle specially the unavailable mapping quality (org.broadinstitute.hellbender.utils.QualityUtils#MAPPING_QUALITY_UNAVAILABLE).
 Use org.broadinstitute.hellbender.engine.filters.ReadFilterLibrary.MappingQualityAvailableReadFilter to explicitly filter out reads with unavailable quality.

## Arguments

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--maximumMappingQuality`<br/>`-maximumMappingQuality` | Integer | null | Maximum mapping quality to keep (inclusive) |
| `--minimumMappingQuality`<br/>`-minimumMappingQuality` | int | 10 | Minimum mapping quality to keep (inclusive) |


