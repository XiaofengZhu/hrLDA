package com.hrLDA.readdocs;

import java.io.File;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.hrLDA.mysql.MySqlDB;
import com.hrLDA.util.FileUtil;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;


public class ConvertDocs{
	
    private  AbstractSequenceClassifier<CoreLabel> classifier;
    
    private  String  serializedClassifierPath = "classifiers"+File.separator+
    		"english.muc.7class.distsim.crf.ser.gz";
    
	private  MySqlDB mySqlDB;
	private String fileExtension;
	private int numOfThreads;
	
    public ConvertDocs (String databaseName, String username, String password, 
    		String fileTableName, String relationTableName, String fileExtension,
    		int numOfThreads){
        this.mySqlDB= new MySqlDB(databaseName,username,password,fileTableName, relationTableName);
        this.mySqlDB.createTable();   
        this.mySqlDB.createRelationTable();  
        this.fileExtension = fileExtension;
        this.numOfThreads =numOfThreads;
    }// end of ConvertDocs (...)    
    
    /**
     * concert documents with different extensions to .txt format files
     * @param corpusDir
     * @throws Exception
     */
	public void Convert(String corpusDir)throws Exception{

        String tempfolderPathString=".."+File.separator+"temp"+File.separator;
        // invoke function from com.xiaofeng.FileUtil
        FileUtil.createTempFolder(tempfolderPathString);
        System.out.println("tempfolderPathString is "+tempfolderPathString);
        
        boolean moveOn = FileUtil.checkClassifierFile();
        if (!moveOn) System.exit(2);
        
        // record total number of qualified documents
        int totalCounts =0;
        
    	/////////////////////////////////////////////////////////////////////        

        try{
        	
        	classifier = CRFClassifier.getClassifier(this.serializedClassifierPath);
        	
        	FileList fileList = new FileList(fileExtension);
        	FileReaderRunnable[] runnables = new FileReaderRunnable[numOfThreads];
			
			
			if (numOfThreads > 1) {	
				
				fileList.getFileList(corpusDir);
				Vector<String> complexResult = fileList.getComplexFileVector();
				
				int thread =0;
				if (complexResult.size() >0){
					runnables[0] = new FileReaderRunnable( complexResult, tempfolderPathString,
							mySqlDB, classifier, 0);

					thread++;
				}

				
				Vector<String> result = fileList.getSimpleFileVector();
				//divide result vector into different threads
				
				if (result.size() >0){
					numOfThreads =2;// set to the fixed number because of NER does not allow multi-thread accesses 
					int docsPerThread = result.size() / (numOfThreads-1);
					int offset = 0;
					
					for (; thread < numOfThreads; thread++) {

						// some docs may be missing at the end due to integer division
						if (thread == numOfThreads - 1) {
							docsPerThread = result.size() - offset;
						}
						List <String> subResultList = result.subList(offset, offset + docsPerThread);
						runnables[thread] = new FileReaderRunnable( subResultList, tempfolderPathString,
								mySqlDB, classifier, thread);
						
						offset += docsPerThread;
					
					}// end of for (; thread < numOfThreads; thread++)					
				}else numOfThreads = 1;
				
			}else {
				Vector<String> result = fileList.getList(corpusDir);
				runnables[0] = new FileReaderRunnable( result, tempfolderPathString,
						mySqlDB,classifier, 0);
			}	
					
			
			ExecutorService executor = Executors.newFixedThreadPool(numOfThreads);	
			
			for (int thread = 0; thread < numOfThreads; thread++) {
				
				System.out.println("submitting thread " + thread);
				executor.submit(runnables[thread]);			
			}	
			
			
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				
			}
			
			boolean finished = false;
			while (! finished) {
				
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					
				}
				
				finished = true;
				
				// Are all the threads done?
				for (int thread = 0; thread < numOfThreads; thread++) {
//					System.out.println("thread " + thread + " done? " + runnables[thread].getIsFinished());
					finished = finished && runnables[thread].getIsFinished();
				}
				
			}			
			executor.shutdown();
			
			for (int thread = 0; thread < numOfThreads; thread++) {
				totalCounts += runnables[thread].getCounter();
			}
			executor.awaitTermination(1, TimeUnit.SECONDS);
			System.out.println("All tasks are finished!");
			System.out.println("Converting "+totalCounts+" Files in total!");
  	
        }catch(Exception e){
            System.out.println("Strip failed."); 
            System.err.println(e.getMessage());            
        }
        /////////////////////////////////////////////////////////////////////  
        
    }// end of Convert

    public static void main(String[] args)throws Exception{
    	
    	long startTime = System.currentTimeMillis();
    	
   
    	ConvertDocs convertDocs = new ConvertDocs("test", "root", "mysql", 
    			"tempTxtFiles", "tempSemiconductor", "txt,ppt,pptx,pdf,doc,docx", 2);
    	convertDocs.Convert("/Users/xiaofengzhu/Documents/eclipseWorkspace/temp3");
    	

		long endTime   = System.currentTimeMillis();
		long totalTime = (endTime - startTime);
		System.out.println(totalTime);
		System.out.println(
		String.format("%d min, %d sec", 
			    TimeUnit.MILLISECONDS.toMinutes(totalTime),
			    TimeUnit.MILLISECONDS.toSeconds(totalTime) - 
			    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalTime))
			)
		);
		// end of testing running time    	
    	
    }// end of main

    
}