---
title: ReadsToFastq
summary: Converts any kind of ReadTools source to FASTQ format.
permalink: ReadsToFastq.html
---

## Description
This tool converts SAM/BAM/CRAM/FASTQ formats into FASTQ, including information from the barcodes (BC tag) in the read name (Illumina format) to allow keeping to some extend sample information if necessary.

If the source is a SAM/BAM/CRAM file and the barcodes are encoded in the read name, the option `--barcodeInReadName` should be used. If the barcode information is encoded in a different tag(s) the option `--rawBarcodeSequenceTags` should be used.

---

## Arguments

Only tool-specific arguments are shown here. Please, check the [Common arguments](common_arguments.html) for other options.

### Required Arguments:

| Argument name(s) | Type | Summary |
| :--------------- | :--: |  :----- |
| --input,-I | String | BAM/SAM/CRAM/FASTQ source of reads. |
| --output,-O | String | Output FASTQ file prefix. |

### Optional Arguments:

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :------ |
| --barcodeInReadName,-barcodeInReadName | Boolean | false | Use the barcode encoded in SAM/BAM/CRAM read names. Note: this is not necessary for input FASTQ files. |
| --createOutputFastqMD5,-OFM | Boolean | false | If true, create a MD5 digest for FASTQ file(s). |
| --forceOverwrite,-forceOverwrite | Boolean | false | Force output overwriting if it exists |
| --input2,-I2 | String | null | BAM/SAM/CRAM/FASTQ the second source of reads (if pair-end). |
| --interleavedFastqOutput,-IFO | Boolean | false | If true, creates an interleaved FASTQ output. Otherwise, it will be splited by pairs/single end. |
| --interleavedInput,-interleaved | Boolean | false | Interleaved input. |
| --outputFormat,-outputFormat | FastqFormat | GZIP | FASTQ output format. |
| --rawBarcodeSequenceTags,-rawBarcodeSequenceTags | String | BC | Include the barcodes encoded in this tag(s) in the read name. Note: this is not necessary for input FASTQ files. WARNING: this tag(s) will be removed/updated as necessary.  This argument may be specified 0 or more times. |
