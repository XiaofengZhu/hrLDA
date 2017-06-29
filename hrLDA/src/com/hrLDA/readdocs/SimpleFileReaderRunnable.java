package com.hrLDA.readdocs;

import java.io.File;
import java.io.FileWriter;
import java.text.Normalizer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

import com.hrLDA.doc.Document;
import com.hrLDA.doc.StripDOCContent;
import com.hrLDA.doc.StripDOCXContent;
import com.hrLDA.doc.StripPDFContent;
import com.hrLDA.doc.StripPPTContent;
import com.hrLDA.doc.StripPPTXContent;
import com.hrLDA.doc.StripTXTContent;
import com.hrLDA.doc.StripXLSContent;
import com.hrLDA.doc.StripXLSXContent;
import com.hrLDA.mysql.MySqlDB;
import com.hrLDA.util.FileUtil;
import com.hrLDA.util.TimeUtil;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;

public class SimpleFileReaderRunnable implements Runnable {

	protected boolean isFinished = false;
	
	protected String tempfolderPathString;
	
	protected  MySqlDB mySqlDB;
	
	protected Document documentContent;
    protected List<String> subResultList;
    protected  int counter =0;
    protected  int threadID;
    public int getCounter() {
		return counter;
	}
    public boolean getIsFinished (){
    	return this.isFinished;
    }
	public SimpleFileReaderRunnable(List<String> subResultList, 
			String tempfolderPathString, MySqlDB mySqlDB,
    		int threadID){
    	this.subResultList = subResultList;
    	this.tempfolderPathString = tempfolderPathString;
    	this.mySqlDB = mySqlDB;
    	this.threadID = threadID;
    }
	@Override
	public void run() {
    	 
        // current file
    	File readfile =null;
    	// write cont to tempfolderPathString
    	FileWriter fw =null;  	       
                
        String full_filename, filename, ext;
        boolean marker = true;
        
		
        try{
        	System.out.println("Processing " +  subResultList.size() + " files ...");
        					
        	for (String vector :subResultList){        		        		
        		readfile = new File(vector); 
        		// full file name with path
				full_filename=readfile.getName();
				// file name without extension
				filename=FilenameUtils.getBaseName(full_filename);				  
				ext =FilenameUtils.getExtension(full_filename);				
				
                 
				if (FileUtil.isFakeFile(filename)){				
					marker = false;//Ingore temp files	
				}else {
					// output originalFilePath					
					System.out.println("------------------------------------------------");
	            	System.out.println("originalFilePath = "+vector);
	            	
					switch(ext.toLowerCase()){
					case "pdf":
						documentContent = new StripPDFContent();
						documentContent.readContent(readfile);						
						break;
					case "ppt":	
						documentContent = new StripPPTContent(); 
						documentContent.readContent(readfile);
						if (documentContent.getCont() != null){
							filename = ext + "-" + filename.replaceAll(" ","_") 
			            			+ TimeUtil.getCurrentDateTime()+".txt";
							// set up txtFilename for ppt documents
							documentContent.setTxtFileName(filename);
							mySqlDB.insertDataToRelationTable(documentContent);
						}
						break;
					case "pptx":
						documentContent = new StripPPTXContent();
						documentContent.readContent(vector);
						if (documentContent.getCont() != null){
							filename = ext + "-" + filename.replaceAll(" ","_") 
			            			+ TimeUtil.getCurrentDateTime()+".txt";
							// set up txtFilename for pptx documents
							documentContent.setTxtFileName(filename);						
							mySqlDB.insertDataToRelationTable(documentContent);
						}
						break;
					case "doc":
						documentContent = new StripDOCContent();
						documentContent.readContent(readfile);	
						break;
					case "docx":
						documentContent = new StripDOCXContent();
						documentContent.readContent(vector);	
						break;
					case "xls":
						documentContent = new StripXLSContent();
						documentContent.readContent(readfile);	
						break;
					case "xlsx":
						documentContent = new StripXLSXContent();
						documentContent.readContent(vector);	
						break;
					case "txt":
						documentContent = new StripTXTContent();
						documentContent.readContent(readfile);	
						break;
					default:
						marker = false;
						break;
							
					}
				
				}// end of if (FileUtil.isFakeFile(filename)){		
				      
				
	            if (marker && documentContent != null){
	            	if (documentContent.getCont() != null){
		            	String cont = documentContent.getCont();
		            	if (!cont.matches("\\s+")){
		            		if (!ext.toLowerCase().contains("ppt")){
		            			// set up txtFilename for none ppt(x) documents
				            	filename = ext + "_" + filename.replaceAll(" ","_")+ 
				            			this.threadID+ "_" + TimeUtil.getCurrentDateTime()+".txt";	
				            	documentContent.setTxtFileName(filename);
		            		}

				            // get dateModified from File readfile
							long modifiedTime = readfile.lastModified();
							String dateModified = TimeUtil.formatDateTime(modifiedTime); 
							
				            // set up originalFilePath, dateModified
				            documentContent.setOriginalFilePath(vector);			            
				            documentContent.setDateModified(dateModified);
				            
				            // output	detail info		            
			            	System.out.println("name = " + filename); 
			            	System.out.println("ext = " + ext);						
							System.out.println("datePublished: "+documentContent.getDatePublished());
							System.out.println("dateModified: "+dateModified);
							System.out.println("authors: "+ documentContent.getAuthors());
							System.out.println("------------------------------------------------");
							
							// insert file info to txtFiles(default)
				            mySqlDB.insertData(documentContent);
				            
				            cont = removeIrrelevantText(cont);
				            
			            	//write cont to a .txt file
				            fw = new FileWriter(new File(tempfolderPathString+filename));			            
				            fw.write(cont+"\n");
				            fw.flush();	
				            
				            this.counter++;		            		
		            	}	
	            	}// end of if (documentContent.getCont() != null){

		            
	            }// end of if (marker && !cont.equals("\\s")){		            

	            marker = true;
                   		
        	}// end of for (String vector :result){  
        	
        	fw.close();
        	System.out.println("Converting "+this.counter+" Files in this thread ---> Done!");
        	System.out.println(); 

        	
        }catch(Exception e){
            System.out.println("Strip failed."); 
            System.err.println(e.getMessage());            
        }
        isFinished = true;		
		
	}

	
	
	public String removeIrrelevantText(String cont){
        // remove Fig 1.2 sytle text
        String figRegex ="(\\[[0-9\\.]+\\])|Fig. [0-9\\.]+|Figure [0-9\\.]+";
        Pattern figPattern4 = Pattern.compile(figRegex, Pattern.CASE_INSENSITIVE);        
        Matcher figMatcher = figPattern4.matcher(cont);
        
        if (figMatcher.find()) {   cont = cont.replaceAll(figRegex, "");  }		            
		// Remove non-ASCII characters
        cont = (Normalizer.normalize(cont, Normalizer.Form.NFD)).replaceAll("[^\\x00-\\x7F]", "");
        cont = cont.replaceAll("&apos;", "'").replaceAll("&quot;", "\"").replaceAll("&gt;", "<".replaceAll("&lt;", ">"));
		// Remove all non letter/number characters except dash and apostrophe
		cont = cont.replaceAll("[^\\p{L}\\p{N}\\s\\,\\.\\;\\?\\-\\'\"\\(\\)]", " ");
		// Reduce all blocks of whitespace to a single space        
        return cont;
	}// end of removeIrrelevantText(...)
	
}// end of FileReaderRunnable
