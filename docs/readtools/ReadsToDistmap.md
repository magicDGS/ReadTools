---
title: ReadsToDistmap
summary: Converts any kind of ReadTools source to Distmap format.
permalink: ReadsToDistmap.html
last_updated: 25-37-2018 02:37:45
---

## Description

Converts to the Distmap format
 (<a href="http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0072614">
 Pandey &amp; Schlötterer 2013</a>) any kind of ReadTools source, including information
 from the barcodes (BC tag) in the read name (Illumina format) to allow keeping sample data.

 <p>If requested, it also applies a trimming/filtering pipeline to the reads.
 </p>

<i>See additional information in the following pages:</i>

- [StandardizeReads](StandardizeReads.html)

## Arguments

### Required Arguments

| Argument name(s) | Type | Description |
| :--------------- | :--: | :------ |
| `--input`<br/>`-I` | String | BAM/SAM/CRAM/FASTQ source of reads. |
| `--output`<br/>`-O` | String | Output in Distmap format. Expected to be in an HDFS file system. |

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--arguments_file` | List[File] | [] | read one or more arguments files and add them to the command line |
| `--disable3pTrim`<br/>`-D3PT` | boolean | false | Disable 3'-trimming. Cannot be true when argument disable5pTrim (D5PT) is true. |
| `--disable5pTrim`<br/>`-D5PT` | boolean | false | Disable 5'-trimming. May be useful for downstream mark of duplicate reads, usually identified by the 5' mapping position. Cannot be true when disable3pTrim (D3P) is true. |
| `--gcs_max_retries`<br/>`-gcs_retries` | int | 20 | If the GCS bucket channel errors out, how many times it will attempt to re-initiate the connection |
| `--help`<br/>`-h` | boolean | false | display the help message |
| `--readFilter`<br/>`-RF` | List[String] | [] | Read filters to be applied in the distmap pipeline |
| `--trimmer`<br/>`-TM` | List[String] | [] | Trimmers to be applied in the distmap pipeline |
| `--version` | boolean | false | display the version number for this tool |

### Optional Common Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--barcodeInReadName`<br/>`-barcodeInReadName` | boolean | false | Use the barcode encoded in SAM/BAM/CRAM read names. Note: this is not necessary for input FASTQ files. |
| `--forceEncoding`<br/>`-forceEncoding` | FastqQualityFormat | null | Force original quality encoding of the input files.<br/><br/><b>Possible values:</b> <i>Solexa</i>, <i>Illumina</i>, <i>Standard</i> |
| `--forceOverwrite`<br/>`-forceOverwrite` | Boolean | false | Force output overwriting if it exists |
| `--input2`<br/>`-I2` | String | null | BAM/SAM/CRAM/FASTQ the second source of reads (if pair-end). |
| `--interleavedInput`<br/>`-interleaved` | boolean | false | Interleaved input. |
| `--QUIET` | Boolean | false | Whether to suppress job-summary info on System.err. |
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
| `--hdfsBlockSize`<br/>`-hdfsBlockSize` | Long | null | Block-size (in bytes) for files in HDFS. If not provided, use default configuration. |
| `--showHidden`<br/>`-showHidden` | boolean | false | display hidden arguments |


