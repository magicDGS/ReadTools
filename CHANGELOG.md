# Change Log
All notable changes to this project will be documented in this file.

## [0.1.3]
### Changed
- Changed logic of bacode detection: at least one difference between the best barcode and the second best is needed to do not discard a barcode (default). For the previous behaviour, use -d 0
- TaggedBamToFastq check the PF flag in the BAM file and ignore reads that does not pass the vendor quality

### Added
- BamBarcodeDetector (new tool) for detect barcodes in a BAM file, adding read groups and split if requested
- New options for barcode detection: maximum Ns, N not counting as mismatch, distance between best barcode and second barcode
- Output metrics file when detecting barcodes

## [0.1.2] - 2015-11-26
### Added
- Checking of output file existence
- Create directories for output if they do not exists
- Checking number of arguments

## [0.1.1] - 2015-11-24
### Fixed
- Fixed bug in single-end TrimFastq tool

## [0.1.0] - 2015-11-20
First pre-release

[Unreleased]: https://github.com/magicDGS/ReadTools/tree/develop
[0.1.3]: https://github.com/magicDGS/ReadTools/releases/tag/0.1.3
[0.1.2]: https://github.com/magicDGS/ReadTools/releases/tag/0.1.2
[0.1.1]: https://github.com/magicDGS/ReadTools/releases/tag/0.1.1
[0.1.0]: https://github.com/magicDGS/ReadTools/releases/tag/0.1.0
