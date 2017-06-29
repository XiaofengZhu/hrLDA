package com.hrLDA.doc;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class StripXLSXContent extends Document{
	public StripXLSXContent(){}
	
  
    
    /**
     * 
     * @param .xlsx filepath
     * @return text content
     * @throws Exception
     */
    public StripXLSXContent readContent(String filepath){    	
    	
    	try{
    		StringBuffer stringBuffer = new StringBuffer();
            @SuppressWarnings("deprecation")
			XSSFWorkbook xwb = new XSSFWorkbook(filepath);

            XSSFSheet sheet = xwb.getSheetAt(0);
            // define row, cell
            XSSFRow row;
            String cell;
           
            for (int i = sheet.getFirstRowNum(); i < sheet.getLastRowNum(); i++) {
                row = sheet.getRow(i);
                for (int j = row.getFirstCellNum(); j < row.getLastCellNum(); j++) {
                	if (row.getCell(j) != null){
                		cell = row.getCell(j).toString();
                		// append the text when it contains English letters
                		if (cell.matches("[\\p{L}_\\s]+") && !cell.matches("\\s+")){
                            stringBuffer.append(cell+"\n");               			
                		}
                        	
                	}
                }
            }
	        
	        if (!stringBuffer.toString().matches("")){
	            this.setCont(stringBuffer.toString());
	            return this;
	        }else return null;

	    	} catch (Exception e) {
				return null;
		}
    } 
	public static void main(String[] args) {
		StripXLSXContent stripXLSXContent = new StripXLSXContent();
		stripXLSXContent.readContent("/Users/xiaofengzhu/Documents/Intel/Materials/wikippts/Wiki0/testXlsx.xlsx");
		System.out.println(stripXLSXContent.getCont());

	}

}
