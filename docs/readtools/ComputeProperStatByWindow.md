---
title: ComputeProperStatByWindow (EXPERIMENTAL)
summary: Computes proper-paired reads statistics over windows 
permalink: ComputeProperStatByWindow.html
last_updated: 05-39-2018 02:39:16
---

{% include warning.html content="This a EXPERIMENTAL tool and should not be used for production" %}

## Description

Computes statistics for properly-paired reads over non-overlapping windows.

 <p>Statistics are computed only for proper reads (mapped on the same contig). Nevertheless,
 statistics might use only single-read information (e.g., <code>ContainIndelCounter</code>,
 which counts the number of reads on the window containing indels) or from both pairs (e.g.,
 <code>PairIntegerTagCounter</code> for NM&lt;2 would count the number of reads on the window where
 both reads on the pair has more than 2 mismatches stored in the NM tag).
 </p>

 <h3>Caveats</h3>

 <ul>
     <li>Pair-end data is required even for computing only single read statistics.</li>
     <li>Coordinate-sorted SAM/BAM/CRAM is required.</li>
     <li>Intervals are not allowed in this tool. The statistics are computed over the genome.</li>
     <li>
         It is recommended that the file includes all the pair-end data (not only a subset of the reads).
         Otherwise, missing pairs would not be used for the statistics.
     </li>
 </ul>

{% include warning.html content='Please, note that disabling default read filters on this tool will produce
 wrong results.' %}

{% include note.html content='In this tool, proper pairs are defined as mapping on the same contig, without
 taking into consideration the SAM flag (0x2).' %}

## Arguments

### Required Arguments

| Argument name(s) | Type | Description |
| :--------------- | :--: | :------ |
| `--count-pair-int-tag-list` | List[String] | Integer SAM tag to count for pairs |
| `--count-pair-int-tag-operator-list` | List[RelationalOperator] | Operation for the integer SAM tag (with respect to the threshold). Should be specified the same number of times as --count-pair-int-tag-list |
| `--count-pair-int-tag-threshold-list` | List[Integer] | Threshold for the integer SAM tag (with respect to the operation). Should be specified the same number of times as --count-pair-int-tag-list |
| `--input`<br/>`-I` | List[String] | BAM/SAM/CRAM file containing reads |
| `--output`<br/>`-O` | String | Tab-delimited output file with the statistic over the windows. A header defines the order of each statistic and the first column the window in the form contig:start-end. |
| `--window-size` | Integer | Window size to perform the analysis |

### Optional Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--arguments_file` | List[File] | [] | read one or more arguments files and add them to the command line |
| `--cloud-index-prefetch-buffer`<br/>`-CIPB` | int | -1 | Size of the cloud-only prefetch buffer (in MB; 0 to disable). Defaults to cloudPrefetchBuffer if unset. |
| `--cloud-prefetch-buffer`<br/>`-CPB` | int | 40 | Size of the cloud-only prefetch buffer (in MB; 0 to disable). |
| `--contig` | List[String] | [] | Limit the computation to the provided contig(s). This argument is used instead of interval arguments and might be removed in the future if intervals are supported. |
| `--disable-bam-index-caching`<br/>`-DBIC` | boolean | false | If true, don't cache bam indexes, this will reduce memory requirements but may harm performance if many intervals are specified.  Caching is automatically disabled if there are no intervals specified. |
| `--gcs_max_retries`<br/>`-gcs_retries` | int | 20 | If the GCS bucket channel errors out, how many times it will attempt to re-initiate the connection |
| `--help`<br/>`-h` | boolean | false | display the help message |
| `--interval-merging-rule`<br/>`-imr` | IntervalMergingRule | ALL | By default, the program merges abutting intervals (i.e. intervals that are directly side-by-side but do not actually overlap) into a single continuous interval. However you can change this behavior if you want them to be treated as separate intervals instead.<br/><br/><b>Possible values:</b> <i>ALL</i>, <i>OVERLAPPING_ONLY</i> |
| `--intervals`<br/>`-L` | List[String] | [] | One or more genomic intervals over which to operate |
| `--reference`<br/>`-R` | String | null | Reference sequence |
| `--stat` | Set[Statistic] | [] | Statistics to compute (currently only for single-reads) |
| `--version` | boolean | false | display the version number for this tool |

### Optional Common Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--add-output-sam-program-record`<br/>`-add-output-sam-program-record` | boolean | true | If true, adds a PG tag to created SAM/BAM/CRAM files. |
| `--add-output-vcf-command-line`<br/>`-add-output-vcf-command-line` | boolean | true | If true, adds a command line header line to created VCF files. |
| `--create-output-bam-index`<br/>`-OBI` | boolean | true | If true, create a BAM/CRAM index when writing a coordinate-sorted BAM/CRAM file. |
| `--create-output-bam-md5`<br/>`-OBM` | boolean | false | If true, create a MD5 digest for any BAM/SAM/CRAM file created |
| `--create-output-variant-index`<br/>`-OVI` | boolean | true | If true, create a VCF index when writing a coordinate-sorted VCF file. |
| `--create-output-variant-md5`<br/>`-OVM` | boolean | false | If true, create a a MD5 digest any VCF file created. |
| `--disable-sequence-dictionary-validation`<br/>`-disable-sequence-dictionary-validation` | boolean | false | If specified, do not check the sequence dictionaries from our inputs for compatibility. Use at your own risk! |
| `--disableReadFilter`<br/>`-DF` | List[String] | [] | Read filters to be disabled before analysis |
| `--disableToolDefaultReadFilters`<br/>`-disableToolDefaultReadFilters` | boolean | false | Disable all tool default read filters |
| `--exclude-intervals`<br/>`-XL` | List[String] | [] | Use this argument to exclude certain parts of the genome from the analysis (like -L, but the opposite). This argument can be specified multiple times. You can use samtools-style intervals either explicitly on the command line (e.g. -XL 1 or -XL 1:100-200) or by loading in a file containing a list of intervals (e.g. -XL myFile.intervals). |
| `--forceOverwrite`<br/>`-forceOverwrite` | Boolean | false | Force output overwriting if it exists |
| `--interval-exclusion-padding`<br/>`-ixp` | int | 0 | Use this to add padding to the intervals specified using -XL. For example, '-XL 1:100' with a padding value of 20 would turn into '-XL 1:80-120'. This is typically used to add padding around targets when analyzing exomes. |
| `--interval-padding`<br/>`-ip` | int | 0 | Use this to add padding to the intervals specified using -L. For example, '-L 1:100' with a padding value of 20 would turn into '-L 1:80-120'. This is typically used to add padding around targets when analyzing exomes. |
| `--interval-set-rule`<br/>`-isr` | IntervalSetRule | UNION | By default, the program will take the UNION of all intervals specified using -L and/or -XL. However, you can change this setting for -L, for example if you want to take the INTERSECTION of the sets instead. E.g. to perform the analysis only on chromosome 1 exomes, you could specify -L exomes.intervals -L 1 --interval-set-rule INTERSECTION. However, it is not possible to modify the merging approach for intervals passed using -XL (they will always be merged using UNION). Note that if you specify both -L and -XL, the -XL interval set will be subtracted from the -L interval set.<br/><br/><b>Possible values:</b> <i>UNION</i>, <i>INTERSECTION</i> |
| `--lenient`<br/>`-LE` | boolean | false | Lenient processing of VCF files |
| `--QUIET` | Boolean | false | Whether to suppress job-summary info on System.err. |
| `--read-index`<br/>`-read-index` | List[String] | [] | Indices to use for the read inputs. If specified, an index must be provided for every read input and in the same order as the read inputs. If this argument is not specified, the path to the index for each input will be inferred automatically. |
| `--read-validation-stringency`<br/>`-VS` | ValidationStringency | SILENT | Validation stringency for all SAM/BAM/CRAM/SRA files read by this program.  The default stringency value SILENT can improve performance when processing a BAM file in which variable-length data (read, qualities, tags) do not otherwise need to be decoded.<br/><br/><b>Possible values:</b> <i>STRICT</i>, <i>LENIENT</i>, <i>SILENT</i> |
| `--readFilter`<br/>`-RF` | List[String] | [] | Read filters to be applied before analysis |
| `--seconds-between-progress-updates`<br/>`-seconds-between-progress-updates` | double | 10.0 | Output traversal statistics every time this many seconds elapse |
| `--sequence-dictionary`<br/>`-sequence-dictionary` | String | null | Use the given sequence dictionary as the master/canonical sequence dictionary.  Must be a .dict file. |
| `--TMP_DIR` | List[File] | [] | Undocumented option |
| `--use_jdk_deflater`<br/>`-jdk_deflater` | boolean | false | Whether to use the JdkDeflater (as opposed to IntelDeflater) |
| `--use_jdk_inflater`<br/>`-jdk_inflater` | boolean | false | Whether to use the JdkInflater (as opposed to IntelInflater) |
| `--verbosity`<br/>`-verbosity` | LogLevel | INFO | Control verbosity of logging.<br/><br/><b>Possible values:</b> <i>ERROR</i>, <i>WARNING</i>, <i>INFO</i>, <i>DEBUG</i> |

### Advanced Arguments

| Argument name(s) | Type | Default value(s) | Description |
| :--------------- | :--: | :--------------: | :------ |
| `--do-not-print-all` | boolean | false | If set, skip printing windows with 0 reads |
| `--showHidden`<br/>`-showHidden` | boolean | false | display hidden arguments |


