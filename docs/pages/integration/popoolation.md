---
title: PoPoolation integration
sidebar: home_sidebar
permalink: popoolation.html
---

## PoPoolation trimming pipeline

[PoPoolation]({{site.data.software.popoolation}}) is a toolkit for handling Pool-Seq data, which includes a Perl script for performing a trimming pipeline similar to the one implemented here ([trim-fastq.pl](https://sourceforge.net/p/popoolation/code/HEAD/tree/trunk/basic-pipeline/trim-fastq.pl)).

All features implemented in that script could be mimic in `TrimReads` to apply the same trimming pipeline.

## Differences

The correspondence between arguments is the following:

| PoPoolation            | TrimReads                                                  | Note |
| :--------------------  | :--------------------------------------------------------- | :--- |
| Default                | Default                                                    | _ReadTools_ does not output single end in a separate file yet. This may be solved in the future |
| `--no-trim-quality`    | `--disableTrimmer MottQualityTrimmer`                      | |
| `--min-length`         | `--minReadLength`                                          | |
| `--no-5p-trim`         | `--disable5pTrim`                                          | |
| `--quality-threshold`    | `--mottQualityThreshold`                                   | |
| `--discard-internal-N` | `--readFilter AmbiguousBaseReadFilter --ambigFilterFrac 0` | This option is behaving slightly different. [PoPoolation] applies this filter before trimming the quality and [TrimReads](TrimReads.html) apply it afterwards |


[PoPoolation]: {{site.data.software.popoolation}}
