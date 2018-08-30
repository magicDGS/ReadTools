---
title: GemMappabilityToBed (EXPERIMENTAL)
summary: Converts a GEM-mappability file into a BED-graph format
permalink: GemMappabilityToBed.html
last_updated: 30-57-2018 12:57:55
---

{% include warning.html content="This a EXPERIMENTAL tool and should not be used for production" %}

## Description

Converts a GEM-mappability
 (<a href="http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0030377">
 Derrien <i>et al.</i> 2012</a>) file into a BED-graph format.

 <p>The GEM-mappability format is a FASTA-like file with:</p>

 <ul>
     <li>Header including information for the algorithm applied (e.g., k-mer size).</li>
     <li>
         Sequences in FASTA-like format: sequence name preceded by ~ and a character per-base
         encoded in the header. Each character encodes a different range of values,
         which represent the number of mappings of the reads starting at that position.
     </li>
 </ul>

 <p>This tool parses the GEM-mappability file and outputs a per-position BED-graph format. For
 each position, different scoring systems could be use (see arguments for more information).
 </p>

## Arguments

### Required Arguments

| Argument name(s) | Type | Description |
| :--------------- | :--: | :------ |
| `--input`<br/>`-I` | String | GEM-mappability file (FASTA-like) to parse |
| `--output`<br/>`-O` | String | Bed-graph output with the number of mappings as score. If it contains a block-compressed extension (e.g., .bgz), it will be compressed. |

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--arguments_file` | List[File] | [] | read one or more arguments files and add them to the command line |
| `--gcs_max_retries`<br/>`-gcs_retries` | int | 20 | If the GCS bucket channel errors out, how many times it will attempt to re-initiate the connection |
| `--help`<br/>`-h` | boolean | false | display the help message |
| `--score-method` | GemScoreMethod | MID | Method to convert number of mappings within a range into the reported score<br/><br/><b>Possible values:</b> <i>MIN</i> (Minimum of the range of mappings.), <i>MAX</i> (Maximum of the range of mappings.), <i>MID</i> (Middle point of the range of mappings.) |
| `--version` | boolean | false | display the version number for this tool |

### Optional Common Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--forceOverwrite`<br/>`-forceOverwrite` | Boolean | false | Force output overwriting if it exists |
| `--QUIET` | Boolean | false | Whether to suppress job-summary info on System.err. |
| `--TMP_DIR` | List[File] | [] | Undocumented option |
| `--use_jdk_deflater`<br/>`-jdk_deflater` | boolean | false | Whether to use the JdkDeflater (as opposed to IntelDeflater) |
| `--use_jdk_inflater`<br/>`-jdk_inflater` | boolean | false | Whether to use the JdkInflater (as opposed to IntelInflater) |
| `--verbosity`<br/>`-verbosity` | LogLevel | INFO | Control verbosity of logging.<br/><br/><b>Possible values:</b> <i>ERROR</i>, <i>WARNING</i>, <i>INFO</i>, <i>DEBUG</i> |

### Advanced Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--showHidden`<br/>`-showHidden` | boolean | false | display hidden arguments |


