---
title: QualityEncodingDetector
summary: Detects the quality encoding format for all kind of sources for ReadTools.
permalink: QualityEncodingDetector.html
---

## Description
Detects the quality encoding for a SAM/BAM/CRAM/FASTQ files, output to the STDOUT the quality encoding.

---

## Arguments

Only tool-specific arguments are shown here. Please, check the [Common arguments](common_arguments.html) for other options.

### Required Arguments:

| Argument name(s) | Type | Summary |
| :--------------- | :--: |  :----- |
| --input,-I | String | Reads input. |


### Optional Arguments:

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :------ |
| --maximumReads,-maximumReads | Long | 1000000 |  Maximum number of reads to use for detect the quality encoding |
