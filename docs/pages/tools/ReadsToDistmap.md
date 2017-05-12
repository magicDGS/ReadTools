---
title: ReadsToDistmap
summary: Converts any kind of ReadTools source to Distmap format.
permalink: ReadsToDistmap.html
---

## Description
This tool converts SAM/BAM/CRAM/FASTQ formats into Distmap format from [Pandey & Schlötterer (PLoS ONE 8, 2013, e72614)]( http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0072614), including information from the barcodes (BC tag) in the read name (Illumina format) to allow keeping to some extend sample information if necessary.

If the source is a SAM/BAM/CRAM file and the barcodes are encoded in the read name, the option `--barcodeInReadName` should be used. If the barcode information is encoded in a different tag(s) the option `--rawBarcodeSequenceTags` should be used.

---

## Arguments

Only tool-specific arguments are shown here. Please, check the [Common arguments](common_arguments.html) for other options.

### Required Arguments:

| Argument name(s) | Type | Summary |
| :--------------- | :--: |  :----- |
| --input,-I | String | BAM/SAM/CRAM/FASTQ source of reads. |
| --output,-O | String | Output in Distmap format. Expected to be in an HDFS file system. |


### Optional Arguments:

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :------ |
| --barcodeInReadName,-barcodeInReadName | Boolean | false | Use the barcode encoded in SAM/BAM/CRAM read names. Note: this is not necessary for input FASTQ files. |
| --forceOverwrite,-forceOverwrite | Boolean | Force output overwriting if it exists |
| --input2,-I2 | String | BAM/SAM/CRAM/FASTQ the second source of reads (if pair-end). |
| --interleavedInput,-interleaved | Boolean | false | Interleaved input. |
| --rawBarcodeSequenceTags,-rawBarcodeSequenceTags | String | BC | Include the barcodes encoded in this tag(s) in the read name. Note: this is not necessary for input FASTQ files. WARNING: this tag(s) will be removed/updated as necessary. |

### Advanced Arguments:

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :----- |
| --hdfsBlockSize,-hdfsBlockSize | Integer | null | Block-size (in bytes) for files in HDFS. If not provided, use default configuration. |
