# RSEM

RSEM is a software package for estimating gene and isoform expression levels from RNA-Seq data. It has support for multi-threaded computation, can run on single-end and paired-end data and may produce statistic values in support of it's expression analysis. For visualization, It can generate BAM and Wiggle files in both transcript-coordinate and genomic-coordinate. For visualization, user may use RSEM-made bam and wiggle files with UCSC Genome browser or IGV browser from Broad Institute. RSEM can also make transcript read depth plots in pdf format. For this workflow, only limited subset of RSEM toolkit is used. This workflow provides a building block for RNAseq data analysis pipelines.

![rsem flowchart](workflow-rsem/docs/RSEM_specs.png)

Steps implemented:

* Removing soft-clipped bases from input bam file (ngsutils, bamutils executable)
* Analyzing transcript expression profile

### Output

    ***isoforms.results - File containing isoform level expression estimates.
    ***genes.results    - File containing gene level expression estimates.
    ***transcript.bam   - File with additional ZW:f:value, where value is a 
                          single precision floating number representing the posterior probability.
                          
## Workflow Options



## Decider Options
