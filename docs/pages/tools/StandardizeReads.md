---
title: StandardizeReads
summary: Standardize quality and format for all kind of sources for ReadTools.
permalink: StandardizeReads.html
---

## Description
This tool standardize the format of reads from both raw and mapped reads and outputs a SAM/BAM/CRAM file:
- Quality encoding: the Standard quality is Sanger. Quality is detected automatically, but is could be forced with `--forceEncoding`
- Raw barcodes: the BC/QT tags will be updated if requested by the barcode options. This options may be useful if the information for the raw barcodes is present in a different tag (e.g., while using illumina2bam with double indexing) or it was not de-multiplexed before mapping using FASTQ file (e.g., barcodes should be encoded in the read name if mapping with DistMap on a cluster). Note: If several indexes are present, barcodes are separated by hyphens and qualities by space.
- FASTQ file(s): the output is a unmapped SAM/BAM/CRAM file with the quality header in the CO tag and the PF binary tag if the read name is in the Casava format. The raw barcode (BC) is extracted from the read name if present (does not require any barcode option).

---

## Arguments

Only tool-specific arguments are shown here. Please, check the [Common arguments](common_arguments.html) for other options.

### Required Arguments:

| Argument name(s) | Type | Summary |
| :--------------- | :--: |  :----- |
| --input,-I | String | BAM/SAM/CRAM/FASTQ source of reads. |
| --output,-O | String  | Output SAM/BAM/CRAM file. |

### Optional Arguments:

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :------ |
| --addOutputSAMProgramRecord,-addOutputSAMProgramRecord | Boolean | true | If true, adds a PG tag to created SAM/BAM/CRAM files. |
| --barcodeInReadName,-barcodeInReadName | Boolean | false | Use the barcode encoded in SAM/BAM/CRAM read names. Note: this is not necessary for input FASTQ files. |
| --createOutputBamIndex,-OBI | Boolean | true | If true, create a BAM/CRAM index when writing a coordinate-sorted BAM/CRAM file. |
| --createOutputBamMD5,-OBM | Boolean | false | If true, create a MD5 digest for any BAM/SAM/CRAM file created |
| --forceOverwrite,-forceOverwrite | Boolean | false | Force output overwriting if it exists |
| --input2,-I2 | String | null | BAM/SAM/CRAM/FASTQ the second source of reads (if pair-end). |
| --interleavedInput,-interleaved | Boolean | false | Interleaved input. |
| --rawBarcodeQualityTag,-rawBarcodeQualityTag | String | null | Use the qualities encoded in this tag(s) as raw barcode qualities. Requires --rawBarcodeSequenceTags. WARNING: this tag(s) will be removed/updated as necessary. |
| --rawBarcodeSequenceTags,-rawBarcodeSequenceTags | String | BC | Include the barcodes encoded in this tag(s) in the read name. Note: this is not necessary for input FASTQ files. WARNING: this tag(s) will be removed/updated as necessary. |
