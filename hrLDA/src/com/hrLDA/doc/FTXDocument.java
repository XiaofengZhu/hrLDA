package com.hrLDA.doc;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ling.CoreLabel;

public class FTXDocument extends Document {

	public FTXDocument() {

	}
    public FTXDocument readContent(File file){
    	return null;
    }
    public FTXDocument readContent(String filePath){
    	return null;
    }
	
	public HashMap<String, String> getSlidesRelationhashMap() {
		return null;
	}    
    public String getFirstPages(PDDocument pdfdocument, boolean sort){
    	return null;
    } 
	
	protected  String regexDatePerson ="<DATE>(.*?)</DATE>|<PERSON>(.*?)</PERSON>";
    protected  AbstractSequenceClassifier<CoreLabel> classifier;	
    
	
	public AbstractSequenceClassifier<CoreLabel> getClassifier() {
		return this.classifier;
	}
	public void setClassifier(AbstractSequenceClassifier<CoreLabel> classifier) {
		this.classifier = classifier;
	}
	public String [] getMetaData (String firstPages){				
		
		if (!firstPages.matches("\\s+")){
			String [] result = new String [2];
	    	HashSet <String> mDatePersonSet = new HashSet <String>();
	    	String datePublished ="";
	    	String authors ="";  		
			String metaF = this.classifier.classifyWithInlineXML(firstPages);
			Pattern patternDatePerson = Pattern.compile(this.regexDatePerson); 		                
			Matcher mDatePerson = patternDatePerson.matcher(metaF);

	        while(mDatePerson.find()){
	        	
	    			if(mDatePerson.group(1)!=null){
	    				if (!mDatePerson.group(1).equals("")){
	    					metaF = metaF.replace("<DATE>"+mDatePerson.group(1)+"</DATE>", "");
	    					if (!mDatePersonSet.contains(mDatePerson.group(1)))
	    					{
	    						datePublished += mDatePerson.group(1)+", ";
	    						mDatePersonSet.add(datePublished);
	    					}
	    					
	    				}// end of if (!mDatePerson.group(1).equals("")){                		
	    			}// end of if(mDatePerson.group(1)!=null){
	    			if(mDatePerson.group(2)!=null){
	    				if (!mDatePerson.group(2).equals("")){
	    					metaF = metaF.replace("<PERSON>"+mDatePerson.group(2)+"</PERSON>", "");
	    					if (!mDatePersonSet.contains(mDatePerson.group(2)))
	    					{
	    						authors += mDatePerson.group(2)+", ";
	    						mDatePersonSet.add(authors);
	    						
	    					}
	    					
	    				}// end of if (!mDatePerson.group(2).equals("")){
	    			}// end of if(mDatePerson.group(2)!=null){	 
	    		mDatePerson = patternDatePerson.matcher(metaF);
	    		
	        }// end of while(mDatePerson.find()){
	        
			datePublished = (datePublished.equals(""))? "":
				(datePublished.substring(0, datePublished.length()-2));
			authors = (authors.equals(""))? "":
				(authors.substring(0, authors.length()-2));	        
	        result[0] = authors;
	        result[1] = datePublished;        
					
	        return result;   
		}// end of if (!firstPages.equals("")){
		return null; 		
		
	}// end of getMetaData ()    


}
