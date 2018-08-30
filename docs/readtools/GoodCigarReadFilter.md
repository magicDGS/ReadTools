---
title: GoodCigarReadFilter
summary: Keep only reads containing good CIGAR string
permalink: GoodCigarReadFilter.html
last_updated: 30-57-2018 12:57:55
---


## Description

Keep only reads containing good CIGAR strings.

 <p>Good CIGAR strings have the following properties:</p>

 <ul>
     <li>Valid according to the <a href="http://samtools.github.io/hts-specs/SAMv1.pdf">SAM specifications.</a></li>
     <li>Does not start or end with deletions (with or without preceding clips).</li>
     <li>Does not have consecutive deletion/insertion operators.</li>
 </ul>

