package com.hrLDA.doc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Section;

public class StripDOCContent extends Document{
	public StripDOCContent() {}
    /**
     * 
     * @param .doc file
     * @return text content
     * @throws IOException
     */
    public StripDOCContent readContent(File file) { 
    	
    	try{
    		
    		StringBuffer stringBuffer = new StringBuffer();
    		FileInputStream is=new FileInputStream(file);
            HWPFDocument doc=new HWPFDocument(is);
            Range r=doc.getRange();
            for(int x=0;x<r.numSections();x++){
                Section s=r.getSection(x);
                for(int y=0;y<s.numParagraphs();y++){
                    Paragraph p=s.getParagraph(y);
                    for(int z=0;z<p.numCharacterRuns();z++){
                        CharacterRun run=p.getCharacterRun(z);
                        if (run != null){
                        	// if this is a text run
                        	if (run.text() != null){
                            	// append it, if it contains text
                        		String tempText = run.text().replaceAll("HYPERLINK \"http:\\/\\/[^\"]+", "")
                        				. replaceAll("\" \\\\o \"", "");
                                if (!tempText.matches("[\\s]+")){
                                	stringBuffer.append(tempText+"\n");
                                }
                        	}
                        }
                    }// end of for(int z=0;z<p.numCharacterRuns();z++){
                }// end of for(int y=0;y<s.numParagraphs();y++){
            }// end of for(int x=0;x<r.numSections();x++){    		
 
        if (!stringBuffer.toString().matches("")){
            this.setCont(stringBuffer.toString());
            return this;
        }else return null;

		} catch (Exception e) {
			return null;
		}    		    	
    }
    
	public static void main(String[] args) {
		Document stripDOCContent = new StripDOCContent();
		File file = new File ("/Users/xiaofengzhu/Documents/Intel/Materials/wikippts/Wiki0/test.doc");
		stripDOCContent.readContent(file);
		System.out.println(stripDOCContent.getCont());

	}

}
