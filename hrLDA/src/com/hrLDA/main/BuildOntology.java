package com.hrLDA.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import cc.hrLDA.topics.HierarchicalRLDA;
import cc.hrLDA.topics.HierarchicalRLDAInferencer;
import cc.mallet.types.InstanceList;
import cc.mallet.util.Randoms;

import com.hrLDA.readdocs.ConvertDocs;
import com.hrLDA.util.TimeUtil;

public class BuildOntology {
	
	private String corpusDir;
	
	private String databaseName;
	private String username;
	private String password;
	
	private String relationTableName = "semiconductor";	
	private String fileTableName = "txtFiles";
	private String tempRelationTableName = "temp";
	
	private String endString ="";
	private String ontologyFileName ="semiconductor";
	
	private boolean mwNgram = false;
	
	private int level = 6;
	private int numOfThreads = 1;
	private int numberOfIterations =2000;	
	
	private boolean converter =true;

	private double alpha = 10.0;
	private double gamma = 1.0;
	private double eta = 0.01;

	private String fileExtension = "txt,ppt,pptx,pdf,doc,docx,xls,xlsx";
	
	String statePath, perplexityPath, ontologyPath;

	public BuildOntology (){
		initilizeParameters();	
		// for initial run
		getTxtCorpus();
	}// end of BuildOntology	
	
	public static void main(String[] args) throws Exception {
		// for testing running time
		long startTime = System.currentTimeMillis();
		
		// build buildOntology, include 1st step into constructor
		BuildOntology buildOntology = new BuildOntology ();
		
		buildOntology.building();
		
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
		
	}//end of main()

	
	private void initilizeParameters(){
		Properties prop = new Properties();
		try{
			
			//get project current path
            String path = System.getProperty("user.dir"); 
            // read settings from configuration file -- parameter.properties
            String propertiesDir =path + File.separator + "parameter.properties";
            
            if (new File(propertiesDir).exists()){
            	
            	//load properties file
                prop.load(new FileInputStream(propertiesDir));	
                
                /** corpusDir, databaseName, databaseUsername and databasePassword are required fields*/                   
                setCorpusDir(prop);
                setDatabase(prop);
                setRelationTableName(prop);
                setFileTableName(prop);
                setTempRelationTableName(prop);
                setEndString(prop);   
                setOntologyFileName(prop); 
                setNumberOfIterations(prop);
                setLevel(prop);
                setMwNgram(prop);
                setAlpha(prop);
                setGamma(prop);
                setEta(prop);
                setFileExtension(prop);
                setNumOfThreads(prop);
            }else{
            	System.err.println("file \"parameter.properties\" not found");        		     
			}// end of loading properties file 
            
        }catch(FileNotFoundException fnfe){
            System.err.println("file \"parameter.properties\" not found");
            fnfe.printStackTrace();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }finally{
        }		
	}// end of initilizeParameters
	

	private void getTxtCorpus(){

		if (!new File("../temp/").exists() || this.converter){
			/**Start from the 1st step ---- Convert documents */
			System.out.println("ConvertDocs");
	    	ConvertDocs convertDocs = new ConvertDocs(
	    			this.databaseName, this.username, this.password, 
	    			this.fileTableName, this.tempRelationTableName, 
	    			this.fileExtension, this.numOfThreads);
	    	try {
				convertDocs.Convert(this.corpusDir);
			} catch (Exception e) {
				System.err.println("Not be able to convet documents from "+this.corpusDir);
			    System.exit(0);	
			}			
		}		
	}// end of getTxtCorpus()
	
	private void building () throws Exception{
		
		/**Start from the 2nd step
		* Define a domain that the ontology is going to work with
        * Begin by importing documents from text to feature sequences
        */
		ImportDirWithRelation importer = new ImportDirWithRelation(
				this.endString, this.databaseName, this.username, this.password, 
				this.relationTableName, this.tempRelationTableName, this.mwNgram,
				this.numOfThreads);
        
        InstanceList instances = importer.readDirectory(new File("../temp/"));
// COMMENT PERPLEXITY EXPERIMENT SETTINGS        
//        for (int level = 4; level <= 10; level = level + 2){
//        	for (int numberOfIteration = 1; numberOfIteration < 50;  numberOfIteration++){
//              HierarchicalLDAInferencer inferencer = runHrLDA(instances, level, numberOfIteration);
//              wrtieOntologyToFile(inferencer);
//        	}
//        	for (int numberOfIteration = 50; numberOfIteration <= 2000;  numberOfIteration = numberOfIteration + 50){
        		for (int times = 1; times <= 1; times++){
	                HierarchicalRLDAInferencer inferencer = runHrLDA(instances, level, numberOfIterations, times);
	                wrtieOntologyToFile(inferencer, level, numberOfIterations,times);
                }
//          	}        	
//        }
 
//        statePath = ".\\state\\"+"hrLDA_"+this.level+"_"+this.numberOfIterations+"\\";
//        perplexityPath = ".\\perplexity\\"+"hrLDA_"+this.level+"_"+this.numberOfIterations+"\\";
//        ontologyPath = ".\\ontology\\"+"hrLDA_"+this.level+"_"+this.numberOfIterations+"\\";
//    	  new File(statePath).mkdirs();
//    	  new File(perplexityPath).mkdirs();
//    	  new File(ontologyPath).mkdirs();        
//        for (int times = 1; times <= 10; times++){
//	        HierarchicalLDAInferencer inferencer = runHrLDA(instances, times);
//	        wrtieOntologyToFile(inferencer);
//        }
//        HierarchicalLDAInferencer inferencer = runHrLDA(instances);
//        wrtieOntologyToFile(inferencer);        
		System.out.println("Done!");		
	}// end of building(...)
	private void wrtieOntologyToFile(HierarchicalRLDAInferencer inferencer, int level, int estimateTime, int times ) 
			throws UnsupportedEncodingException, FileNotFoundException{
    	ontologyPath = ".\\ontology\\"+"hrLDA_"+level+"_"+estimateTime+"\\";
    	if (!new File(ontologyPath).exists()) new File(ontologyPath).mkdirs();	
    	
		OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(
				ontologyPath+ontologyFileName+"_"+this.level+"_"+this.numberOfIterations+"_"+times+".rdf"),"UTF-8");
		System.out.println();      
		try {
			inferencer.printRDFNode(inferencer.getRootNode(), null).write( out, "RDF/XML" );
		} finally {
			// TODO: handle exception
			  if (out != null) {
				    try {out.close();} catch (IOException ignore) {}
				  }
		}		
	}// end of wrtieOntologyToFile (...)
	
	private void wrtieOntologyToFile(HierarchicalRLDAInferencer inferencer) 
			throws UnsupportedEncodingException, FileNotFoundException{
    	ontologyPath = ".\\ontologyResult\\";
    	if (!new File(ontologyPath).exists()) new File(ontologyPath).mkdirs();
		OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(
				ontologyPath+ontologyFileName+this.level+"_"+this.numberOfIterations+"_"
						+TimeUtil.getCurrentDateTime()+".rdf"),"UTF-8");
		System.out.println();      
		try {
			inferencer.printRDFNode(inferencer.getRootNode(), null).write( out, "RDF/XML" );
		} finally {
			// TODO: handle exception
			  if (out != null) {
				    try {out.close();} catch (IOException ignore) {}
				  }
		}		
	}// end of wrtieOntologyToFile (...)
	
	private HierarchicalRLDAInferencer runHrLDA(InstanceList instances) 
			throws FileNotFoundException, IOException{
		
		HierarchicalRLDA model = new HierarchicalRLDA();
        
		//set parameters
		model.setAlpha(this.alpha);//10.0
		model.setGamma(this.gamma);
		model.setEta(this.eta);//0.1
		model.setEnd(this.endString);
		model.setStateFilePath(statePath+"hrLDA_"+this.level+"_"+this.numberOfIterations+".state");
		model.setPerplexityFilePath(perplexityPath+"hrLDA_"+this.level+"_"+this.numberOfIterations+".txt");
		//set level
		model.initialize(instances, instances, this.level, this.numOfThreads, new Randoms(), 
				this.databaseName, this.username, this.password, 
				this.relationTableName, this.tempRelationTableName);
		
//		model.printState();		
		//set numberOfIterations		
		model.estimate(this.numberOfIterations);
		
		System.out.println("total number of phrases: "+model.getNumOfPhrases());
		HierarchicalRLDAInferencer inferencer = new HierarchicalRLDAInferencer(model);
		
		return inferencer;
	}// end of runHrLDA(...)	
	private HierarchicalRLDAInferencer runHrLDA(InstanceList instances, int times)
			throws FileNotFoundException, IOException{
		HierarchicalRLDA model = new HierarchicalRLDA();
        
		//set parameters
		model.setAlpha(this.alpha);//10.0
		model.setGamma(this.gamma);
		model.setEta(this.eta);//0.1
		model.setEnd(this.endString);
		model.setStateFilePath(statePath+"hrLDA_"+this.level+"_"+this.numberOfIterations+"_"+times+".state");
		model.setPerplexityFilePath(perplexityPath+"hrLDA_"+this.level+"_"+this.numberOfIterations+"_"+times+".txt");
		//set level
		model.initialize(instances, instances, this.level, this.numOfThreads, new Randoms(), 
				this.databaseName, this.username, this.password, 
				this.relationTableName, this.tempRelationTableName);
		
//		model.printState();		
		//set estimateTime		
		model.estimate(this.numberOfIterations);
		
		System.out.println("total number of phrases: "+model.getNumOfPhrases());
		HierarchicalRLDAInferencer inferencer = new HierarchicalRLDAInferencer(model);
		
		return inferencer;
	}// end of runHrLDA(...)	
	private HierarchicalRLDAInferencer runHrLDA(InstanceList instances, int level, int estimateTime, int times) 
			throws FileNotFoundException, IOException{
        statePath = ".\\state\\"+"hrLDA_"+level+"_"+estimateTime+"\\";
        perplexityPath = ".\\perplexity\\"+"hrLDA_"+level+"_"+estimateTime+"\\";
        
    	if (!new File(statePath).exists()) new File(statePath).mkdirs();
    	if (!new File(perplexityPath).exists()) new File(perplexityPath).mkdirs();
	
		
		HierarchicalRLDA model = new HierarchicalRLDA();
        
		//set parameters
		model.setAlpha(this.alpha);//10.0
		model.setGamma(this.gamma);
		model.setEta(this.eta);//0.1
		model.setEnd(this.endString);
		model.setStateFilePath(statePath+"hrLDA_"+level+"_"+estimateTime+"_"+times+".state");
		model.setPerplexityFilePath(perplexityPath+"hrLDA_"+level+"_"+estimateTime+"_"+times+".txt");
		//set level
		model.initialize(instances, instances, level, numOfThreads, new Randoms(), 
				databaseName, username, password, relationTableName, tempRelationTableName);
		
//		model.printState();		
		//set estimateTime		
		model.estimate(estimateTime);
		
		System.out.println("total number of phrases: "+model.getNumOfPhrases());
		HierarchicalRLDAInferencer inferencer = new HierarchicalRLDAInferencer(model);
		
		return inferencer;
	}// end of runHrLDA(...)		
	
	// set parameters
	private void setCorpusDir(Properties prop) {
        if (prop.getProperty("corpusDir")!=null)
        	//--read corpus dir
        	this.corpusDir = prop.getProperty("corpusDir").trim(); 
        
		/**converter = 0 means the user tempts to start the program from the 2nd step directly*/
		if (this.corpusDir.equals("0")) setConverter(false);
		else if(!new File(this.corpusDir).exists()){
			System.err.println("Corpus dir is incorrect");
		    System.exit(0);	
		}         
	}// end of setCorpusDir(...)
	
	private void setDatabase(Properties prop) {
        if (prop.getProperty("databaseName")!=null 
        		&& prop.getProperty("databaseUserName")!=null
        		&& prop.getProperty("databasePassword")!=null){
        	//--read databaseName
        	this.databaseName = prop.getProperty("databaseName").trim();
        	//--read database username
        	this.username = prop.getProperty("databaseUserName").trim();
        	//--read database password
        	this.password = prop.getProperty("databasePassword").trim();                  	
        }else{                	
			System.err.println("MySQL DB info is required!");
		    System.exit(0);    			    
        } 
	}// end of setDatabase(...)
 
	private void setRelationTableName(Properties prop) {
	    if (prop.getProperty("relationTableName")!=null)
	    	//--set relationTableName
	    	this.relationTableName = prop.getProperty("relationTableName").trim();   
	}// end of setRelationTableName(...)

	private void setFileTableName(Properties prop) {
	    if (prop.getProperty("fileTableName")!=null)
	    	//--set fileTableName(...)
	    	this.fileTableName = prop.getProperty("fileTableName").trim();  
	}// end of setFileTableName(...)

	private void setTempRelationTableName(Properties prop) {
	    if (prop.getProperty("tempRelationTableName")!=null)
	    	//--set relationTableName
	    	this.tempRelationTableName = prop.getProperty("tempRelationTableName").trim();  
	}// end of setTempRelationTableName(...)

	private void setEndString(Properties prop) {
	    if (prop.getProperty("endString")!=null)
	    	//--set endString
	    	this.endString = prop.getProperty("endString").trim();    
	}// end of setEndString(...)

	private void setOntologyFileName(Properties prop) {
	    if (prop.getProperty("ontologyFileName")!=null)
	    	//--set ontologyFileName
	    	this.ontologyFileName = prop.getProperty("ontologyFileName").trim();  
	}// end of setOntologyFileName(...)

	private void setMwNgram(Properties prop) {
	    if (prop.getProperty("mwNgram")!=null)
	    	//--set Ngram usage
	    	this.mwNgram = Boolean.valueOf(prop.getProperty("mwNgram").trim());
	}// end of setMwNgram(...)

	public void setLevel(Properties prop) {
	    if (prop.getProperty("level")!=null)
	    	//--set level
	    	this.level = Integer.parseInt(prop.getProperty("level"));    
	}// end of setLevel(...)
	
	public void setNumOfThreads(Properties prop) {
	    if (prop.getProperty("level")!=null)
	    	//--set level
	    	this.numOfThreads = Integer.parseInt(prop.getProperty("numOfThreads"));    
	}// end of setNumOfThreads(...)
	
	private void setNumberOfIterations(Properties prop) {
	    if (prop.getProperty("numberOfIterations")!=null)
	    	//--set estimateTime
	    	this.numberOfIterations = Integer.parseInt(prop.getProperty("numberOfIterations"));  
	}// end of setEstimateTime(...)

	public void setConverter(boolean converter) {
		this.converter = converter;
	}// end of setConverter(...)

	private void setAlpha(Properties prop) {
	    if (prop.getProperty("alpha")!=null)
	    	//--set alpha
	    	this.alpha = Double.parseDouble(prop.getProperty("alpha")); 		
	}// end of setAlpha(...)

	private void setGamma(Properties prop) {
	    if (prop.getProperty("gamma")!=null)
	    	//--set gamma
	    	this.gamma = Double.parseDouble(prop.getProperty("gamma")); 		
	}// end of setGamma
	
	private void setEta(Properties prop) {
	    if (prop.getProperty("eta")!=null)
	    	//--set eta
	    	this.alpha = Double.parseDouble(prop.getProperty("eta")); 		
	}// end of setEta	
	private void setFileExtension(Properties prop){
		if (prop.getProperty("fileExtension")!=null){
			String tempFileExtension = prop.getProperty("fileExtension").replaceAll(" ", "");
			if (checkFileExtension(tempFileExtension)){
				this.fileExtension = tempFileExtension;
			}else{                	
			System.err.println("error setting fileExtension");
		    System.exit(0);    			    
        	} 
		}

	}// end of setFileExtension
	// end of set parameters
	private boolean checkFileExtension(String tempFileExtension){
	    Set<String> FileFilterSet = new HashSet<String>(Arrays.asList(
	     new String[] {"txt","ppt","pptx","pdf","doc","docx","xls","xlsx"}
		));
		String [] fileExtensions= tempFileExtension.split(",");
		for (String fileExtension : fileExtensions){
			if (! FileFilterSet.contains(fileExtension))
				return false;
		}
		return true;
	}
	
}