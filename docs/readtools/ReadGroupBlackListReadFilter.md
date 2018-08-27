---
title: ReadGroupBlackListReadFilter
summary: Keep records not matching the read group tag and exact match string.
permalink: ReadGroupBlackListReadFilter.html
last_updated: 27-49-2018 03:49:16
---


## Description

Keep records not matching the read group tag and exact match string.

 <p>For example, this filter value:
   <code>PU:1000G-mpimg-080821-1_1</code>
 would filter out a read with the read group PU:1000G-mpimg-080821-1_1</p>

## Arguments

### Required Arguments

| Argument name(s) | Type | Description |
| :--------------- | :--: | :------ |
| `--readGroupBlackList` | List[String] | The name of the read group to filter out |


