---
title: MetricsReadFilter
summary: Filter out reads that fail platform quality checks, are unmapped and represent secondary/supplementary alignments
permalink: MetricsReadFilter.html
last_updated: 27-49-2018 03:49:16
---


## Description

Filter out reads that:

 <ul>
     <li>Fail platform/vendor quality checks (0x200)</li>
     <li>Are unmapped (0x4)</li>
     <li>Represent secondary/supplementary alignments (0x100 or 0x800)</li>
 </ul>

