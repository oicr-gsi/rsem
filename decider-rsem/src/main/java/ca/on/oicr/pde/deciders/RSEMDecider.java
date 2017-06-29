package ca.on.oicr.pde.deciders;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import net.sourceforge.seqware.common.hibernate.FindAllTheFiles;
import net.sourceforge.seqware.common.module.FileMetadata;
import net.sourceforge.seqware.common.module.ReturnValue;
import net.sourceforge.seqware.common.util.Log;


public class RSEMDecider extends OicrDecider {
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    private Map<String, BeSmall> fileSwaToSmall;
    
    private String index_dir;
    private String ngsutilsPythonPath = "";
    private String additionalRsemParams = "";
    private String numOfThreads  = "6";
    private String bamutilMemory = "8000";
    private String rsemMemory    = "10000";
    private String rsemStrandedness = "none"; // For all TrueSeq stranded - derived protocols should be set to reverse
    private String provisionRsemBamFile = "true";
    
    private final static String DEFAULT_THREADS = "6";
    private final static String BAM_METATYPE = "application/bam";
    private final static String TRANSCRIPTOME_SUFFIX = "Aligned.toTranscriptome.out";
    private final static String DEFAULT_INDEXDIR = "/.mounts/labs/PDE/data/reference/hg19_random/RSEM/RSEM";
    
    public RSEMDecider() {
        super();
        fileSwaToSmall = new HashMap<String, BeSmall>();
        parser.accepts("ini-file", "Optional: the location of the INI file.").withRequiredArg();
        parser.accepts("index-dir", "reference index dir").withRequiredArg();
        
        //RSEM
        parser.accepts("rsem-threads", "Optional: RSEM threads, default is 6.").withRequiredArg();
        parser.accepts("rsem-mem-mb", "Optional: RSEM allocated memory Mb, default is 10000.").withRequiredArg();
        parser.accepts("rsem-strandedness", "Optional: RSEM strandedness, default is none.").withRequiredArg();
        parser.accepts("ngsutils-mem-mb", "Optional: ngsutils allocated memory Mb, default is 8000.").withRequiredArg();
        parser.accepts("ngsutils-pythonpath", "Optional: ngsutils needs correct PYTHONPATH set, normally user shouldn't change this.").withRequiredArg();
        parser.accepts("additionalRsemParams", "Optional: RSEM additional parameters").withRequiredArg();
        parser.accepts("template-type", "Optional: limit the run to only specified template type").withRequiredArg();
        parser.accepts("provision-rsem-bam-file", "Optional: Set the flag (true or false) to indicate if we want RSEM .bam file. Default: true").withRequiredArg();

    }

    @Override
    public ReturnValue init() {
        Log.debug("INIT");
        this.setMetaType(Arrays.asList("application/bam"));
        this.setHeadersToGroupBy(Arrays.asList(FindAllTheFiles.Header.FILE_SWA));

        //allows anything defined on the command line to override the defaults here.
        if (this.options.has("index-dir")){
            this.index_dir = options.valueOf("index-dir").toString();
        } else {
            this.index_dir = DEFAULT_INDEXDIR;
        }

        //RSEM
        if (this.options.has("rsem-threads")) {
            this.numOfThreads = options.valueOf("rsem-threads").toString();
        } else {
            this.numOfThreads = DEFAULT_THREADS;    
        }
        
        if (this.options.has("rsem-mem-mb")) {
            this.rsemMemory = options.valueOf("rsem-mem-mb").toString();
        }
        
        if (this.options.has("rsem-strandedness")) {
            this.rsemStrandedness = options.valueOf("rsem-strandedness").toString();
        }
        
        if (this.options.has("ngsutils-mem-mb")) {
            this.bamutilMemory = options.valueOf("ngsutils-mem-mb").toString();
        }
        
        if (this.options.has("ngsutils-pythonpath")) {
            this.ngsutilsPythonPath = options.valueOf("ngsutils-pythonpath").toString();
        }
        
        if (this.options.has("additional-rsem-params")) {
            this.additionalRsemParams = options.valueOf("additional-rsem-params").toString();
        }
        
         if (this.options.has("provision-rsem-bam-file")) {
            Log.debug("Setting provisioning RSEM bam file, default is true and needs to be set only in special cases");
            String tempProv = options.valueOf("provision-rsem-bam-file").toString();
            if (tempProv.equalsIgnoreCase("false") || tempProv.equalsIgnoreCase("true")) {
                this.provisionRsemBamFile = tempProv.toLowerCase();
            }
        }

        ReturnValue val = super.init();

        return val;
    }

    @Override
    protected boolean checkFileDetails(FileAttributes fa) {
        if (fa.getPath().contains(TRANSCRIPTOME_SUFFIX)) {
            return super.checkFileDetails(fa);
        } else {
            return false;
        }
    }

    @Override
    public Map<String, List<ReturnValue>> separateFiles(List<ReturnValue> vals, String groupBy) {
        // get files from study
        Map<String, ReturnValue> iusDeetsToRV = new HashMap<String, ReturnValue>();
        // Override the supplied group-by value
        for (ReturnValue currentRV : vals) {
            boolean metatypeOK = false;

            for (int f = 0; f < currentRV.getFiles().size(); f++) {
                try {
                    if (currentRV.getFiles().get(f).getMetaType().equals(BAM_METATYPE)) {
                        metatypeOK = true;
                    }
                } catch (Exception e) {
                    Log.stderr("Error checking a file");
                }
            }
            if (!metatypeOK) {
                continue; // Go to the next value
            }

            BeSmall currentSmall = new BeSmall(currentRV);
            fileSwaToSmall.put(currentRV.getAttribute(groupBy), currentSmall);
            //make sure you only have the most recent single file for each
            //sequencer run + lane + barcode + meta-type
            String fileDeets = currentSmall.getIusDetails();
            Date currentDate = currentSmall.getDate();

            //if there is no entry yet, add it
            if (iusDeetsToRV.get(fileDeets) == null) {
                iusDeetsToRV.put(fileDeets, currentRV);
            } //if there is an entry, compare the current value to the 'old' one in
            //the map. if the current date is newer than the 'old' date, replace
            //it in the map
            else {
                ReturnValue oldRV = iusDeetsToRV.get(fileDeets);
                BeSmall oldSmall = fileSwaToSmall.get(oldRV.getAttribute(FindAllTheFiles.Header.FILE_SWA.getTitle()));
                Date oldDate = oldSmall.getDate();
                if (currentDate.after(oldDate)) {
                    iusDeetsToRV.put(fileDeets, currentRV);
                }
            }
        }

        //only use those files that entered into the iusDeetsToRV
        //since it's a map, only the most recent values
        List<ReturnValue> newValues = new ArrayList<ReturnValue>(iusDeetsToRV.values());
        Map<String, List<ReturnValue>> map = new HashMap<String, List<ReturnValue>>();

        //group files according to the designated header (e.g. sample SWID)
        for (ReturnValue r : newValues) {
            String currVal = fileSwaToSmall.get(r.getAttribute(FindAllTheFiles.Header.FILE_SWA.getTitle())).getGroupByAttribute();
            List<ReturnValue> vs = map.get(currVal);
            if (vs == null) {
                vs = new ArrayList<ReturnValue>();
            }
            vs.add(r);
            map.put(currVal, vs);
        }

        return map;
    }
    
    @Override
    protected ReturnValue doFinalCheck(String commaSeparatedFilePaths, String commaSeparatedParentAccessions) {
        String[] filePaths = commaSeparatedFilePaths.split(",");
        boolean haveTranscriptBam = false;

        for (String p : filePaths) {
            for (BeSmall bs : fileSwaToSmall.values()) {
                if (!bs.getPath().equals(p)) {
                    continue;
                }
                if (!haveTranscriptBam) {haveTranscriptBam = p.contains(TRANSCRIPTOME_SUFFIX);}                   
                }

            }
        
        if (!haveTranscriptBam) {
            Log.error("The Decider was not able to find alignedToTranscriptome sequencing alignment, WON'T RUN");
            return new ReturnValue(ReturnValue.INVALIDPARAMETERS);
        }
        return super.doFinalCheck(commaSeparatedFilePaths, commaSeparatedParentAccessions);
    }
    
    @Override
    protected String handleGroupByAttribute(String attribute) {
        String a = super.handleGroupByAttribute(attribute);
        BeSmall small = fileSwaToSmall.get(a);
        if (small != null) {
            return small.getGroupByAttribute();
        }
        return attribute;
    }

    @Override
    protected boolean checkFileDetails(ReturnValue returnValue, FileMetadata fm) {
        Log.debug("CHECK FILE DETAILS:" + fm);

        if (this.options.has("template-type")) {
            if (!returnValue.getAttribute(FindAllTheFiles.Header.SAMPLE_TAG_PREFIX.getTitle() + "geo_library_source_template_type").equals(this.options.valueOf("template-type"))) {
                return false;
            }
        }   

        return super.checkFileDetails(returnValue, fm);
    }

    @Override
    protected Map<String, String> modifyIniFile(String commaSeparatedFilePaths, String commaSeparatedParentAccessions) {
        Log.debug("INI FILE:" + commaSeparatedFilePaths);

        String[] filePaths = commaSeparatedFilePaths.split(",");       
        BeSmall currentBs = null;
        for (String p : filePaths) {
            for (BeSmall bs : fileSwaToSmall.values()) {
                if (!bs.getPath().equals(p) || !bs.getPath().contains(TRANSCRIPTOME_SUFFIX)) {
                    continue;
                }
                currentBs = bs;
            }
        }
            
        // Refuse to continue if we don't have an object with metadta for one of the files
        if (null == currentBs) {
            Log.error("Was not able to retrieve fastq files for either one or two subsets of paired reads, setting mode to test");
            abortSchedulingOfCurrentWorkflowRun();
        }
        
        Map<String, String> iniFileMap = super.modifyIniFile(commaSeparatedFilePaths, commaSeparatedParentAccessions);
        iniFileMap.put("input_file", currentBs.getPath());
        iniFileMap.put("index_dir", this.index_dir);
        iniFileMap.put("additionalRSEMParams", this.additionalRsemParams);

        iniFileMap.put("rsem_threads",     this.numOfThreads);
        iniFileMap.put("rsem_mem_mb",      this.rsemMemory);
        iniFileMap.put("rsem_strandedness",this.rsemStrandedness);
        iniFileMap.put("ngsutils_mem_mb",  this.bamutilMemory);

        iniFileMap.put("ius_accession", currentBs.getIus_accession());
        iniFileMap.put("sequencer_run_name", currentBs.getSequencer_run_name());
        iniFileMap.put("lane", currentBs.getLane());
        iniFileMap.put("barcode", currentBs.getBarcode());
        iniFileMap.put("library", currentBs.getRGLB());
        iniFileMap.put("provision_rsem_bam_file", this.provisionRsemBamFile);

        //PYTHONPATH is configured in the dafault ini and should not be normally changed
        if (!this.ngsutilsPythonPath.isEmpty()) {
            iniFileMap.put("ngsutils_pythonpath", this.ngsutilsPythonPath);
        }
        return iniFileMap;
    }

   
    public static void main(String args[]) {

        List<String> params = new ArrayList<String>();
        params.add("--plugin");
        params.add(RSEMDecider.class.getCanonicalName());
        params.add("--");
        params.addAll(Arrays.asList(args));
        System.out.println("Parameters: " + Arrays.deepToString(params.toArray()));
        net.sourceforge.seqware.pipeline.runner.PluginRunner.main(params.toArray(new String[params.size()]));

    }

    private class BeSmall {

    private Date date = null;
    private String iusDetails = null;
    private String groupByAttribute = null;
    private String tissueType = null;
    private String path = null;
    private String tubeID = null;
    private String groupID = null;
    private String groupDescription = null;
    private final String RGLB;
    private final String RGPU;
    private final String ius_accession;
    private final String sequencer_run_name;
    private final String barcode;
    private final String lane;

    //TODO get a hold on alignedToTranscript bam
    public BeSmall(ReturnValue rv) {
        try {
            this.date = format.parse(rv.getAttribute(FindAllTheFiles.Header.PROCESSING_DATE.getTitle()));
        } catch (ParseException ex) {
            Log.error("Bad date!", ex);
            ex.printStackTrace();
        }

        FileAttributes fa = new FileAttributes(rv, rv.getFiles().get(0));
        this.tissueType = fa.getLimsValue(Lims.TISSUE_TYPE);
        this.tubeID = fa.getLimsValue(Lims.TUBE_ID);
        if (null == this.tubeID || this.tubeID.isEmpty()) {
            this.tubeID = "NA";
        }
        this.groupID = fa.getLimsValue(Lims.GROUP_ID);
        if (null == this.groupID || this.groupID.isEmpty()) {
            this.groupID = "NA";
        }
        this.groupDescription = fa.getLimsValue(Lims.GROUP_DESC);
        if (null == this.groupDescription || this.groupDescription.isEmpty()) {
            this.groupDescription = "NA";
        }

        this.lane = fa.getLane().toString();
        this.RGLB = fa.getLibrarySample();
        this.RGPU = fa.getSequencerRun() + "_" + this.lane + "_" + fa.getBarcode();
        this.iusDetails = this.RGLB + this.RGPU + rv.getAttribute(FindAllTheFiles.Header.FILE_SWA.getTitle());
        this.ius_accession = rv.getAttribute(FindAllTheFiles.Header.IUS_SWA.getTitle());
        this.sequencer_run_name = fa.getSequencerRun();
        this.barcode = fa.getBarcode();

        StringBuilder gba = new StringBuilder(fa.getDonor());
        gba.append(":").append(fa.getLimsValue(Lims.LIBRARY_TEMPLATE_TYPE));
        gba.append(":").append(this.ius_accession);

        String trs = fa.getLimsValue(Lims.TARGETED_RESEQUENCING);
        if (null != trs && !trs.isEmpty()) {
            gba.append(":").append(trs);
        }

        this.groupByAttribute = gba.toString();
        this.path = rv.getFiles().get(0).getFilePath() + "";
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getGroupByAttribute() {
        return this.groupByAttribute;
    }

    public void setGroupByAttribute(String groupByAttribute) {
        this.groupByAttribute = groupByAttribute;
    }

    public String getTissueType() {
        return this.tissueType;
    }

    public String getIusDetails() {
        return this.iusDetails;
    }

    public void setIusDetails(String iusDetails) {
        this.iusDetails = iusDetails;
    }

    public String getPath() {
        return this.path;
    }

    public String getTubeId() {
        return this.tubeID;
    }

    public String getGroupID() {
        return this.groupID;
    }

    public String getGroupDescription() {
        return this.groupDescription;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRGLB() {
        return RGLB;
    }

    public String getRGPU() {
        return RGPU;
    }

    public String getIus_accession() {
        return ius_accession;
    }

    public String getSequencer_run_name() {
        return sequencer_run_name;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getLane() {
        return lane;
    }
}
}
