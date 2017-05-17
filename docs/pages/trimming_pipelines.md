---
title: Trimming Pipelines
sidebar: home_sidebar
permalink: trimming_pipelines.html
---

[TrimReads](TrimReads.html) applies a trimming/filtering pipeline that can be highly customized by the user. The tool includes default trimmers/filters, but they could be disabled or other ones included. The order of the pipeline is the following:

1. Default trimmers
2. User-specified trimmers
3. Default filters
4. User-specified filters

This order is important, because some trimmers would not apply in some situations. For example, if the read is already trimmed in a right-most position for 5' when it pass to a trimmer, the 5' is not trimmer further. For reordering defaults, specify `--disableAllDefaultTrimmers` and provide them in the new order.

The following trimmers and filters could be applied in the trimming pipeline:

* Trimmers. For detailed information of each trimmer, please go to [Trimmer description](trimmers.html).
  - [CutReadTrimmer](trimmers.html#cutreadtrimmer): Trimmer for crop some bases in one or both sides of the read.
  - [MottQualityTrimmer](trimmers.html#mottqualitytrimmer): Computes trim points for quality drop under a certain threshold using the Mott algorithm.
  - [TrailingNtrimmer](trimmers.html#trailingntrimmer): Trim trailing Ns on the read sequence.

* Filters
  - Filters uses in _ReadTools_ are implemented in GATK4, which is still unreleased. We will include their documentation as soon as they make it available.
