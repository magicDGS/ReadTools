---
title: StandardizeReads
summary: Standardizes quality and format for all kind of sources for ReadTools.
permalink: StandardizeReads.html
last_updated: 27-49-2018 03:49:16
---


## Description

General tool for standardize any kind of read source (both raw and mapped reads).

 <p>This tool outputs a SAM/BAM/CRAM file as defined in the
 <a href="http://samtools.github.io/hts-specs/SAMv1.pdf">SAM specifications</a>:</p>

 <ul>

 <li><b>Quality encoding</b>: the Standard quality is Sanger. Quality is detected automatically,
 but it could be forced with <code>--forceEncoding</code></li>

 <li><b>Raw barcodes</b>: the standard barcode tags are BC for the sequence and QT for the
 quality. To correctly handle information in a SAM/BAM/CRAM file with misencoded barcode tags,
 one of the following options could be used:

 <ul>

 <li>Barcodes in the read name: use <code>--barcodeInReadName</code> option. This may be useful
 for files produced by mapping a multiplexed library stored as FASTQ files. </li>

 <li>Barcodes in a different tag(s): use <code>--rawBarcodeSequenceTags</code>. This may be
 useful
 if the barcode is present in a different tag (e.g., when using <a
 href="http://gq1.github.io/illumina2bam/">illumina2bam</a> with
 dual indexing, the second index will be in the B2 tag)</li>

 </ul></li>

 <li><b>FASTQ file(s)</b>: the output is an unmapped SAM/BAM/CRAM file with the quality header
 added to the CO tag. The raw barcode is extracted from the read name if present independently of
 the read name encoding (Casava or Illumina legacy).
 In the case of the Casava's read name encoding, the PF binary tag is also updated.</li>

 </ul>

{% include warning.html content='If several barcode indexes are present, barcodes are separated by hyphens and
 qualities by space as defined in the <a href="http://samtools.github.io/hts-specs/SAMv1.pdf">SAM
 specifications</a>.' %}

{% include note.html content='FASTQ files does not require the <code>--barcodeInReadName</code> option.' %}

## Arguments

### Required Arguments

| Argument name(s) | Type | Description |
| :--------------- | :--: | :------ |
| `--input`<br/>`-I` | String | BAM/SAM/CRAM/FASTQ source of reads. |
| `--output`<br/>`-O` | String | Output SAM/BAM/CRAM file. |

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--arguments_file` | List[File] | [] | read one or more arguments files and add them to the command line |
| `--gcs_max_retries`<br/>`-gcs_retries` | int | 20 | If the GCS bucket channel errors out, how many times it will attempt to re-initiate the connection |
| `--help`<br/>`-h` | boolean | false | display the help message |
| `--version` | boolean | false | display the version number for this tool |

### Optional Common Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--addOutputSAMProgramRecord`<br/>`-addOutputSAMProgramRecord` | boolean | true | If true, adds a PG tag to created SAM/BAM/CRAM files. |
| `--barcodeInReadName`<br/>`-barcodeInReadName` | boolean | false | Use the barcode encoded in SAM/BAM/CRAM read names. Note: this is not necessary for input FASTQ files. |
| `--createOutputBamIndex`<br/>`-OBI` | boolean | true | If true, create a BAM/CRAM index when writing a coordinate-sorted BAM/CRAM file. |
| `--createOutputBamMD5`<br/>`-OBM` | boolean | false | If true, create a MD5 digest for any BAM/SAM/CRAM file created |
| `--forceEncoding`<br/>`-forceEncoding` | FastqQualityFormat | null | Force original quality encoding of the input files.<br/><br/><b>Possible values:</b> <i>Solexa</i>, <i>Illumina</i>, <i>Standard</i> |
| `--forceOverwrite`<br/>`-forceOverwrite` | Boolean | false | Force output overwriting if it exists |
| `--input2`<br/>`-I2` | String | null | BAM/SAM/CRAM/FASTQ the second source of reads (if pair-end). |
| `--interleavedInput`<br/>`-interleaved` | boolean | false | Interleaved input. |
| `--QUIET` | Boolean | false | Whether to suppress job-summary info on System.err. |
| `--rawBarcodeQualityTag`<br/>`-rawBarcodeQualityTag` | List[String] | [] | Use the qualities encoded in this tag(s) as raw barcode qualities. Requires --rawBarcodeSequenceTags. WARNING: this tag(s) will be removed/updated as necessary. |
| `--rawBarcodeSequenceTags`<br/>`-rawBarcodeSequenceTags` | List[String] | [BC] | Include the barcodes encoded in this tag(s) in the read name. Note: this is not necessary for input FASTQ files. WARNING: this tag(s) will be removed/updated as necessary. |
| `--readValidationStringency`<br/>`-VS` | ValidationStringency | SILENT | Validation stringency for all SAM/BAM/CRAM files read by this program. The default stringency value SILENT can improve performance when processing a BAM file in which variable-length data (read, qualities, tags) do not otherwise need to be decoded.<br/><br/><b>Possible values:</b> <i>STRICT</i>, <i>LENIENT</i>, <i>SILENT</i> |
| `--reference`<br/>`-R` | String | null | Reference sequence file. Required for CRAM input. |
| `--secondsBetweenProgressUpdates`<br/>`-secondsBetweenProgressUpdates` | double | 10.0 | Output traversal statistics every time this many seconds elapse. |
| `--TMP_DIR` | List[File] | [] | Undocumented option |
| `--use_jdk_deflater`<br/>`-jdk_deflater` | boolean | false | Whether to use the JdkDeflater (as opposed to IntelDeflater) |
| `--use_jdk_inflater`<br/>`-jdk_inflater` | boolean | false | Whether to use the JdkInflater (as opposed to IntelInflater) |
| `--verbosity`<br/>`-verbosity` | LogLevel | INFO | Control verbosity of logging.<br/><br/><b>Possible values:</b> <i>ERROR</i>, <i>WARNING</i>, <i>INFO</i>, <i>DEBUG</i> |

### Advanced Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--showHidden`<br/>`-showHidden` | boolean | false | display hidden arguments |


