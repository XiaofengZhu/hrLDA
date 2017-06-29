package com.hrLDA.doc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import org.apache.poi.hslf.HSLFSlideShow;
import org.apache.poi.hslf.model.Slide;
import org.apache.poi.hslf.usermodel.RichTextRun;
import org.apache.poi.hslf.usermodel.SlideShow;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;


public class StripPPTContent extends PPTSDocument{

	public StripPPTContent(){
		
	}

	/**   
	 * 
	 * @param .ppt file
	 * @return text content
	 * @throws IOException
	 */
    public StripPPTContent readContent(File file){
    	HashMap <String, String> slidesRelationhashMap = new HashMap <String, String> ();
    	int maxMetaSearchNo = 2;
    	String authors ="";
    	String datePublished ="";
	
        try {
	        FileInputStream is=new FileInputStream(file);
	        StringBuffer stringBuffer = new StringBuffer();
	        // Extract text lines from pages one by one
	        SlideShow ss=new SlideShow(new HSLFSlideShow(is));
	        Slide[] slides=ss.getSlides();
	        
	        for(int i=0; i<slides.length; i++){
	            HashMap<String, String[]> hashLevels = new HashMap<String, String[]>();
	            String lastText = "";	
	            String lastIndentWithBullet = "";
//	            System.out.println(slides[i].getTextRuns().length);
	            for(int j=0;j<slides[i].getTextRuns().length;j++){
	            	RichTextRun[] runs=slides[i].getTextRuns()[j].getRichTextRuns();
	            	for (RichTextRun run : runs){
	            		if (run.getText() != null){ 
	    	            	if (!run.getText().matches("\\s+")){    	            		
	    	            		//text paragraph (text on the same level)
	    	            		
	    	            		String parentText = "";
	    	            		String parentIndentWithBullet ="";
	    	            		String indentWithBullet = run.isBullet()? (""+run.getTextOffset() + run.getBulletChar()):""+run.getTextOffset();
	    	            		for (String srun : run.getText().split("\n")){
	    	            			if (srun.trim().length()>3){
		    	            			// ppt file name, slide content
		    	            			slidesRelationhashMap.put(this.getTxtFileName(), srun);	    	            				
			    	            		if(i < maxMetaSearchNo){
			    	                        // invoke method from Document
			    	            			String [] metaData = this.getMetaData(srun.trim());
			    	                        if (metaData != null){
			    	                        	
			    	                        	authors += metaData[0];
			    	                        	datePublished += metaData[1];
			    	            			}	    	                        
			    	            		}// end of if(i < maxSearchNo){            			                   	            		
	            	            		srun =srun.replaceAll("[^\\p{L}\\p{N}\\s\\,\\.\\;\\?\\-\\'\"\\(\\)]|(^\\p{N}+\\.*\\p{N}*)", "").trim();
	            	            		if (!srun.matches("(\\[[0-9\\.]+\\])|Fig. [0-9\\.]+|Figure [0-9\\.]+") 
	            	            				&& !(srun.matches("\\s+")) && !(srun.matches("[^\\p{L}]+"))){
	            	            			if (!srun.equals(""))
	            	            			stringBuffer.append(srun+".\n");
	            	            			
	            	            			
	                	            		// extract format info
	            	            			if (hashLevels.containsKey(indentWithBullet) && hashLevels.containsKey(lastIndentWithBullet) 
	            	            					&& lastIndentWithBullet.equals(hashLevels.get(indentWithBullet)[2])){
	        		    	            		parentText = lastText;
	        		    	            		parentIndentWithBullet = lastIndentWithBullet;
	        		    	            	}else if (hashLevels.containsKey(indentWithBullet)){
	            	    	            		parentText = hashLevels.get(indentWithBullet)[1];
	            	    	            		parentIndentWithBullet = hashLevels.get(indentWithBullet)[2]; 	            		
	            	    	            	}else{
	            	    	            		parentText = lastText;
	            	    	            		parentIndentWithBullet = lastIndentWithBullet;
	            	    	            	}
	
	                	            		lastIndentWithBullet = indentWithBullet;
	            	    	            	lastText = srun;
	            	    	            	String [] record = {srun, parentText, parentIndentWithBullet};
	            	    	            	slidesRelationhashMap.put(srun, parentText);
	            	    	            	hashLevels.put(indentWithBullet, record);
	            	            		}// end of (!srun.matches("(\\[[0-9\\.]+\\])|Fig. [0-9\\.]+|Figure [0-9\\.]+")){    	            				
	    	            			}// end of if (srn.length()>2)
	    	            		}//end of for (String srun : run.getText().split("\n")){
	    	            	}// end of if (!run.getText().matches("\\s+")){  
	            		}// end of if (run.getText() != null){ 
	            	}// end of for (RichTextRun run : runs){
	            }// end of for(int j=0;j<slides[i].getTextRuns().length;j++){
	        }// end of for(int i=0;i<slides.length;i++){	  
	        
	        String cont = stringBuffer.toString();
            if (cont.matches("")) return null;
            else {
    	        this.setCont(cont);
    	        this.setAuthors(authors);
            	if (!datePublished.equals(""))
            		this.setDatePublished(datePublished);  
    	        this.setSlidesRelationhashMap(slidesRelationhashMap);
            	return this; 
            } 
        }catch (Exception e){
        	return null;
        }
    } 
    	
    public static void main(String[] args) throws Exception{
    	
        AbstractSequenceClassifier<CoreLabel> classifier;
        String  serializedClassifier = "classifiers/english.muc.7class.distsim.crf.ser.gz";
        classifier = CRFClassifier.getClassifier(serializedClassifier);
        
		Document pptContent = new StripPPTContent();
		pptContent.setClassifier(classifier); 
		File file = new File ("R:/Intel/Materials/ECEN5004-Ch9.ppt");
		pptContent.readContent(file);
		if (pptContent.getCont() != null){
			System.out.println(pptContent.getCont());
			System.out.println(pptContent.getDatePublished());	
			System.out.println(pptContent.getAuthors());	
			HashMap RelationhashMap =pptContent.getSlidesRelationhashMap();
			if (RelationhashMap != null){
				System.out.println(RelationhashMap.size());		
			}
		}
    }

}
