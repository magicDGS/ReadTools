---
title: QualityEncodingDetector
summary: Detects the quality encoding format for all kind of sources for ReadTools.
permalink: QualityEncodingDetector.html
last_updated: 05-39-2018 02:39:16
---


## Description

Detects the quality encoding for a SAM/BAM/CRAM/FASTQ files, output to the STDOUT the quality encoding.

## Arguments

### Required Arguments

| Argument name(s) | Type | Description |
| :--------------- | :--: | :------ |
| `--input`<br/>`-I` | String | Reads input. |

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--arguments_file` | List[File] | [] | read one or more arguments files and add them to the command line |
| `--gcs_max_retries`<br/>`-gcs_retries` | int | 20 | If the GCS bucket channel errors out, how many times it will attempt to re-initiate the connection |
| `--help`<br/>`-h` | boolean | false | display the help message |
| `--maximumReads`<br/>`-maximumReads` | Long | 1000000 | Maximum number of reads to use for detect the quality encoding. |
| `--version` | boolean | false | display the version number for this tool |

### Optional Common Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--QUIET` | Boolean | false | Whether to suppress job-summary info on System.err. |
| `--readValidationStringency`<br/>`-VS` | ValidationStringency | SILENT | Validation stringency for all SAM/BAM/CRAM files read by this program. The default stringency value SILENT can improve performance when processing a BAM file in which variable-length data (read, qualities, tags) do not otherwise need to be decoded.<br/><br/><b>Possible values:</b> <i>STRICT</i>, <i>LENIENT</i>, <i>SILENT</i> |
| `--TMP_DIR` | List[File] | [] | Undocumented option |
| `--use_jdk_deflater`<br/>`-jdk_deflater` | boolean | false | Whether to use the JdkDeflater (as opposed to IntelDeflater) |
| `--use_jdk_inflater`<br/>`-jdk_inflater` | boolean | false | Whether to use the JdkInflater (as opposed to IntelInflater) |
| `--verbosity`<br/>`-verbosity` | LogLevel | INFO | Control verbosity of logging.<br/><br/><b>Possible values:</b> <i>ERROR</i>, <i>WARNING</i>, <i>INFO</i>, <i>DEBUG</i> |

### Advanced Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--showHidden`<br/>`-showHidden` | boolean | false | display hidden arguments |


