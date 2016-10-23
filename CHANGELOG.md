# Change Log
All notable changes to this project will be documented in this file.

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

[Unreleased]: https://github.com/magicDGS/ReadTools/tree/develop
[0.2.2]: https://github.com/magicDGS/ReadTools/releases/tag/0.2.2
[0.2.1]: https://github.com/magicDGS/ReadTools/releases/tag/0.2.1
[0.2.0]: https://github.com/magicDGS/ReadTools/releases/tag/0.2.0
[0.1.4]: https://github.com/magicDGS/ReadTools/releases/tag/0.1.4
[0.1.3]: https://github.com/magicDGS/ReadTools/releases/tag/0.1.3
[0.1.2]: https://github.com/magicDGS/ReadTools/releases/tag/0.1.2
[0.1.1]: https://github.com/magicDGS/ReadTools/releases/tag/0.1.1
[0.1.0]: https://github.com/magicDGS/ReadTools/releases/tag/0.1.0
