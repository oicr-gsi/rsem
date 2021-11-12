#!/bin/bash
cd $1

# We have two .results files and a transcriptome .bam file

echo ".results files:"
find . -name '*.results' | xargs md5sum | sort -V

# For bam file we do the md5sum since it's
module load samtools/1.9 1>/dev/null

echo "transcriptome .bam file:"
find . -name *.bam | xargs md5sum
