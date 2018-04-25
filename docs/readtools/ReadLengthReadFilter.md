---
title: ReadLengthReadFilter
summary: Keep only reads whose length is within a certain range
permalink: ReadLengthReadFilter.html
last_updated: 25-37-2018 02:37:45
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


