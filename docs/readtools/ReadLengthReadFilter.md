---
title: ReadLengthReadFilter
summary: Keep only reads whose length is within a certain range
permalink: ReadLengthReadFilter.html
last_updated: 05-39-2018 02:39:16
---


## Description

Keep only reads whose length is &ge; min value and &le; max value.

## Arguments

### Required Arguments

| Argument name(s) | Type | Description |
| :--------------- | :--: | :------ |
| `--maxReadLength` | Integer | Keep only reads with length at most equal to the specified value |

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--minReadLength` | int | 1 | Keep only reads with length at least equal to the specified value |


