---
title: Barcodes
permalink: barcodes.html
---

## How _ReadTools_ handle barcode information

_ReadTools_ handle automatically barcode information in the read name for FASTQ files with Illumina legacy and Casava formatting.
For SAM/BAM/CRAM files, we assume that the sample barcode is encoded in the 'BC' tag as described in the [SAM specifications]({{site.data.formats.specs.sam}}).
In case that these files were obtained from FASTQ files without _ReadTools_, it may be possible that the barcode is still attached to the read name. Please, use the option `--barcodeInReadName` for that input formats.

---

## Barcode file format
The barcode file format of _ReadTools_ follows the same format as the [ExtractIlluminaBarcodes](https://broadinstitute.github.io/picard/command-line-overview.html#ExtractIlluminaBarcodes) tool from [Picard]({{site.data.software.picard}}): tab-delimited table with named columns for including information. It could contain other columns, but the following are used for assigning Read Groups by barcode:

### Required

- **barcode_sequence** or **barcode_sequence_1**: sequence for the first barcode.
- **barcode_sequence_2**: sequence for the second barcode in dual indexed libraries. Only required if more than one barcode is expected.
- **sample_name** or **barcode_name**: name for the sample, which will appear in the `SM` Read Group field.

### Optional

- **library_name**: the name for the library, which will appear in the `LB` Read Group field.

### Example file

```
sample_name	library_name	barcode_sequence	barcode_sequence_2
sample1 lib1	ATTACTCG	ATAGAGGC
sample2	lib2	TCCGGAGA	CCTATCCT
sample3	lib3	CGCTCATT	GGCTCTGA
sample4	lib4	GAGATTCC	AGGCGAAG
sample5	lib5	ATTCAGAA	TAATCTTA
sample6	lib1	GAATTCGT	CAGGACGT
sample7	lib2	CTGAAGCT	GTACTGAC
sample8	lib3	TAATGCGC	TATAGCCT
sample9	lib4	CGGCTATG	ATAGAGGC
sample1	lib5	TCCGCGAA	CCTATCCT
```

{% include warning.html content="All the barcodes present in the multiplexed file should be included in the barcode column" %}
