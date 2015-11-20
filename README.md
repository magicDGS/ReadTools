[![Build Status](https://travis-ci.org/magicDGS/ReadTools.svg?branch=master)](https://travis-ci.org/magicDGS/ReadTools)
# ReadTools

Tools for sequencing data (BAM and FASTQ files)

---

The tools contained in this software are develop for working with FASTQ and BAM files (no mapped, although some of the tools will work with them) for different pre-processing of the data. The aim is to provide a set of tools for trimming, barcoding and quality-related pipelines, but not adaptor removal or barcode identification from scratch.

---

### Installation

To install this software you need [Maven](https://maven.apache.org/) installed in your computer. In the downloaded folder, to install the later release you should run the following command:

`mvn install`

The executable jar file will appear under the dist folder with the name _ReadTools.jar_. It could be copied to a different folder and the rest of the folder could be removed.

---

### Usage

To obtain a list of implemented tools run the following command:

`java -jar ReadTools.jar`

For a long description of each tool:

`java -jar ReadTools.jar <toolName> --help`

---

### License and citing

The software is provided with a copy of the [MIT License](http://opensource.org/licenses/MIT), but is build with several Java libraries that have different licenses. All the information for the libraries used in the sofware is available in the pom.xml file.

If you use this software, please add the citation as following (the version is printed in the header of the help):

&nbsp;&nbsp;&nbsp;&nbsp;Gómez-Sánchez D (2015): ReadTools ${version}, Institut für Populationsgenetik, Vetmeduni Vienna.

If some of the tools is a (re)implementation of a method described in a different place, the citation for the method is provided under the long description. Please, cite the method in addition to this software to give credit to the original authors.
