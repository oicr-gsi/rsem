# rsem

RSEM 2.0, workflow for accurate quantification of gene and isoform expression from RNA-Seq data

## Overview

## Dependencies

* [rsem 1.3.3](https://github.com/deweylab/RSEM)


## Usage

### Cromwell
```
java -jar cromwell.jar run rsem.wdl --inputs inputs.json
```

### Inputs

#### Required workflow parameters:
Parameter|Value|Description
---|---|---
`inputBam`|File|Input BAM file with aligned RNAseq reads.
`outputFileNamePrefix`|String|Output prefix, customizable. Default is the input file's basename.
`reference`|String|Name and version of reference genome


#### Optional workflow parameters:
Parameter|Value|Default|Description
---|---|---|---


#### Optional task parameters:
Parameter|Value|Default|Description
---|---|---|---
`runRsem.timeout`|Int|48|Timeout in hours, needed to override imposed limits
`runRsem.jobMemory`|Int|12|Memory in Gb for this job


### Outputs

Output | Type | Description
---|---|---
`geneResults`|File|expression levels for all genes recorded in the reference
`isoformResults`|File|expression levels for all isoforms recorded in the reference
`transcriptBam`|File|BAM file with additional ZW:f:value, a single precision floating number representing the posterior probability


## Commands
 
 This section lists command(s) run by rsem workflow
 
 * Running rsem
 
 rsem workflow runs the following command (excerpt from .wdl file). $RSEM_ROOT pints to rsem's installation directory and defined
 by the module which a user chooses to use (this is specific to OICR environment). 
  
  * INPUT_FILE     is a placeholder for an input file.
  * RSEM_INDEX_DIR is a placeholder for a direcory with RSEM reference files
  * SAMPLE_ID      is a placeholder for a sample id
 
 ```
 $RSEM_ROOT/bin/rsem-calculate-expression --bam --paired-end INPUT_FILE RSEM_INDEX_DIR SAMPLE_ID
 
 ```
 
 ## Support

For support, please file an issue on the [Github project](https://github.com/oicr-gsi) or send an email to gsi@oicr.on.ca .

_Generated with generate-markdown-readme (https://github.com/oicr-gsi/gsi-wdl-tools/)_
