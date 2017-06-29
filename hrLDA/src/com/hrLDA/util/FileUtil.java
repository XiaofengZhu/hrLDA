package com.hrLDA.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Pattern;



public class FileUtil {

	public FileUtil(){}
	
    /**
     * delete a folder and its files
     * @param   sPath 
     * @return  return true if success
     */
    public static boolean deleteDirectory(String sPath) {

        if (!sPath.endsWith(File.separator)) {
            sPath = sPath + File.separator;
        }
        File dirFile = new File(sPath);

        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        boolean flag = true;
        //delete all files
        File[] files = dirFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            //delete files
            if (files[i].isFile()) {
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) break;
            } //delete dirs
            else {
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag) break;
            }
        }
        if (!flag) return false;
        //delete current dir
        if (dirFile.delete()) {
            return true;
        } else {
            return false;
        }
    }// end of deleteDirectory()
    
    /**
     * delete single file
     * @param   sPath    
     * @return return true if success
     */
    public static boolean deleteFile(String sPath) {
        boolean flag = false;
        File file = new File(sPath);
        // delete a file
        if (file.isFile() && file.exists()) {
            file.delete();
            flag = true;
        }
        return flag;
    }// end of deleteFile(...)   
 
    
    public static void createTempFolder (String tempfolderPathString){
        if(new File(tempfolderPathString).exists()){
        	deleteDirectory(tempfolderPathString);
        	System.out.println("\nDelete the temp folder!");
        	new File(tempfolderPathString).mkdir();
        	System.out.println("Create a new temp folder!, .."+File.separator+"temp");
        }else{
        	new File(tempfolderPathString).mkdir();
        	System.out.println("Create a temp folder, .."+File.separator+"temp!");
        }
    }
    
    
    public static boolean isFakeFile(String filename){
    	// use to check if current file is fake
        String regex="~\\$\\S+"; 
        return Pattern.matches(regex, filename);
    }
    
    public static boolean checkClassifierFile(){
    	String serializedClassifierPath = "classifiers"+File.separator
    			+"english.muc.7class.distsim.crf.ser.gz";
    	
    	if(!new File(serializedClassifierPath).exists()) return false;
    	return true;
    }
}
