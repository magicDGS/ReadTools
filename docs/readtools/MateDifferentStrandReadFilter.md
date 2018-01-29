---
title: MateDifferentStrandReadFilter
summary: Keep only reads with mates mapped on the different strand
permalink: MateDifferentStrandReadFilter.html
last_updated: 29-03-2018 04:03:32
---

## Description

For paired reads (0x1), keep only reads that are mapped, have a mate that is mapped (read is not 0x8), and both
 the read and its mate are on different strands (when read is 0x20, it is not 0x10), as is the typical case.

 <p>See MappedReadFilter for criteria defining an mapped read.</p>

<i>See additional information in the following pages:</i>

- [MappedReadFilter](MappedReadFilter.html)

