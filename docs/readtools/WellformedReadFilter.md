---
title: WellformedReadFilter
summary: Keep only reads that are well-formed
permalink: WellformedReadFilter.html
last_updated: 27-49-2018 03:49:16
---


## Description

Tests whether a read is &quot;well-formed&quot; -- that is, is free of major internal inconsistencies and issues that could lead
 to errors downstream. If a read passes this filter, the rest of the engine should be able to process it without
 blowing up.

 <p><b>Well-formed reads definition</b></p>
 <ul>
     <li><b>Alignment coordinates:</b> start larger than 0 and end after the start position.</li>
     <li><b>Alignment agrees with header:</B> contig exists and start is within its range.</li>
     <li><b>Read Group and Sequence are present</b></li>
     <li><b>Consistent read length:</b> bases match in length with the qualities and the CIGAR string.</b></li>
     <li><b>Do not contain skipped regions:</b> represented by the 'N' operator in the CIGAR string.</li>
 </ul>

<i>See additional information in the following pages:</i>

- [ValidAlignmentStartReadFilter](ValidAlignmentStartReadFilter.html)
- [ValidAlignmentEndReadFilter](ValidAlignmentEndReadFilter.html)
- [AlignmentAgreesWithHeaderReadFilter](AlignmentAgreesWithHeaderReadFilter.html)
- [HasReadGroupReadFilter](HasReadGroupReadFilter.html)
- [MatchingBasesAndQualsReadFilter](MatchingBasesAndQualsReadFilter.html)
- [ReadLengthEqualsCigarLengthReadFilter](ReadLengthEqualsCigarLengthReadFilter.html)
- [SeqIsStoredReadFilter](SeqIsStoredReadFilter.html)
- [CigarContainsNoNOperator](CigarContainsNoNOperator.html)

