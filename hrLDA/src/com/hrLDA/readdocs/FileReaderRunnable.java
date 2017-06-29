package com.hrLDA.readdocs;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
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
import edu.stanford.nlp.ling.CoreLabel;

public class FileReaderRunnable implements Runnable{

	private  AbstractSequenceClassifier<CoreLabel> classifier;
	
	private boolean isFinished = false;
	
	private String tempfolderPathString;
	
	private  MySqlDB mySqlDB;
	
	private Document documentContent;
	private List<String> subResultList;
	private  int counter =0;
	private  int threadID;

	public int getCounter() {
		return this.counter;
	}
    public boolean getIsFinished (){
    	return this.isFinished;
    }
    
	public FileReaderRunnable(List<String> subResultList, 
			String tempfolderPathString, MySqlDB mySqlDB,
			AbstractSequenceClassifier<CoreLabel> classifier,
    		int threadID){
    	this.subResultList = subResultList;
    	this.tempfolderPathString = tempfolderPathString;
    	this.mySqlDB = mySqlDB;
    	this.threadID = threadID;
    	this.classifier = classifier;
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
        		
        					
        	for (String full_path :subResultList){        		        		
        		readfile = new File(full_path); 
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
					System.out.println("thread ID = "+threadID);
	            	System.out.println("originalFilePath = "+full_path);
	            	
					switch(ext.toLowerCase()){
					case "pdf":
						this.documentContent = new StripPDFContent();
						this.documentContent.setClassifier(classifier); 
						this.documentContent.readContent(readfile);						
						break;
					case "ppt":	
						documentContent = new StripPPTContent();
						documentContent.setClassifier(classifier); 
						filename = ext + "-" + filename.replaceAll(" ","_") 
		            			+ TimeUtil.getCurrentDateTime()+".txt";
						// set up txtFilename for ppt documents
						documentContent.setTxtFileName(filename);						
						documentContent.readContent(readfile);
						if (documentContent.getCont() != null){
							mySqlDB.insertDataToRelationTable(documentContent);
						}
						break;
					case "pptx":
						documentContent = new StripPPTXContent();
						documentContent.setClassifier(classifier); 
						filename = ext + "-" + filename.replaceAll(" ","_") 
		            			+ TimeUtil.getCurrentDateTime()+".txt";
						// set up txtFilename for pptx documents
						documentContent.setTxtFileName(filename);						
						documentContent.readContent(full_path);
						if (documentContent.getCont() != null){					
							mySqlDB.insertDataToRelationTable(documentContent);
						}
						break;
					case "doc":
						this.documentContent = new StripDOCContent();
						this.documentContent.readContent(readfile);	
						break;
					case "docx":
						this.documentContent = new StripDOCXContent();
						this.documentContent.readContent(full_path);	
						break;
					case "xls":
						this.documentContent = new StripXLSContent();
						this.documentContent.readContent(readfile);	
						break;
					case "xlsx":
						this.documentContent = new StripXLSXContent();
						this.documentContent.readContent(full_path);	
						break;
					case "txt":
						this.documentContent = new StripTXTContent();
						this.documentContent.readContent(readfile);	
						break;
					default:
						marker = false;
						break;
							
					}
				
				}// end of if (FileUtil.isFakeFile(filename)){		
				      
				
	            if (marker && this.documentContent != null){
	            	if (this.documentContent.getCont() != null){
		            	String cont = this.documentContent.getCont();
		            	if (!cont.matches("\\s+")){
		            		if (!ext.toLowerCase().contains("ppt")){
		            			// set up txtFilename for none ppt(x) documents
				            	filename = ext + "_" + filename.replaceAll(" ","_")+ 
				            			 "_" + this.threadID+TimeUtil.getCurrentDateTime()+".txt";	
				            	this.documentContent.setTxtFileName(filename);
		            		}
				            // set up originalFilePath
				            documentContent.setOriginalFilePath(full_path);	

				            // get dateModified from File readfile
							long modifiedTime = readfile.lastModified();
							String dateModified = TimeUtil.formatDateTime(modifiedTime); 		            
				            documentContent.setDateModified(dateModified);
				            
		            		// get created date from full_path
		            		if (documentContent.getDatePublished().equals("")){
			            		Path file = Paths.get(full_path);
			            		BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
			            		long CreatedTime = attrs.creationTime().toMillis();
			            		//String dateCreated = TimeUtil.formatDateTime(attrs.creationTime().toMillis());
			            		documentContent.setDatePublished(CreatedTime+"");		            			
		            		}
				            
				            // output	detail info		            
			            	System.out.println("txtName = " + filename); 
			            	System.out.println("ext = " + ext);						
							System.out.println("datePublished: "+documentContent.getDatePublished());
							System.out.println("dateModified: "+documentContent.getDateModified());
							System.out.println("authors: "+ documentContent.getAuthors());
							System.out.println("------------------------------------------------");
				            
				            cont = this.removeIrrelevantText(cont);
				            
			            	//write cont to a .txt file
				            fw = new FileWriter(new File(tempfolderPathString+filename));			            
				            fw.write(cont+"\n");
				            fw.flush();	
				            
				            this.counter++;		            		
		            	}	
	            	}// end of if (this.documentContent.getCont() != null){

		            
	            }// end of if (marker && !cont.equals("\\s")){		            

	            marker = true;
                   		
        	}// end of for (String full_path :result){  
        	
        	fw.close();
        	System.out.println("Converting "+this.counter+" Files in this thread ---> Done!");
        	System.out.println(); 

        	
        }catch(Exception e){
            System.out.println("Strip failed."); 
            System.err.println(e.getMessage());            
        }
        this.isFinished = true;		
		
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
