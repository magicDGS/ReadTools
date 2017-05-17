---
title: Read Sources
permalink: read_sources.html
summary: Towards a standard way of storing reads
---

Diverse formats were developed for storing both raw and mapped reads, but there is no consensus yet about which one is the best. When _ReadTools_ started to be developed, the most common way of storing reads was [FASTQ] for raw datasets and [SAM] for already mapped/processed data.

The [SAM]({{site.data.formats.sam}}) format is well-defined and has a good support in the community ([SAM specs]({{site.data.formats.specs.sam}})), in contrast with the implementation dependent [FASTQ]({{site.data.formats.fastq}}) format. In addition, several programs widely used in bioinformatics are moving towards unmapped reads in [SAM]({{site.data.formats.sam}}) formatting, such as [Picard Tools]({{site.data.software.picard}}) or [bwa]({{site.data.software.bwa}}) (aln). _ReadTools_ use a consistent format based on the [SAM specs] for output reads, and handle other sources to standardize them.

## Supported formats
The following formats are accepted in _ReadTools_ native tools:

* [FASTQ]({{site.data.formats.fastq}})
* [SAM]({{site.data.formats.sam}})/BAM
* [CRAM]({{site.data.formats.cram}})

## Future support

{% include important.html content="Requests for new formats are welcome." %}

The following formats are not accepted yet, but there are plans to support them in the future:

* [PacBio HDF5]({{site.data.formats.specs.pacbio_hdf5}}): the legacy PacBio format was stored in HDF5, but currently they use a new BAM format. We plan to support `bas.h5` input files to allow the conversion between legacy and current pipelines.
* [SRA]({{site.data.formats.sra}}): the NCBI format to store public datasets. Supporting this source of reads will be important for downloading already standardized data. This support depends on a native library, and may require the download of libraries the first time is used.
