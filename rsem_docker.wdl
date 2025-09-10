version 1.0


workflow rsem {
input {
  File inputBam
  String outputFileNamePrefix
  String reference
}

  Map[String, Array[File]] referenceFiles = {
    "hg19": [
      "/home/ubuntu/module_data/rsem_data/hg19/hg19_random_rsem.chrlist",
      "/home/ubuntu/module_data/rsem_data/hg19/hg19_random_rsem.grp", 
      "/home/ubuntu/module_data/rsem_data/hg19/hg19_random_rsem.idx.fa",
      "/home/ubuntu/module_data/rsem_data/hg19/hg19_random_rsem.n2g.idx.fa",
      "/home/ubuntu/module_data/rsem_data/hg19/hg19_random_rsem.seq",
      "/home/ubuntu/module_data/rsem_data/hg19/hg19_random_rsem.ti",
      "/home/ubuntu/module_data/rsem_data/hg19/hg19_random_rsem.transcripts.fa"
    ],
    "hg38": [
      "/home/ubuntu/module_data/rsem_data/hg38/hg38_random_rsem.chrlist",
      "/home/ubuntu/module_data/rsem_data/hg38/hg38_random_rsem.grp",
      "/home/ubuntu/module_data/rsem_data/hg38/hg38_random_rsem.idx.fa", 
      "/home/ubuntu/module_data/rsem_data/hg38/hg38_random_rsem.n2g.idx.fa",
      "/home/ubuntu/module_data/rsem_data/hg38/hg38_random_rsem.seq",
      "/home/ubuntu/module_data/rsem_data/hg38/hg38_random_rsem.ti",
      "/home/ubuntu/module_data/rsem_data/hg38/hg38_random_rsem.transcripts.fa"
    ]
  }
  
  call runRsem {
    input:
      inputFile = inputBam,
      sampleID = outputFileNamePrefix,
      referenceFiles = referenceFiles[reference]
  }

parameter_meta {
  inputBam: "Input BAM file with aligned RNAseq reads."
  outputFileNamePrefix: "Output prefix, customizable. Default is the input file's basename."
  reference: "Name and version of reference genome"
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
  Array[File] referenceFiles
  String docker = "rsem:1.3.3"
  Int jobMemory = 12
}

parameter_meta {
 inputFile: "Input .bam file for analysis sample"
 jobMemory: "Memory in Gb for this job"
 docker: "Names and versions of docker"
}

command <<<
  
  # Create symlinks to reference files in working directory
  for file in ~{sep=' ' referenceFiles}; do
    ln -s "$file" $(basename "$file")
  done
  
  # Extract prefix from first file
  PREFIX=$(basename ~{referenceFiles[0]} .chrlist)
  echo "Using prefix: ${PREFIX}"
 rsem-calculate-expression --bam --paired-end ~{inputFile} ${PREFIX} ~{sampleID}
>>>

runtime {
  memory:  "~{jobMemory} GB"
  docker: "~{docker}"
}

output {
  File genes    = "~{sampleID}.genes.results"
  File isoforms = "~{sampleID}.isoforms.results"
  File transcriptBam = "~{sampleID}.transcript.bam"
}
}

