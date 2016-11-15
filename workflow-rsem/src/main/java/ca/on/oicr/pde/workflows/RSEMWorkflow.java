package ca.on.oicr.pde.workflows;

import ca.on.oicr.pde.utilities.workflows.SemanticWorkflow;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.seqware.pipeline.workflowV2.model.Job;
import net.sourceforge.seqware.pipeline.workflowV2.model.SqwFile;

public class RSEMWorkflow extends SemanticWorkflow {

    String input_path = null;
    String rsem_index_dir = null;
    String dataDir = null;
    String outputFileName = null;
    boolean manualOutput;
    //bamutils and RSEM parameters
    String RSEMDIR;
    String bamutils;
    
    int numOfThreads;
    SqwFile inputFile;
    SqwFile outputTranscripts;
    SqwFile outputIsoforms;
    SqwFile outputGenes;
    
    String queue;

    private final static String DEFAULT_THREADS = "6";
    private final static String RSEM_GENES  = "genes.results";
    private final static String RSEM_ISO    = "isoforms.results";
    private final static String RSEM_TRANSCRIPTS = "transcript.bam";
    private final static String BAM = "application/bam";
    private final static String TXT = "text/plain";

    //Ontology-related variables
    private static final String EDAM = "EDAM";
    private static final Map<String, Set<String>> cvTerms;
    
        static {
        cvTerms = new HashMap<String, Set<String>>();
        cvTerms.put(EDAM, new HashSet<String>(Arrays.asList("BAM", "BAI","Text","Alignment format",
                                                            "Sequence alignment","Gene expression")));
    }
        
    /**
     * Function that returns CV terms put into a Map container
     * @return myTerms (list of terms)
     */
    @Override
    protected Map<String, Set<String>> getTerms() {
       Map<String, Set<String>> myTerms = new HashMap<String, Set<String>>();
       myTerms.putAll(cvTerms);
       return myTerms;
    }
    
    @Override
    public Map<String, SqwFile> setupFiles() {

        try {

            RSEMDIR    = getProperty("rsem_dir");
            if (!RSEMDIR.endsWith("/")) {RSEMDIR+="/";}
            
            bamutils   = getProperty("bamutils");
            input_path = getProperty("input_file");
            rsem_index_dir = getProperty("index_dir"); //index dir also need to include the prefix used to build index files
            manualOutput = Boolean.valueOf(getOptionalProperty("manual_output", "false"));
            numOfThreads = Integer.valueOf(getOptionalProperty("rsem_threads", DEFAULT_THREADS));
            queue = getOptionalProperty("queue", "");

            if (hasPropertyAndNotNull("outputFileName") && !getProperty("outputFileName").isEmpty()) {
                outputFileName = getProperty("outputFileName");
            } else {
		outputFileName = "SWID_" + getProperty("ius_accession") + "_" 
             	+ getProperty("library") + "_" + getProperty("sequencer_run_name") + "_" + getProperty("barcode") 
             	+ "_L00" + getProperty("lane");
            }


        } catch (Exception e) {
            Logger.getLogger(RSEMWorkflow.class.getName()).log(Level.SEVERE, null, e);
            System.exit(1);
        }

        //registers input and output files
        inputFile  = this.createFile("input_file");
        inputFile.setSourcePath(input_path);
        inputFile.setType(BAM);
        inputFile.setIsInput(true);
        
        outputTranscripts = createOutputFile(this.dataDir + outputFileName + "." + RSEM_TRANSCRIPTS, BAM, manualOutput);
        outputGenes       = createOutputFile(this.dataDir + outputFileName + "." + RSEM_GENES, TXT, manualOutput);
        outputIsoforms    = createOutputFile(this.dataDir + outputFileName + "." + RSEM_ISO, TXT, manualOutput);

        return this.getFiles();
    }

    @Override
    public void setupDirectory() {
        dataDir = getOptionalProperty("data_dir","data");
        if (!dataDir.endsWith("/")) {dataDir+="/";}
        this.addDirectory(dataDir);
    }

    @Override
    public void buildWorkflow() {
         
        // Step 1.  To run RSEMDIR on Transcriptome-aligned reads we need to remove soft-clipped bases
        Job job01 = this.getWorkflow().createBashJob("rm_softclip_bases");
        String unclipped_path = this.dataDir + this.outputFileName + ".noclip.bam";
        
        job01.setCommand(bamutils + " removeclipping ");
        job01.getCommand().addArgument(inputFile.getProvisionedPath());
        job01.getCommand().addArgument(unclipped_path);
        job01.setMaxMemory(getProperty("ngsutils_mem_mb"));
        job01.setQueue(queue);
        
        // Step 2. Run RSEMDIR. It will produce a number of files, we provision everything
        Job job02 = this.getWorkflow().createBashJob("run_rsem");
        job02.setCommand(RSEMDIR + "rsem-calculate-expression "
                        + "--bam "
                        + "--paired-end "
                        + unclipped_path + " "
                        + this.rsem_index_dir + " "
                        + this.dataDir + outputFileName);
        
        String addParams = parameters();
        if (null != addParams) {
            job02.getCommand().addArgument(addParams);
        }
        
        job02.addParent(job01);
        if (this.numOfThreads > 1) {
         job02.getCommand().addArgument("-p " + this.numOfThreads);
         job02.setThreads(this.numOfThreads);
        }
        job02.setMaxMemory(getProperty("rsem_mem_mb"));
        job02.setQueue(queue);
        
        this.attachCVterms(outputTranscripts, EDAM, "BAM,Sequence alignment,Alignment format");
        this.attachCVterms(outputGenes, EDAM, "Text,Gene expression");
        this.attachCVterms(outputIsoforms, EDAM, "Text,Gene expression");
        
	job02.addFile(outputTranscripts);
        job02.addFile(outputGenes);
        job02.addFile(outputIsoforms);
        
    }
       
    /**
     * A function for handling additional parameters
     * 
     * @return String
     */
    public String parameters() {

        String paramCommand = null;
        StringBuilder a = new StringBuilder();

        try {
                // If strandedness defined (we have TrueSeq Stranded protocol, for example) set the RSEMDIR parameter
                if (hasPropertyAndNotNull("rsem_strandedness") && !getProperty("rsem_strandedness").isEmpty()) {
                   String rsem_strand = getProperty("rsem_strandedness").toLowerCase();
                   if (!rsem_strand.equals("none")) {
                       a.append(" --strandedness ");
                       a.append(rsem_strand);
                   }
                }
            
                // Set additional RSEMDIR parameters if requested
                if (hasPropertyAndNotNull("additionalRSEMParams") && !getProperty("additionalRSEMParams").isEmpty()) {
                   String rsem_params = getProperty("additionalRSEMParams");
                   a.append(" ");
                   a.append(rsem_params);
                }
                                      
                paramCommand = a.toString();
                return paramCommand; 

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return paramCommand;
    }

}
