package com.hrLDA.doc;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;


public class StripDOCXContent extends Document{
	public StripDOCXContent(){}
	

    /**
    * 
    * @param filepath
    * @return text content
    * @throws Exception
    */
    public StripDOCXContent readContent(String filepath){
    	
    	try{
    		  StringBuffer stringBuffer = new StringBuffer();
			  InputStream is = new FileInputStream(filepath);  
    	      XWPFDocument doc = new XWPFDocument(is);  
    	      List<XWPFParagraph> paras = doc.getParagraphs(); 

    	      for (XWPFParagraph para : paras) {  
    	    	   List<XWPFRun> runs = para.getRuns();
    	    	   for(int i = 0; i< runs.size() ; i++) {
    	    		   XWPFRun run = runs.get(i);
                       if (run != null){  
                    	   // if this is a text run
                    	   if (run.getText(0) !=null){
                           	// append it, if it contains text
                       			String tempText = run.getText(0).replaceAll("HYPERLINK \"http:\\/\\/[^\"]+", "")
                       				. replaceAll("\" \\\\o \"", "");
                       			if (!tempText.matches("\\s+")){
                       				stringBuffer.append(tempText+"\n");
                       			}   
                    	   }
                       }
    	    	   }// end of  for(int i = 0; i< runs.size() ; i++) {
    	      }// end of for (XWPFParagraph para : paras) {      		
    	      
	        if (!stringBuffer.toString().matches("")){
	            this.setCont(stringBuffer.toString());
	            return this;
	        }else return null;

		} catch (Exception e) {
    			return null;
		}  
   }
    

	public static void main(String[] args) {
		Document stripDOCXContent = new StripDOCXContent();
		stripDOCXContent.readContent("/Users/xiaofengzhu/Documents/Intel/Materials/wikippts/Wiki0/TESTV1.1_simple_cleaned.docx");
		if (stripDOCXContent.getCont() != null){
			System.out.println(stripDOCXContent.getCont());
		}

	}
	

}
