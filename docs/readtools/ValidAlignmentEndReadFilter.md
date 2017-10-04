---
title: ValidAlignmentEndReadFilter
summary: Keep only reads where the read end is properly aligned
permalink: ValidAlignmentEndReadFilter.html
last_updated: 04-49-2017 12:49:37
---

## Description

Keep only reads where the read end corresponds to a proper alignment -- that is, the read ends after the start
 (non-negative number of bases in the reference), calculated as:

 <p>
 <code>
 end - start + 1 &ge; 0<br>
 where<br>
  start = 1-based inclusive leftmost position of the clipped sequence (0 if no position)<br>
  end = 1-based inclusive rightmost position of the clipped sequence (0 if unmapped)<br>
 </code>

 <p>Note: keep also unmapped reads (align to zero bases in the reference). See MappedReadFilter for criteria defining an unmapped read.

<i>See additional information in the following pages:</i>

- [MappedReadFilter](MappedReadFilter.html)

