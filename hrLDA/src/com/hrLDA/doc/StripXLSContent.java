package com.hrLDA.doc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;


public class StripXLSContent extends Document{
	public StripXLSContent(){}

    
    /**
     * 
     * @param .xls file
     * @return text content
     * @throws IOException
     */     
    public StripXLSContent readContent(File file){
    	
    	try{
    			StringBuffer stringBuffer = new StringBuffer();
    			FileInputStream is=new FileInputStream(file);
    	        HSSFWorkbook wbHssfWorkbook=new HSSFWorkbook(new POIFSFileSystem(is));
    	        //open excel
    	        HSSFSheet sheet=wbHssfWorkbook.getSheetAt(0);

    	        HSSFRow row=null;
    	        String cell;
    	        
    	        // fetch data from each row
    	        for (int i = 0; i <=sheet.getLastRowNum(); i++) {
    	            row =sheet.getRow(i);
    	            // fetch data from each coloum
    	            for (int j = 0; j <= row.getLastCellNum(); j++) {
    	            	if (row.getCell(j) != null){
                    		cell = row.getCell(j).toString();
                    		// append the text when it contains English letters
                    		if (cell.matches("[\\p{L}_\\s]+") && !cell.matches("\\s+")){
                                stringBuffer.append(cell+"\n");               			
                    		}
    	            	}

    	            }// end of for (int j = 0; j <= row.getLastCellNum(); j++) {
    	        }// end of for (int i = 0; i <=sheet.getLastRowNum(); i++) {

        if (!stringBuffer.toString().matches("")){
            this.setCont(stringBuffer.toString());
            return this;
        }else return null;

    	} catch (Exception e) {
			return null;
	}
    
} 	
    
	public static void main(String[] args) {
		Document stripXLSContent = new StripXLSContent();
		File file = new File ("/Users/xiaofengzhu/Documents/Intel/Materials/wikippts/Wiki0/testXls.xls");
		stripXLSContent.readContent(file);
		System.out.println(stripXLSContent.getCont());

	}

}
