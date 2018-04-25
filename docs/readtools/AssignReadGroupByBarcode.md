---
title: AssignReadGroupByBarcode
summary: Assigns read groups based on barcode tag(s) for all kind of sources for ReadTools.
permalink: AssignReadGroupByBarcode.html
last_updated: 25-37-2018 02:37:45
---

## Description

Assigns read groups (@RG) using the barcode information present in the raw barcode tag(s).

 <p>Read groups are assigned by matching the ones provided in the barcode file against the
 present in the tag(s), allowing mismatches and unknown bases (Ns) in the sequence. We also
 discard ambiguous barcodes, defined as the ones where the number of mismatches is at least x
 mismatches apart (specified with <code>--maximumMismatches</code>) from the second best barcode
 (at least one mismatch of difference, change by using <code>--minimumDistance</code>).
 If several indexes are used and none of them identify uniquely the read group, it is assigned by
 majority vote.</p>

{% include warning.html content='If several barcodes are present and one of them
 identify uniquely the read group, this is assigned directly. Thus, it is recommended to provide
 all the barcodes present in the library to the parameter.' %}

{% include note.html content='For pair-end reads, only one read is used to assign the barcode.' %}

## Arguments

### Required Arguments

| Argument name(s) | Type | Description |
| :--------------- | :--: | :------ |
| `--barcodeFile`<br/>`-bc` | String | Tab-delimited file with header for barcode sequences ('barcode_sequence' or 'barcode_sequence_1' for the first barcode, 'barcode_sequence_$(number)' for subsequent if more than one index is used), sample name ('sample_name' or 'barcode_name') and, optionally, library name ('library_name').  Barcode file will overwrite any of Read Group arguments for the same information. WARNING: this file should contain all the barcodes present in the multiplexed file. |
| `--input`<br/>`-I` | String | BAM/SAM/CRAM/FASTQ source of reads. |
| `--output`<br/>`-O` | String | Output SAM/BAM/CRAM file prefix. |

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--arguments_file` | List[File] | [] | read one or more arguments files and add them to the command line |
| `--gcs_max_retries`<br/>`-gcs_retries` | int | 20 | If the GCS bucket channel errors out, how many times it will attempt to re-initiate the connection |
| `--help`<br/>`-h` | boolean | false | display the help message |
| `--keepDiscarded`<br/>`-keepDiscarded` | boolean | false | Keep reads does not assigned to any record in a separate file. |
| `--maximumMismatches`<br/>`-mm` | List[Integer] | [0] | Maximum number of mismatches allowed for a matched barcode. Specify more than once for apply a different threshold to several indexes. |
| `--maximumN`<br/>`-maxN` | Integer | null | Maximum number of unknown bases (Ns) allowed in the barcode to consider them. If 'null', no threshold will be applied. |
| `--minimumDistance`<br/>`-md` | List[Integer] | [1] | Minimum distance (difference in number of mismatches) between the best match and the second. Specify more than once for apply a different threshold to several indexes. |
| `--nNoMismatch`<br/>`-nnm` | boolean | false | Do not count unknown bases (Ns) as mismatch. |
| `--RGCN`<br/>`-CN` | String | null | Read Group sequencing center name |
| `--RGDT`<br/>`-DT` | Iso8601Date | null | Read Group run date |
| `--RGLB`<br/>`-LB` | String | null | Read Group Library |
| `--RGPI`<br/>`-PI` | Integer | null | Read Group predicted insert size |
| `--RGPL`<br/>`-PL` | PlatformValue | null | Read Group platform (e.g. illumina, solid)<br/><br/><b>Possible values:</b> <i>CAPILLARY</i>, <i>LS454</i>, <i>ILLUMINA</i>, <i>SOLID</i>, <i>HELICOS</i>, <i>IONTORRENT</i>, <i>ONT</i>, <i>PACBIO</i> |
| `--RGPM`<br/>`-PM` | String | null | Read Group platform model |
| `--RGPU`<br/>`-PU` | String | null | Read Group platform unit (eg. run barcode) |
| `--runName`<br/>`-runName` | String | null | Run name to add to the ID in the read group information. |
| `--splitLibraryName` | boolean | false | Split file by library. |
| `--splitReadGroup` | boolean | false | Split file by read group. |
| `--splitSample` | boolean | false | Split file by sample. |
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
| `--outputFormat`<br/>`-outputFormat` | BamFormat | BAM | SAM/BAM/CRAM output format.<br/><br/><b>Possible values:</b> <i>SAM</i>, <i>BAM</i>, <i>CRAM</i> |
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
| `--showHidden`<br/>`-showHidden` | boolean | false | display hidden arguments |


