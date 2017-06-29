package com.hrLDA.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {
	public TimeUtil(){}

	 
    public static String getCurrentDateTime()  
    {   
    	SimpleDateFormat dateFormat = new SimpleDateFormat("dd-M-yyyy-hh-mm-ss-SSS");
    	Date date = new Date();
    	return "-"+dateFormat.format(date); 
    } 
    
    public static String formatDateTime(long modifiedTime){
        Date date=new Date(modifiedTime);
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:MM:SS");
        return sdf.format(date);     	
    }
}
