---
title: ReadLengthReadFilter
summary: Keep only reads whose length is &ge; min value and &le; max value.
permalink: ReadLengthReadFilter.html
last_updated: 19-58-2017 02:58:19
---

## Description

Keep only reads whose length is &ge; min value and &le; max value.

## Arguments

### Required Arguments

| Argument name(s) | Type | Description |
| :--------------- | :--: | :------ |
| `--maxReadLength`<br/>`-maxReadLength` | Integer | Keep only reads with length at most equal to the specified value |

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--minReadLength`<br/>`-minReadLength` | int | 1 | Keep only reads with length at least equal to the specified value |


