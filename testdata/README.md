# Example Data for _ReadTools_

Data in this folder contain modified read data from 
[SRR1931701](https://www.ncbi.nlm.nih.gov/sra/?term=SRR1931701).

## Read sources

Files containing reads in different formats.

| File name                 | Format | Quality encoding | Read name format | Single/Paired  | Barcode-index | # Reads | More details |
|:------------------------- | :----: | :--------------: | :--------------: | :------------: | :-----------: | :-----: | :----------- |
| SRR1931701_1.fq           | FASTQ  | Standard         | Illumina-legacy  | First of pair  | Single        | 103     | Barcode attached to read name. |
| SRR1931701_2.fq           | FASTQ  | Standard         | Illumina-legacy  | Second of pair | Single        | 103     | Barcode attached to read name. |
| SRR1931701.illumina_1.fq  | FASTQ  | Illumina         | Illumina-legacy  | First of pair  | Dual          | 4       | Barcodes attached to read name, separated by hyphens. |
| SRR1931701.illumina_2.fq  | FASTQ  | Illumina         | Illumina-legacy  | Second of pair | Dual          | 4       | Barcodes attached to read name, separated by hyphens. |
| SRR1931701.illumina_se.fq | FASTQ  | Illumina         | Illumina-legacy  | Single-end     | Dual          | 4       | Barcodes attached to read name, separated by hyphens. |
| SRR1931701.casava.fq      | FASTQ  | Standard         | Casava           | Single-end     | Single        | 103     | Barcode in the Casava name. |
| SRR1931701.interleaved.fq | FASTQ  | Illumina         | Illumina-legacy  | Pair-end       | Single        | 10      | Barcodes attached to read name, separated by hyphens. |