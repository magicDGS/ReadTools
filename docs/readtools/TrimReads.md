---
title: TrimReads
summary: Applies a trimming pipeline to any kind of sources for ReadTools
permalink: TrimReads.html
---

## Description
Apply a trimming/filtering pipeline to the reads as following:
- Trimmers are applied in order. If any read is trimmed completely, the rest are ignored.
- Filter out completely trim reads.
- Apply the filters in order. If any read is filtered, the 'FT' tag reflects the reason.

Default arguments perform the same algorithm as the one described in [Kofler _et al._ (2011)]({{site.data.software.popoolation}}). Other features in the pipeline implemented there could be applied with some minor modifications in the command line.

Note: default trimmer(s)/filter(s) are applied before any other user-specified trimmer. If you would like to apply them in a different order, use --disableAllDefaultTrimmers in combination with the new ordering.

---

## Details
For details on how to integrate with the trimming pipeline described in [Kofler _et al._ (2011)]({{site.data.software.popoolation}}), see [PoPoolation Integration](popoolation.html).

Find more information about the pipeline and available trimmers/filters in [Trimming pipelines](trimming_pipelines.html).

---

## Arguments

Only tool-specific arguments are shown here. Please, check the [Common arguments](common_arguments.html) for other options.

### Required Arguments:

| Argument name(s) | Type | Summary |
| :--------------- | :--: |  :----- |
| --input,-I | String | BAM/SAM/CRAM/FASTQ source of reads. |
| --output,-O | String | Output SAM/BAM/CRAM file. |


### Optional Arguments:

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :------ |
| --addOutputSAMProgramRecord,-addOutputSAMProgramRecord | Boolean | true | If true, adds a PG tag to created SAM/BAM/CRAM files. |
| --createOutputBamIndex,-OBI | Boolean | true | If true, create a BAM/CRAM index when writing a coordinate-sorted BAM/CRAM file. |
| --createOutputBamMD5,-OBM | Boolean | false | If true, create a MD5 digest for any BAM/SAM/CRAM file created |
| --disable3pTrim,-D3PT | Boolean | false | Disable 3'-trimming. Cannot be true when argument disable5pTrim(D5PT) is true. |
| --disable5pTrim,-D5PT | Boolean | false | Disable 5'-trimming. May be useful for downstream mark of duplicate reads, usually identified by the 5' mapping position. Cannot be true when argument disable3pTrim(D3PT) is true. |
| --disableAllDefaultTrimmers,-disableAllDefaultTrimmers | Boolean | false | Disable all default trimmers. It may be useful to reorder the trimmers. |
| --disableReadFilter,-DF | String | null | Read filters to be disabled after trimming |
| --disableToolDefaultReadFilters,-disableToolDefaultReadFilters | Boolean | false | Disable all tool default read filters for trimming |
| --disableTrimmer,-DTM | String | null | Default trimmers to be disabled.  This argument may be specified 0 or more times. |
| --forceOverwrite,-forceOverwrite | Boolean | false | Force output overwriting if it exists |
| --input2,-I2 | String | null | BAM/SAM/CRAM/FASTQ the second source of reads (if pair-end). |
| --interleavedInput,-interleaved | Boolean | false | Interleaved input. |
| --keepDiscarded,-keepDiscarded | Boolean | false | Keep discarded reads in a separate file. Note: For pair-end input, this file contain also  mates of discarded reads (they do not have FT tag). |
| --readFilter,-RF | String | null | Read filters to be applied after trimming |
| --trimmer,-TM | String | null | Trimmers to be applied. Note: default trimmers are applied first and then the rest of them in order. |
