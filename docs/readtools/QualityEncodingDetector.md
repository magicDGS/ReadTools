---
title: QualityEncodingDetector
summary: Detects the quality encoding format for all kind of sources for ReadTools.
permalink: QualityEncodingDetector.html
last_updated: 19-58-2017 02:58:19
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
| `--help`<br/>`-h` | boolean | false | display the help message |
| `--maximumReads`<br/>`-maximumReads` | Long | 1000000 | Maximum number of reads to use for detect the quality encoding. |
| `--version` | boolean | false | display the version number for this tool |

### Optional Common Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--QUIET` | Boolean | false | Whether to suppress job-summary info on System.err. |
| `--TMP_DIR` | List[File] | [] | Undocumented option |
| `--use_jdk_deflater`<br/>`-jdk_deflater` | boolean | false | Whether to use the JdkDeflater (as opposed to IntelDeflater) |
| `--use_jdk_inflater`<br/>`-jdk_inflater` | boolean | false | Whether to use the JdkInflater (as opposed to IntelInflater) |
| `--verbosity`<br/>`-verbosity` | LogLevel | INFO | Control verbosity of logging.<br/><br/><b>Possible values:</b> <i>ERROR</i>, <i>WARNING</i>, <i>INFO</i>, <i>DEBUG</i> |

### Advanced Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--showHidden`<br/>`-showHidden` | boolean | false | display hidden arguments |


