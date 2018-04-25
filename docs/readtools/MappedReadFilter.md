---
title: MappedReadFilter
summary: Filter out unmapped reads
permalink: MappedReadFilter.html
last_updated: 25-37-2018 02:37:45
---

## Description

Filter out unmapped reads.

 <p>Unmapped reads are defined by three criteria:</p>

 <ul>
     <li>SAM flag value 0x4</li>
     <li>An asterisk for reference name or RNAME (column 3 of a SAM record)</li>
     <li>A zero value for leftmost mapping position for POS (column 4 of SAM record)</li>
 </ul>

