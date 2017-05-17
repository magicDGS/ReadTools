---
title: Common Arguments
sidebar: home_sidebar
permalink: common_arguments.html
---

## Common arguments

This arguments are shared for all the tools.

### Optional Arguments:

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :----- |
| --arguments_file | File | null | read one or more arguments files and add them to the command line |
| --help,-h | Boolean | false | display the help message |
| --QUIET | Boolean | false | Whether to suppress job-summary info on System.err |
| --TMP_DIR | File | null | Undocumented option |
| --use_jdk_deflater,-jdk_deflater | Boolean | false |  Whether to use the JdkDeflater (as opposed to IntelDeflater) |
| --use_jdk_inflater,-jdk_inflater | Boolean | false | Whether to use the JdkInflater (as opposed to IntelInflater)  |
| --verbosity,-verbosity | LogLevel | INFO | Control verbosity of logging |
| --version | Boolean | false | display the version number for this tool |

### Advanced Arguments:

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :----- |
| --showHidden,-showHidden | Boolean | false | display hidden arguments |

---

## Shared arguments

This arguments are shared for general tools.

### Optional Arguments:

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :----- |
| --input2,-I2 | String  | null | BAM/SAM/CRAM/FASTQ the second source of reads (if pair-end). |
| --interleavedInput,-interleaved | Boolean | false | Interleaved input. |
| --readValidationStringency,-VS | ValidationStringency | SILENT | Validation stringency for all SAM/BAM/CRAM files read by this program. The default stringency value SILENT can improve performance when processing a BAM file in which variable-length data (read, qualities, tags) do not otherwise need to be decoded. |
| --reference,-R | File | null | Reference sequence file. Required for CRAM input. |
|--secondsBetweenProgressUpdates,-secondsBetweenProgressUpdates | Double | 10.0 | Output traversal statistics every time this many seconds elapse. |

### Advanced Arguments:

| Argument name(s) | Type | Default value(s) | Summary |
| :--------------- | :--: | :--------------: | :----- |
| --forceEncoding,-forceEncoding | FastqQualityFormat | null | Force original quality encoding of the input files. |
