---
title: TrimReads
summary: Applies a trimming pipeline to any kind of sources for ReadTools
permalink: TrimReads.html
last_updated: 25-37-2018 02:37:45
---

## Description

Applies a trimming/filtering pipeline to the reads:

 <ol>

 <li>Trimmers are applied in order. If ay read is trimmed completely, other trimmers are
 ignored.</li>

 <li>Filter out completely trim reads.</li>

 <li>Apply the fiters in order. If any read is filtered, the FT tag reflects the ReadFilter
 involved.</li>

 </ol>

{% include warning.html content='Default trimmers/filters are applied before any other user-specified
 trimmers/filters. If you would like to apply them in a differen order, use
 <code>--disableAllDefaultTrimmers</code>/<code>--disableAllDefaultFilters</code> in combination
 with the new ordering.' %}

{% include note.html content='Default arguments perform the same algorithm as the one described in
 <a href="http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0015925">
 Kofler <i>et al.</i> (2011)</a>. Other features in their implementation could be applied with
 some minor modifications in the command line.' %}

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
| `--disable3pTrim`<br/>`-D3PT` | boolean | false | Disable 3'-trimming. Cannot be true when argument disable5pTrim (D5PT) is true. |
| `--disable5pTrim`<br/>`-D5PT` | boolean | false | Disable 5'-trimming. May be useful for downstream mark of duplicate reads, usually identified by the 5' mapping position. Cannot be true when disable3pTrim (D3P) is true. |
| `--disableAllDefaultTrimmers`<br/>`-disableAllDefaultTrimmers` | boolean | false | Disable all default trimmers. It may be useful to reorder the trimmers. |
| `--disableReadFilter`<br/>`-DF` | List[String] | [] | Read filters to be disabled after trimming |
| `--disableToolDefaultReadFilters`<br/>`-disableToolDefaultReadFilters` | boolean | false | Disable all tool default read filters for trimming |
| `--disableTrimmer`<br/>`-DTM` | Set[String] | [] | Default trimmers to be disabled. |
| `--gcs_max_retries`<br/>`-gcs_retries` | int | 20 | If the GCS bucket channel errors out, how many times it will attempt to re-initiate the connection |
| `--help`<br/>`-h` | boolean | false | display the help message |
| `--keepDiscarded`<br/>`-keepDiscarded` | boolean | false | Keep discarded reads in a separate file. Note: For pair-end input, this file contain also mates of discarded reads (they do not have FT tag). |
| `--readFilter`<br/>`-RF` | List[String] | [] | Read filters to be applied after trimming |
| `--trimmer`<br/>`-TM` | List[String] | [] | Trimmers to be applied. Note: default trimmers are applied first and then the rest of them in order. |
| `--version` | boolean | false | display the version number for this tool |

### Optional Common Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--addOutputSAMProgramRecord`<br/>`-addOutputSAMProgramRecord` | boolean | true | If true, adds a PG tag to created SAM/BAM/CRAM files. |
| `--createOutputBamIndex`<br/>`-OBI` | boolean | true | If true, create a BAM/CRAM index when writing a coordinate-sorted BAM/CRAM file. |
| `--createOutputBamMD5`<br/>`-OBM` | boolean | false | If true, create a MD5 digest for any BAM/SAM/CRAM file created |
| `--forceEncoding`<br/>`-forceEncoding` | FastqQualityFormat | null | Force original quality encoding of the input files.<br/><br/><b>Possible values:</b> <i>Solexa</i>, <i>Illumina</i>, <i>Standard</i> |
| `--forceOverwrite`<br/>`-forceOverwrite` | Boolean | false | Force output overwriting if it exists |
| `--input2`<br/>`-I2` | String | null | BAM/SAM/CRAM/FASTQ the second source of reads (if pair-end). |
| `--interleavedInput`<br/>`-interleaved` | boolean | false | Interleaved input. |
| `--QUIET` | Boolean | false | Whether to suppress job-summary info on System.err. |
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


