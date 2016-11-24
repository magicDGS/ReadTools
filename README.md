[![Build Status](https://travis-ci.org/magicDGS/ReadTools.svg?branch=master)](https://travis-ci.org/magicDGS/ReadTools)
[![codecov](https://codecov.io/gh/magicDGS/ReadTools/branch/master/graph/badge.svg)](https://codecov.io/gh/magicDGS/ReadTools)
[![Language](http://img.shields.io/badge/language-java-brightgreen.svg)](https://www.java.com/)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/dd842750e7a74112870a5156a24a8cbf)](https://www.codacy.com/app/daniel-gomez-sanchez/ReadTools?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=magicDGS/ReadTools&amp;utm_campaign=Badge_Grade)
[![Sputnik](https://sputnik.ci/conf/badge)](https://sputnik.ci/app#/builds/magicDGS/ReadTools)
[![MIT licensed](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

# ReadTools

Tools for sequencing reads data sources (SAM/BAM/CRAM/FASTQ files)

---

Tools contained in this software are develop for work with different sources of reads data, in different formats. The aim is to provide a set of tools for:

- Pre-processing raw reads from any kind of sources (SAM/BAM/CRAM/FASTQ), as trimming and sample-barcode assignation.
- Standardize read sources to follow the [SAM specs](http://samtools.github.io/hts-specs/), as fixing quality encoding or barcode tags.

---

### Download

Packaged jar files could be found in [Releases](https://github.com/magicDGS/ReadTools/releases). For install from source, see bellow.

---

### Usage

To obtain a list of implemented tools run the following command:

`java -jar ReadTools.jar`

For a long description of each tool:

`java -jar ReadTools.jar <toolName> --help`

For getting the packaged version:

`java -jar ReadTools.jar --version` or `java -jar ReadTools.jar -v`

The current pre-release includes the following tools:

* __TrimFastq__: Implementation of the trimming algorithm from [Kofler _et al._ (2011)](http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0015925)
* __TaggedBamToFastq__: Convert an BAM file with BC tags into a FASTQ file
* __QualityEncodingDetector__: Detects the quality encoding format for all kind of sources for ReadTools
* __StandardizeReads__: Standardize quality and format for all kind of sources for ReadTools
* __FastqBarcodeDetector__: Identify barcodes in the read name for a FASTQ file and assign to the ones used on the library
* __BamBarcodeDetector__:	Identify barcodes in the read name for a BAM file and assign to the ones used on the library
* __ReadsToFastq__: Converts any kind of ReadTools source to FASTQ format

Legacy tools that will disapear in following releases:
* __QualityChecker__: Get the quality encoding for a BAM/FASTQ file
* __StandardizeQuality__: Convert an Illumina BAM/FASTQ file into a Sanger

---

### Installation from source

Master branch is guarantee to compile successfully and contain the latest release changes. For installation, run:

```

git clone https://github.com/magicDGS/ReadTools.git
cd ReadTools
./gradlew shadowJar

```

Executable jar file will be under the __build/libs/ReadTools.jar__ path. This packaged jar contains all needed dependencies, and could be used independently of the repository folder.

---

### License and citing

The software is provided with a copy of the [MIT License](http://opensource.org/licenses/MIT), but is build with several Java libraries that have different licenses. Information for the libraries used in the software is available in the [build.gradle](https://github.com/magicDGS/ReadTools/blob/master/build.gradle) file.

If you use this software, please add the citation as following:

&nbsp;&nbsp;&nbsp;&nbsp;Gómez-Sánchez D (2015): ReadTools ${version}, Institut für Populationsgenetik, Vetmeduni Vienna.

If some of the tools is a (re)implementation of a method described in a different place, the citation for the method is provided under the long description. Please, cite the method in addition to this software to give credit to the original authors.

---
*Please, if you find any problem add a new [issue](https://github.com/magicDGS/ReadTools/issues) or contact by email: <daniel.gomez.sanchez@hotmail.es>
