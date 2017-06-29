package com.hrLDA.doc;


import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;

public class StripPDFContent extends FTXDocument{
	public StripPDFContent(){}
	
    public StripPDFContent readContent(File file){

    	boolean sort=false;
        
        String authors = "";
        String datePublished = "";
        try{
        	PDDocument pdfdocument=null;
            pdfdocument=PDDocument.load(file);
            
            // the first few pages are likely to contain metaData
            String firstPages = getFirstPages(pdfdocument, sort);

            // invoke method from Document
			String [] metaData = this.getMetaData(firstPages);
            if (metaData != null){
            	
            	authors += metaData[0];
            	datePublished += metaData[1];
			}
            
            
            PDFTextStripper stripper=new PDFTextStripper();
            stripper.setSortByPosition(sort);

            String cont = stripper.getText(pdfdocument);
            cont = cont.replaceAll("\\s+|\"", " ");     
            //Remove irrelevant text
            //For papers
            String regexREF="REFERENCES.*|ACKNOWLEDGMENT.*"; 
            Pattern patternREF = Pattern.compile(regexREF,Pattern.CASE_INSENSITIVE);
            Matcher mREF = patternREF.matcher(cont);
            if (mREF.find()) {  cont=cont.substring(0, mREF.start());}
            
            if (cont.matches("")) {
            	pdfdocument.close();
            	return null;
            }else {
            	this.setCont(cont);
            	this.setAuthors(authors);
            	if (!datePublished.equals(""))
            		this.setDatePublished(datePublished); 
            	pdfdocument.close();
            	return this; 
            }
                       
        }catch(Exception e){
            return null;
        }finally{

        }
    }// end of getText(...)
    
    public String getFirstPages(PDDocument pdfdocument, boolean sort){
        int startPage=1;
        int endPage=2;    	

        String regexAbstract ="(.*)(Abstract[—-]?)|(ABSTRACT:?)";
        Pattern patternAbstract = Pattern.compile(regexAbstract, Pattern.MULTILINE);
        Matcher mAbstract;
        try{
	        PDFTextStripper stripper=new PDFTextStripper();
	        stripper.setSortByPosition(sort);
	        stripper.setStartPage(startPage);
	        stripper.setEndPage(endPage);
	        String tempAbstract= stripper.getText(pdfdocument); 

	        if (tempAbstract != null){
	            if (!tempAbstract.equals("")){
	                mAbstract = patternAbstract.matcher(tempAbstract);
	                
	                if (mAbstract.find()) {  
	
	                	if (mAbstract.group(1)!=null){
	                    	if (!mAbstract.group(1).equalsIgnoreCase("")){
	        					return mAbstract.group(1);// find abstract text
	        				}
	                	}
	            	}// end of if (mAbstract.find())  
	            }// end of if (!tempAbstrct.equals("")){        	
	        }// end of if (tempAbstrct != null){
        
	        return "";
        }catch(Exception e){
            return "";
        }finally{

        }// end of finally 
        
    }// end of getFirstPages
    
    public static void main(String[] args) throws Exception{
    	
        AbstractSequenceClassifier<CoreLabel> classifier;
        String  serializedClassifier = "classifiers/english.muc.7class.distsim.crf.ser.gz";
        classifier = CRFClassifier.getClassifier(serializedClassifier);    	
    	Document stripPDFContent = new StripPDFContent();
    	stripPDFContent.setClassifier(classifier); 
        File file=new File("/Users/xiaofengzhu/Documents/Intel/Materials/Bevan.pdf");
        stripPDFContent.readContent(file);
        if (stripPDFContent.getCont() != null){
	        System.out.println(stripPDFContent.getCont());
	        System.out.println(stripPDFContent.getDatePublished());
        }
    }
}