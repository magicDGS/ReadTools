---
title: AlignmentAgreesWithHeaderReadFilter
summary: Filters out reads where the alignment does not match the contents of the header
permalink: AlignmentAgreesWithHeaderReadFilter.html
last_updated: 30-57-2018 12:57:55
---


## Description

Filter out reads where the alignment does not match the contents of the header.

 <p>The read does not match the contents of the header if:</p>

 <ul>
     <li>It is aligned to a non-existing contig</li>
     <li>It is aligned to a point after the end of the contig</li>
 </ul>

