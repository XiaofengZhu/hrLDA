package com.hrLDA.readdocs;


import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.io.FilenameUtils;

public class FileList {
    private Set<String> fileFilterSet = new HashSet<String>(Arrays.asList(
    	     new String[] {"txt","ppt","pptx","pdf","doc","docx","xls","xlsx"}
    		));
    
    private Set<String> simpleFileFilterSet = new HashSet<String>(Arrays.asList(
   	     new String[] {"txt","doc","docx","xls","xlsx"}
   		));
    private Set<String> complexFileFilterSet = new HashSet<String>(Arrays.asList(
   	     new String[] {"ppt","pptx","pdf"}
   		));

	private Vector<String> simpleFileVector =new Vector<String>(); 	 // simple file vector
    private Vector<String> complexFileVector =new Vector<String>(); 	 // complex file vector
    
    public Vector<String> getSimpleFileVector() {
		return simpleFileVector;
	}


	public Vector<String> getComplexFileVector() {
		return complexFileVector;
	}
	
    public FileList(String fileExtension){
        fileFilterSet = new HashSet<String>(Arrays.asList(
             fileExtension.split(",")
            ));
    }
 
    
    public void getFileList(String dir_name) throws Exception{
    	Vector<String> ver =new Vector<String>();   // directory vector
    	
        ver.add(dir_name);
        while(ver.size()>0){        	
        	// get all files from the path
            File[] files = new File(ver.get(0).toString()).listFiles();
            ver.remove(0);            
            // in case some folders need additional permissions
            if (files != null){	
                int len=files.length;

                for(int i=0;i<len;i++){
                    String tmp=files[i].getAbsolutePath();
                    if(files[i].isDirectory())                    	
                    	// if it'a directory add to the ver                   	
                		ver.add(tmp);
                    else                    	
                    	// it'a file 
                    	// if it's an accepted file, add to the res
                    	if (acceptSimpleFile(tmp)){
                    		this.simpleFileVector.add(tmp);
                    		System.out.println(tmp);
                		}else if (acceptComplexFile(tmp)){
                    		this.complexFileVector.add(tmp);
                    		System.out.println(tmp);
                		}
                }	
            }
        }
    }// end of getFileList()
    
	/**
	 * 
	 * @param dir_name
	 * @return res
	 * @throws Exception
	 */
    public Vector <String> getList(String dir_name) throws Exception{
    	Vector<String> ver =new Vector<String>();   // directory vector
    	Vector<String> res =new Vector<String>(); 	 // file vector
        ver.add(dir_name);
        while(ver.size()>0){        	
        	// get all files from the path
            File[] files = new File(ver.get(0).toString()).listFiles();
            ver.remove(0);            
            // in case some folders need additional permissions
            if (files != null){	
                int len=files.length;

                for(int i=0;i<len;i++){
                    String tmp=files[i].getAbsolutePath();
                    if(files[i].isDirectory())                    	
                    	// if it'a directory add to the ver                   	
                		ver.add(tmp);
                    else                    	
                    	// it'a file 
                    	// if it's an accepted file, add to the res
                    	if (accept(tmp)){
                    		res.add(tmp);
                    		System.out.println(tmp);
                		}
                }	
            }
        }
        return res;
    }// end of getList()
    
    /**
     * 
     * @param tmp
     * @return true extension in FileFilterSet
     */
    public boolean acceptSimpleFile(String filePath){
    	String ext = FilenameUtils.getExtension(filePath);
    	return simpleFileFilterSet.contains(ext);
    }// end of accept()

    public boolean acceptComplexFile(String filePath){
    	String ext = FilenameUtils.getExtension(filePath);
    	return complexFileFilterSet.contains(ext);
    }// end of accept()
    public boolean accept(String filePath){
    	String ext = FilenameUtils.getExtension(filePath);
    	return fileFilterSet.contains(ext);
    }// end of accept()    
    public static void main (String [] args) throws Exception{
    	
    	FileList fileList = new FileList ("txt,ppt,pptx,pdf,doc,docx,xls,xlsx");
    	// Change it to your path
    	fileList.getList("/Users/xiaofengzhu/Documents/Intel/Materials/wikippts/Wiki0");
    	
    }// end of main
    
}