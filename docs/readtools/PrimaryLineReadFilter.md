---
title: PrimaryLineReadFilter
summary: Keep only reads representing primary alignments (those that satisfy both the NotSecondaryAlignment and
 NotSupplementaryAlignment filters, or in terms of SAM flag values, must have neither of the 0x100 or
 0x800 flags set).
permalink: PrimaryLineReadFilter.html
last_updated: 04-49-2017 12:49:37
---

## Description

Keep only reads representing primary alignments (those that satisfy both the NotSecondaryAlignment and
 NotSupplementaryAlignment filters, or in terms of SAM flag values, must have neither of the 0x100 or
 0x800 flags set).

 Note that this filter represents a stronger criteria for "primary alignment" than the
 SAM flag 0x100 (representing ""not primary alignment" in some contexts).

 For example, a read that has only the supplementary flag (0x800) set, but not the secondary (0x100)
 flag will be filtered out from processing by the PrimaryLineReadFilter, but would NOT be filtered out by
 other software that uses the looser notion of "not primary" that only depends on the "secondary" flag being set.

