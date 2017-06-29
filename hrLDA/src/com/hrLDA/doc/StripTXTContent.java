package com.hrLDA.doc;

import java.io.File;

import org.apache.commons.io.FileUtils;


public class StripTXTContent extends Document{
	public StripTXTContent(){}
    /**
     * 
     * @param .txt file
     * @return text content
     * @throws Exception
     */
    public StripTXTContent readContent(File file) {
//    	StripTXTContent stripTXTContent = new StripTXTContent();
    	try{
    		String temp = FileUtils.readFileToString(file);
    		temp = temp.replaceAll("\\[.*\\]","");// This is for Wiki articles 
            if (!temp.matches("\\s+") && !temp.matches("")){
                setCont(temp);
                return this;
            }else return null;

		} catch (Exception e) {
    			return null;
		}
   }	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Document stripTXTContent = new StripTXTContent();
		File file = new File ("/Users/xiaofengzhu/Documents/Intel/Materials/wikippts/Wiki0/Chip.txt");
		stripTXTContent.readContent(file);
		System.out.println(stripTXTContent.getCont());
	}

}
