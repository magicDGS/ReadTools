---
title: AssignReadGroupByBarcode
summary: Assigns read groups based on barcode tag(s) for all kind of sources for ReadTools.
permalink: AssignReadGroupByBarcode.html
---

## Description
Assigns the read groups present in the file(s) based on the barcode present in the raw barcode tag(s). Read groups are assigned by matching the ones provided in the barcode file against the present in the tag(s), allowing mismatches and unknown bases (Ns) in the sequence. Ambiguous barcodes, defined as the ones that have a concrete distance with the second match (at least one mismatch of difference), are also discarded. If several indexed are used and none of them identify uniquely the read group, the read group is assigned by majority vote.

Note: for pair-end reads, only one read is used to assign the barcode.

WARNING: If several barcodes are present and one of them identify uniquely the read group, this is assigned directly. Thus, it is recommended to provide all the barcodes present in the library to the parameter.

---

## Arguments

Only tool-specific arguments are shown here. Please, check the [Common arguments](common_arguments.html) for other options.

### Required Arguments:

| Argument name(s) | Type | Summary |
| :--------------- | :--: |  :----- |
| --barcodeFile,-bc | String | Tab-delimited file with header for barcode sequences ('barcode_sequence' or 'barcode_sequence_1' for the first barcode, 'barcode_sequence_$(number)' for subsequent if more than one index is used), sample name ('sample_name' or 'barcode_name') and, optionally, library name ('library_name').  Barcode file will overwrite any of Read Group arguments for the same information. WARNING: this file should contain all the barcodes present in the multiplexed file. |
| --input,-I | String | BAM/SAM/CRAM/FASTQ source of reads. |
| --output,-O | String | Output SAM/BAM/CRAM file prefix. |


### Optional Arguments:

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :------ |
| --addOutputSAMProgramRecord,-addOutputSAMProgramRecord | Boolean | true | If true, adds a PG tag to created SAM/BAM/CRAM files. |
| --barcodeInReadName,-barcodeInReadName | Boolean | false | Use the barcode encoded in SAM/BAM/CRAM read names. Note: this is not necessary for input FASTQ files. |
| --createOutputBamIndex,-OBI | Boolean | true | If true, create a BAM/CRAM index when writing a coordinate-sorted BAM/CRAM file. |
| --createOutputBamMD5,-OBM | Boolean | false | If true, create a MD5 digest for any BAM/SAM/CRAM file created |
| --forceOverwrite,-forceOverwrite | Boolean | Force output overwriting if it exists |
| --input2,-I2 | String | null | BAM/SAM/CRAM/FASTQ the second source of reads (if pair-end). |
| --interleavedInput,-interleaved | Boolean | false | Interleaved input. |
| --keepDiscarded,-keepDiscarded | Boolean | false | Keep reads does not assigned to any record in a separate file. |
| --maximumMismatches,-mm | Integer | 0 | Maximum number of mismatches allowed for a matched barcode. Specify more than once for apply a different threshold to several indexes. |
| --maximumN,-maxN | Integer | null | Maximum number of Ns allowed in the barcode to consider them. If null, no threshold will be applied. |
| --minimumDistance,-md | Integer | 1 | Minimum difference in  number of mismatches between the best match and the second. Specify more than once for apply a different threshold to several indexes. |
| --nNoMismatch,-nnm | Boolean | false | Do not count unknown bases (Ns) as mismatch. |
| --outputFormat,-outputFormat | BamFormat | BAM | SAM/BAM/CRAM output format. |
| --rawBarcodeSequenceTags,-rawBarcodeSequenceTags | String | BC | Include the barcodes encoded in this tag(s) in the read name. Note: this is not necessary for input FASTQ files. WARNING: this tag(s) will be removed/updated as necessary. |
| --RGCN,-CN | String | null | Read Group sequencing center name |
| --RGDT,-DT | Iso8601Date | null | Read Group run date |
| --RGLB,-LB | String | null | Read Group Library |
| --RGPI,-PI | Integer | null | Read Group predicted insert size |
| --RGPL,-PL | PlatformValue | null | Read Group platform (e.g. illumina, solid) |
| --RGPM,-PM | String | null | Read Group platform model |
| --RGPU,-PU | String | null | Read Group platform unit (eg. run barcode) |
| --runName,-runName | String | null | Run name to add to the ID in the read group information. |
| --splitLibraryName | Boolean | false | Split file by library. |
| --splitReadGroup | Boolean | false | Split file by read group. |
| --splitSample | Boolean | false | Split file by sample. |
