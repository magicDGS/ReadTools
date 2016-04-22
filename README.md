[![Build Status](https://travis-ci.org/magicDGS/ReadTools.svg?branch=master)](https://travis-ci.org/magicDGS/ReadTools)
[![Sputnik](https://sputnik.ci/conf/badge)](https://sputnik.ci/app#/builds/magicDGS/ReadTools)

# ReadTools

Tools for sequencing data (BAM and FASTQ files)

---

The tools contained in this software are develop for working with FASTQ and BAM files (no mapped, although some of the tools will work with them) for different pre-processing of the data. The aim is to provide a set of tools for trimming, barcoding and quality-related pipelines, but not adaptor removal or barcode identification from scratch.

---

### Download release

[Releases](https://github.com/magicDGS/ReadTools/releases) for the software could be downloaded as a jar file. You could use that jar file without installing from source, although some developmental changes could be still not included in the jar file.

---

### Usage

To obtain a list of implemented tools run the following command:

`java -jar ReadTools.jar`

For a long description of each tool:

`java -jar ReadTools.jar <toolName> --help`


The current release includes the following tools:

* __TrimFastq__: Implementation of the trimming algorithm from [Kofler _et al._ (2011)](http://journals.plos.org/plosone/article?id=10.1371/journal.pone.0015925)
* __TaggedBamToFastq__: Convert an BAM file with BC tags into a FASTQ file
* __QualityChecker__: Get the quality encoding for a BAM/FASTQ file
* __StandardizeQuality__: Convert an Illumina BAM/FASTQ file into a Sanger
* __FastqBarcodeDetector__: Identify barcodes in the read name for a FASTQ file and assign to the ones used on the library
* __BamBarcodeDetector__:	Identify barcodes in the read name for a BAM file and assign to the ones used on the library

---

### Installation from source

To install this software you need [Maven](https://maven.apache.org/) installed in your computer. Master branch is guarantee to compile successfully and containing the later changes.

To install the later release with the updated changes, you should run the following commands:

```

git clone https://github.com/magicDGS/ReadTools.git
cd ReadTools
mvn install

```

The executable jar file will appear under the dist folder with the name _ReadTools.jar_. It could be copied to a different folder and the rest of the folder could be removed.

---

### License and citing

The software is provided with a copy of the [MIT License](http://opensource.org/licenses/MIT), but is build with several Java libraries that have different licenses. All the information for the libraries used in the sofware is available in the pom.xml file.

If you use this software, please add the citation as following (the version is printed in the header of the help):

&nbsp;&nbsp;&nbsp;&nbsp;Gómez-Sánchez D (2015): ReadTools ${version}, Institut für Populationsgenetik, Vetmeduni Vienna.

If some of the tools is a (re)implementation of a method described in a different place, the citation for the method is provided under the long description. Please, cite the method in addition to this software to give credit to the original authors.

---
*Please, if you find any problem add a new [issue](https://github.com/magicDGS/ReadTools/issues) or contact me: <daniel.gomez.sanchez@hotmail.es>
