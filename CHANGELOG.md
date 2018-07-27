# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning (SemVer)](http://semver.org/).
The meaning of each entry is user-based and can be found in our
[Wiki Versioning page](https://github.com/magicDGS/ReadTools/wiki/Versioning).

## [Unreleased]

### Fixed
- Improved performance for quality checking in every tool
- Improved performance for trimming pipelines
- Support non-local reference files for CRAM (e.g., HDFS or GCS)

### Developer
- Upgrade gradle and plugin versions
- Simplify `RTBaseTest` and extend functionality with GATK's `BaseTest`
- Force locale to US-EN in tests
- Update HTSJDK to 2.16.0
- Update to released GATK 4.0.6.0 (patched-version for ReadTools)

## [1.4.1]

### Fixed
- Support non-local FASTQ inputs (e.g., HDFS or GCS)
- `DownloadDistmapResult` checks for successful job (*_SUCCESS* file)

## [1.4.0]

### Changed
- Read filters does not support short arguments anymore (GATK update)

### Fixed
- Improve command line startup information
- Better error message for invalid tool names

### Added
- Experimental tool for compute mapped pair-end statistics per window (`ComputeProperStatByWindow`)
- Add support for long reference sequences in BAM (HTSJDK update)
- Add support for long CIGAR in BAM (HTSJDK update)
- Improvement in Intel compression (GKL update)

### Developer
- New framework for read statistics
- New program group for mapped reads
- New walker class for reads (SAM/BAM/CRAM) - use only for mapped reads
- New templates for documentation to integrate experimental/beta features
- Update HTSJDK to 2.15.0
- Update to released GATK 4.0.2.1 (patched-version for ReadTools)
- Update to barclay 2.0.0 and other transitive dependencies from GATK
- Configuration from GATK is made by the default Owner config file

## [1.3.0]

### Fixed
- Fix failing uncompression of the jar file on OSX due to name collisions
- More concise repository README without redundant information
- Clarify usage of `DownloadDistmapResult` --noRemoveTaskProgramGroup

### Added
- Include validation stringency argument in `QualityEncodingDetector`
- Support for Hadoop compression service in the classpath and documentation in help pages

### Security
- Update documentation page dependencies, which had security issues

### Developer
- Exluce `junit` from gradle configuration
- Add Wiki for developers (repository level)
- Add contribution information and code of conduct
- Remove warnings in help pages (jekyll build) 

## [1.2.1]

### Fixed
- Bug fix: remove PG tag in records while `DownloadDistmapResult` drops PG headers

## [1.2.0]

### Fixed
- User error error for corrupted BAM file instead of unexpected error

### Added
- Trimming support for `ReadsToDistmap`
- Support of input CRAM files with reference in HDFS (might fail if used also for output)

### Changed
- Argument expansion for collections is now **.args** instead of **.list**
- Support for several libraries in `LibraryReadFilter` (GATK v4 change)
- Update documentation

### Developer
- Refactored trimming engine for improved re-usability
- Updated jacoco version to 0.8.0
- Removed VersionEye due to end of the service
- Using our own classes for argument definitions instead of relying in GATK
- More test and consistency on them
- Fix FreeMarker template to properly format jekyll tables

## [1.1.0] - 2017-10-04

### Fixed
- Fix bug for FASTQ read names with spaces without barcode/pair-end information
- Honor asynchronous writing in Distmap tools
- Fix error for different number of barcodes in dictionary and files
- Major bug fixes to CRAM format (HTSJDK change)

### Added
- Add walkthrough data for documentation
- Use pre-sorted output for DistmapPartDownloader if possible (speed-up)
- Better support of Snappy compression/decompression in more systems (HTSJDK change)

### Changed
- Assume SO:unsorted and GO:query for pair-end data if no header is present
- Do not allow SO:coordinate for pair-end data
- Do not change SO:queryname for SO:unsorted for single-end data
- Update documentation
- Remove `PrimaryAlignmentReadFilter` in favor of `PrimaryLineReadFilter` (GATK v4 change)
- More consistent GCS support (GATK v4 change)

### Developer
- Force HTSJDK dependency to be the one provided
- Implement automatic documentation framework
- Remove `FastqToReadIterator` in favor of generic `RecordToReadIterator`
- Add tests for Distmap tools for HDFS
- Force testing dependencies to be in hte version provided
- Use walkthrough data for some tests
- Test clean-up

## [1.0.0] - 2017-05-18

### Fixed
- MD5 file digest for FASTQ is for the file itself and not its content.
- Fixed relative path handling in writers
- FASTQ writing honors buffer parameters

### Added
- HDFS support for input and output. The URI should specify the schema (__hdfs__) and the host.
- Support for convert any input to the Distmap format.
- Common arguments

### Changed
- Removed legacy tools
- Barcode metrics improvements: add header and PCT_RECORDS
- Changed argument for split by read group, sample and/or library
- Changed separator for barcode qualities if multiple indexes to space. Hyphen was used before, but it is a valid quality character.
- (Developer) Removed legacy IO system
- (Developer) Removed some deprecated code
- (Developer) Repackaged some classes
- (Developer) Removed GATKReadFilterPluginDescriptor hack
- (Developer) Improve gradle build versioning

## [0.3.0] - 2017-02-06

### Added
- New tools for quality checking/standardizing: `QualityEncodingDetector` and `StandardizeReads`
- New tool for converting to other format: `ReadsToFastq`
- New tool for barcode detection: `AssignReadGroupByBarcode`
- New tool for trimming/filtering pipeline: `TrimReads`
- Input formats for every tool (except specified) includes BAM/SAM/CRAM/FASTQ
- Tools output is in a consistent BAM/SAM/CRAM format (except for conversion tools)
- Walker framework for single/pair-end reads traversal
- BAM/SAM/CRAM/FASTQ sources are managed in the same way for a consistent output

### Fixed
- Fixed issue for CRAM files detection by extension and IO
- Fixed barcode detection metrics (solves issue [#77](https://github.com/magicDGS/ReadTools/issues/77) and other minor issues)
- BarcodeDictionary.getSampleReadGroups() returns an unmodifiable list

### Changed
- Tool deprecation: `QualityChecker`, `StandardizeQuality`, `BamBarcodeDetector`, `FastqBarcodeDetector`, `TaggedBamToFastq`, `TrimFastq`
- Barcode file format (for new tools): strict tab-delimited file and header for each column (Picard's ExtractIlluminaBarcodes style). Requires at least a sample/barcode name and a barcode sequence. Library is optional.
- Metrics from barcode detection ordered as the input file (samples and barcodes).
- For new barcode tools, several indexes are joined with hyphen instead of underscore.
- Default values are provided in the command line for list arguments in BarcodeArgumentCollection. If the user provide an option, the arguments will be overridden.
- `TrimReads` applies trimmers before filters (see [#101](https://github.com/magicDGS/ReadTools/issues/101))

## [0.2.3] - 2016-11-01

### Fixed
- Validation stringency parameter (solves issue [#35](https://github.com/magicDGS/ReadTools/issues/35))
- Option `--forceOverwrite` for force overwrite output (solves issue [#39](https://github.com/magicDGS/ReadTools/issues/39))
- Option `--readNameEncoding` for Illumina/Casava read name formats (solves issue [#48](https://github.com/magicDGS/ReadTools/issues/48))

### Changed
- Testing is done with TestNG (developer)
- Code cleaning and refactoring using [GATK4](https://github.com/broadinstitute/gatk) (developer improvement).

### Added
- Integration tests for the all the tools
- Support for CRAM files reading (requires reference sequence). This support is not tested and relies on [HTSJDK](https://samtools.github.io/htsjdk) implementation
- Option for create MD5 digest files from outputs (FATSQ/BAM/SAM/CRAM)

## [0.2.2] - 2016-08-09

### Fixed
- Option for allow higher qualities (`-ahq`) when standard encoding (issue [#24](https://github.com/magicDGS/ReadTools/issues/24))
- Fixed bug in discarded output from TaggedBamToFastq for pair-end data (issue [#34](https://github.com/magicDGS/ReadTools/issues/34))

## Changed
- Change from Maven to Gradle as build system (developer improvement)

## [0.2.1] - 2016-04-19

### Fixed
- Fixed bug with standard qualities encoded withe the Illumina 1.8 protocol (issue [#21](https://github.com/magicDGS/ReadTools/issues/21))

### Added
- Maximum length option for TrimFastq (`--max`)
- More informative error when quality checking fails

## [0.2.0] - 2016-03-21

### Fixed
- Fixed BAM file not standardize output when `-nstd` option is provided in BamBarcodeDetector
- Fixed bug in (issue [#19](https://github.com/magicDGS/ReadTools/issues/19))

### Changed
- New barcode file format (without header): SampleName, Library, FirstBarcode, SecondBarcode. The program detects if the library is single or double indexed depending on the existence of the SecondBarcode column.
- Now barcodes with several indexes are not merged in the read name, but separated by "_". This breaks the compatibility with respect to version 0.1.*

### Added
- Checking quality of every base when converting and every 1000 reads when not
- Default values (BC and B2) for tags containing the barcodes in unmapped BAM files 

## [0.1.4] - 2015-12-10

### Fixed
- Fixed bug when splitting with BamBarcodeDetector (issue [#14](https://github.com/magicDGS/ReadTools/issues/14))

## [0.1.3] - 2015-12-09

### Changed
- Logic of bacode detection by default: at least one difference between the best barcode and the second best is needed to do not discard a barcode. For the previous behaviour, use `-d 0`
- TaggedBamToFastq check the PF flag in the BAM file and ignore reads that does not pass the vendor quality

### Added
- BamBarcodeDetector (new tool) for detect barcodes in a BAM file, adding read groups and split if requested
- New options for barcode detection: maximum Ns, N not counting as mismatch, distance between best barcode and second barcode
- Output metrics file when detecting barcodes
- Barcode files are white space delimited (either space or tabs)

## [0.1.2] - 2015-11-26

### Added
- Checking output file existence
- Create directories for output if they do not exists
- Checking number of arguments

## [0.1.1] - 2015-11-24

### Fixed
- Fixed bug in single-end TrimFastq tool (issue [#9](https://github.com/magicDGS/ReadTools/issues/9))

## [0.1.0] - 2015-11-20
First pre-release


[Unreleased]: https://github.com/magicDGS/ReadTools/tree/master
[0.1.0]: https://github.com/magicDGS/ReadTools/releases/tag/0.1.0
[0.1.1]: https://github.com/magicDGS/ReadTools/releases/tag/0.1.1
[0.1.2]: https://github.com/magicDGS/ReadTools/releases/tag/0.1.2
[0.1.3]: https://github.com/magicDGS/ReadTools/releases/tag/0.1.3
[0.1.4]: https://github.com/magicDGS/ReadTools/releases/tag/0.1.4
[0.2.0]: https://github.com/magicDGS/ReadTools/releases/tag/0.2.0
[0.2.1]: https://github.com/magicDGS/ReadTools/releases/tag/0.2.1
[0.2.2]: https://github.com/magicDGS/ReadTools/releases/tag/0.2.2
[0.2.3]: https://github.com/magicDGS/ReadTools/releases/tag/0.2.2
[0.3.0]: https://github.com/magicDGS/ReadTools/releases/tag/0.3.0
[1.0.0]: https://github.com/magicDGS/ReadTools/releases/tag/1.0.0
[1.1.0]: https://github.com/magicDGS/ReadTools/releases/tag/1.1.0
[1.2.0]: https://github.com/magicDGS/ReadTools/releases/tag/1.2.0
[1.2.1]: https://github.com/magicDGS/ReadTools/releases/tag/1.2.1
[1.3.0]: https://github.com/magicDGS/ReadTools/releases/tag/1.3.0
[1.4.0]: https://github.com/magicDGS/ReadTools/releases/tag/1.4.0
[1.4.1]: https://github.com/magicDGS/ReadTools/releases/tag/1.4.1
