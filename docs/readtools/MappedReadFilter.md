---
title: MappedReadFilter
summary: Filter out unmapped reads
permalink: MappedReadFilter.html
last_updated: 04-49-2017 12:49:37
---

## Description

Filter out unmapped reads. Umapped reads are defined by three criteria:

 <ul>
     <li>SAM flag value 0x4</li>
     <li>An asterisk for reference name or RNAME (column 3 of a SAM record)</li>
     <li>A zero value for leftmost mapping position for POS (column 4 of SAM record)</li>
 </ul>

