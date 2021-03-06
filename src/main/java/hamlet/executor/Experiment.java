package hamlet.executor;

import com.opencsv.CSVWriter;
import hamlet.base.Attribute;
import hamlet.base.DatasetSchema;
import hamlet.users.stockUser.stockAttributeEnum;
import lombok.Data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Data
public class Experiment {

    private DatasetSchema schema;
    private int defaultEPW = 50000;
    private int defaultWorkloadSize = 50;
    private String streamFile;
    private boolean isLocal = true;
    private String workloadPath;

    public Experiment(boolean isLocal){
        this.schema = new DatasetSchema();
        for (stockAttributeEnum a: stockAttributeEnum.values()){
            this.schema.addAttribute(new Attribute(a.toString()));
        }
        this.isLocal = isLocal;
        this.workloadPath = this.isLocal?"src/main/resources/Nasdaq/":"Workload/";
        this.streamFile = this.isLocal?"src/main/resources/Nasdaq/Nasdaq_sample.csv": "Stream/Nasdaq_sample.csv";

    }

    public void varyEpw(){
        String workloadFile = workloadPath+"Workload_"+this.defaultWorkloadSize+".txt";
        String logFile = "output/varyEpw.csv";

        for (int epw = 50000; epw < 110000; epw +=10000){

            for (int iter = 1;iter < 4; iter++) {
                Executor executor = new Executor(this.schema, epw, workloadFile, streamFile);
                executor.workloadAnalysis(workloadFile);
                executor.streamPartitioning(streamFile);

                executor.run();

                logging(executor, epw, iter, logFile);
            }

        }
    }

    public void varyQueryNum(){
        String logFile = "output/varyQueryNum.csv";


        for (int workloadSize = 20; workloadSize < 110; workloadSize +=10){
            String workloadFile = this.workloadPath+"Workload_"+workloadSize+".txt";

            for (int iter = 1;iter < 4; iter++) {
                int epw = defaultEPW;
                Executor executor = new Executor(this.schema, epw, workloadFile, this.streamFile);
                executor.workloadAnalysis(workloadFile);
                executor.streamPartitioning(this.streamFile);

                executor.run();

                logging(executor, epw, iter, logFile);
            }


        }
    }



    /**
     * logging method for Dynamic versus Static Sharing Decision.
     * @param executor the executor that run dynamic vs. static
     * @param epw the events per Window
     * @param iter iteration number
     * @param logFile the log file
     */
    public void logging(Executor executor, int epw, int iter, String logFile){

        String[] header = { "iter", "epw","# queries",
                "Workload Analysis Overhead", "Template Construction Overhead",
                "Static Ham throughput","Dynamic Ham throughput",
                "Static Ham throughput(relevant events)","Dynamic Ham throughput(relevant events)",
                "Static Ham execution time","Dynamic Ham optimizer time", "Dynamic Ham execution time",
                "Static Ham memory", "Dynamic Ham memory",
                "Merge num","Split num",
                "Graphlet Num", "Merged Graphlet Num",
                "Static Snapshot Num","Dynamic Snapshot Num"
        };
        String[] data = new String[header.length];
        int i =0;

        data[i] = iter+"";        i++;
        data[i] = epw+"";        i++;
        data[i] = executor.getWholeWorkload().getQueries().size()+"";   i++;

        // overhead
        data[i] = executor.getWorkloadAnalyzeDuration()+"";     i++;
        data[i] = executor.getTemplateGenerationDuration()+"";  i++;

        // throughput
        data[i] = ((float)(executor.getEpw()*1000)/ executor.getStaticLatency()) +"";        i++;
        data[i] = ((float)(executor.getEpw()*1000)/ (executor.getDynamicOptTime()+executor.getDynamicExcTime())) +"";        i++;

        //actual throughput(relevant events)
        data[i] = ((float)(executor.getStaticRelevantEvents()*1000)/ executor.getStaticLatency()) +"";        i++;
        data[i] = ((float)(executor.getDynamicRelevantEvents()*1000)/ (executor.getDynamicOptTime()+executor.getDynamicExcTime())) +"";         i++;

        //execution time
        data[i] = executor.getStaticLatency()+"";        i++;
        data[i] = executor.getDynamicOptTime()+"";        i++;
        data[i] = executor.getDynamicExcTime()+"";        i++;

        //memory
        data[i] = executor.getStaticMemory()+"";        i++;
        data[i] = executor.getDynamicMemory()+"";    i++;

        //merge, split burst number
        data[i] = executor.getMergeNum()+"";    i++;
        data[i] = executor.getSplitNum()+"";    i++;

        //graphlet number
        data[i] = executor.getGraphletNum()+"";     i++;
        data[i] = executor.getMergedGraphletNum()+"";   i++;

        //snapshot numbers
        data[i] = executor.getStaticSnapshotNum()+"";   i++;
        data[i] = executor.getDynamicSnapshotNum()+"";



        checkFolder("output/"+logFile.split("/")[0]);

        File file = new File("output/"+ logFile);

        writeCSV(file,header, data);
    }



    /**
     * create the folder if it doesn't exist
     * @param path path of the directory
     */
    void checkFolder(String path){

        File folder = new File(path);

        if (!folder.exists() && !folder.isDirectory()) {
            folder.mkdirs();
            System.out.println("directory created");
        } else {
            System.out.println();
        }

    }


    /**
     * method of writing a line into a csv file
     * @param file file name
     * @param header header of the csv
     * @param data a line of data
     */
    public void writeCSV(File file, String[] header, String[] data){
        try {
            if(!file.exists()){
                file.createNewFile();
                FileWriter outputfile = new FileWriter(file, true);
                CSVWriter writer = new CSVWriter(outputfile);
                writer.writeNext(header);
                writer.close();
            }
            FileWriter fileWriter = new FileWriter(file, true);
            CSVWriter writer = new CSVWriter(fileWriter);
            //write the data
            writer.writeNext(data);
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
