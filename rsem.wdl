version 1.0

struct GenomeResources {
    String modules
    String data_modules
    String reference_indexdir
}


workflow rsem {
input {
  File inputBam
  String outputFileNamePrefix
  String reference
  String local_code_modulefile_path = "/home/ubuntu/local_modules/gsi/modulator/modulefiles/Ubuntu24.04"
  String local_data_modulefile_path = "/home/ubuntu/local_modules/gsi/modulator/modulefiles/data"
}

Map[String, GenomeResources] resources = {
  "hg19": {
    "modules": "rsem/1.3.3a",
    "data_modules": "hg19-rsem-index/1.3.3",
    "reference_indexdir": "$HG19_RSEM_INDEX_ROOT/hg19_random_rsem"
  },
  "hg38": {
    "modules": "rsem/1.3.3a",
    "data_modules": "hg38-rsem-index/1.3.3",
    "reference_indexdir": "$HG38_RSEM_INDEX_ROOT/hg38_random_rsem"
  }
}

# run RSEM
call runRsem { input: inputFile = inputBam, sampleID = outputFileNamePrefix, modules = resources[reference].modules, data_modules = resources[reference].data_modules, rsemIndexDir = resources[reference].reference_indexdir, local_code_modulefile_path = local_code_modulefile_path, local_data_modulefile_path = local_data_modulefile_path }

parameter_meta {
  inputBam: "Input BAM file with aligned RNAseq reads."
  outputFileNamePrefix: "Output prefix, customizable. Default is the input file's basename."
  reference: "Name and version of reference genome"
  local_code_modulefile_path: "Path to locally build code modulefiles"
  local_data_modulefile_path: "Path to locally build data modulefiles"
}

meta {
  author: "Peter Ruzanov"
  email: "peter.ruzanov@oicr.on.ca"
  description: "RSEM is a software package for estimating gene and isoform expression levels from RNA-Seq data. It has support for multi-threaded computation, can run on single-end and paired-end data and may produce statistic values in support of it's expression analysis. For visualization, It can generate BAM and Wiggle files in both transcript-coordinate and genomic-coordinate. For visualization, user may use RSEM-made bam and wiggle files with UCSC Genome browser or IGV browser from Broad Institute. RSEM can also make transcript read depth plots in pdf format. For this workflow, only limited subset of RSEM toolkit is used. This workflow provides a building block for RNAseq data analysis pipelines."
  dependencies: [
      {
        name: "rsem/1.3.3",
        url: "https://github.com/deweylab/RSEM"
      }
    ]
    output_meta: {
    geneResults: {
        description: "expression levels for all genes recorded in the reference",
        vidarr_label: "geneResults"
    },
    isoformResults: {
        description: "expression levels for all isoforms recorded in the reference",
        vidarr_label: "isoformResults"
    },
    transcriptBam: {
        description: "BAM file with additional ZW:f:value, a single precision floating number representing the posterior probability",
        vidarr_label: "transcriptBam"
    }
}
}

output {
  File geneResults = runRsem.genes
  File isoformResults = runRsem.isoforms
  File transcriptBam = runRsem.transcriptBam 
}

}

# ==========================
#  configure and run RSEM
# ==========================
task runRsem {
input {
  File inputFile
  String sampleID
  String rsemIndexDir
  String modules
  String local_code_modulefile_path
  String local_data_modulefile_path
  String data_modules
  Int timeout = 48
  Int jobMemory = 12
}

parameter_meta {
 inputFile: "Input .bam file for analysis sample"
 jobMemory: "Memory in Gb for this job"
 rsemIndexDir: "Base of RSEM indexes, includes the directory common file prefix"
 modules: "Names and versions of modules"
 timeout: "Timeout in hours, needed to override imposed limits"
 local_code_modulefile_path: "Path to locally build code modulefiles"
 local_data_modulefile_path: "Path to locally build data modulefiles"
}

command <<<
  . /usr/share/modules/init/bash
  module use ~{local_code_modulefile_path }
  module load ~{modules}
  module use ~{local_data_modulefile_path }
  module load ~{data_modules}
 $RSEM_ROOT/bin/rsem-calculate-expression --bam --paired-end ~{inputFile} ~{rsemIndexDir} ~{sampleID}
>>>

runtime {
  memory:  "~{jobMemory} GB"
  modules: "~{modules}"
  timeout: "~{timeout}"
}

output {
  File genes    = "~{sampleID}.genes.results"
  File isoforms = "~{sampleID}.isoforms.results"
  File transcriptBam = "~{sampleID}.transcript.bam"
}
}

