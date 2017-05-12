---
title: ReadTools
keywords: ReadTools homepage
sidebar: home_sidebar
permalink: index.html
summary: A universal toolkit for handling sequence data from different sequencing platforms
toc: false
---

_ReadTools_ provides a consistent and highly tested set of tools for processing sequencing data from any kind of source and focusing on raw reads, while including tools for mapped reads as well.

Diverse formats were developed for storing reads (see [Read sources](read_sources.html)), but _ReadTools_ opt for following the [SAM specs]({{site.data.formats.specs.sam}}) to maintain a common data format to store both raw/mapped reads. Thus, _ReadTools_ also helps to standardize sequencing data in different formats.

## Download

Executable jar files for all released versions can be found under [Releases]. For using unreleased versions, see the [README]({{site.data.repo.readme}}).

Both released and unreleased changes are reported in the [CHANGELOG]({{site.data.repo.changelog}}).

## Main usage
To obtain a list of implemented tools run the following command:

> `java -jar ReadTools.jar`

For a long description of each tool:

> `java -jar ReadTools.jar <toolName> --help`

For getting the packaged version:

> `java -jar ReadTools.jar --version` or `java -jar ReadTools.jar -v`


---

## Bug reports

Please, if you find any problem use our [issue tracker]({{site.data.repo.issue_tracker}}) or contact by email <daniel.gomez.sanchez@hotmail.es>
