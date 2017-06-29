package com.hrLDA.doc;

import java.io.FileInputStream;
import java.util.HashMap;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreLabel;

public class StripPPTXContent extends PPTSDocument{


	public StripPPTXContent(){

	}
	
    /**
     * 
     * @param .pptx file
     * @return text content
     * @throws Exception
     */
	public StripPPTXContent readContent(String fileName) {
		HashMap <String, String> slidesRelationhashMap = new HashMap <String, String> ();
    	int maxMetaSearchNo = 2;
    	String datePublished ="";
    	String authors ="";
        try {
        	FileInputStream is=new FileInputStream(fileName);
	        StringBuffer stringBuffer = new StringBuffer();
	        XMLSlideShow pptx = new XMLSlideShow(is);
	        //get slides 
	        XSLFSlide[] slides = pptx.getSlides();	 	       
            
	        for(int i=0;i<slides.length;i++){
	            HashMap<String, String[]> hashLevels = new HashMap<String, String[]>();
	            String lastIndentWithBullet = "";
	            String lastText = "";

	            XSLFShape[] sh = slides[i].getShapes();

    	        for (int j = 0; j < sh.length; j++){
	
	    	          if (sh[j] instanceof XSLFTextShape){
		    	            XSLFTextShape shape = (XSLFTextShape)sh[j];
		    	            //work with a shape that can hold text	

		    	            XSLFTextShape txShape = (XSLFTextShape) shape;
		    	            for (XSLFTextParagraph xslfParagraph : txShape.getTextParagraphs()){
		    	            	if (xslfParagraph.getText() != null){
			    	            	if (!xslfParagraph.getText().matches("\\s+")){
			    	            		String srun = xslfParagraph.getText().trim();
			    	            		if(srun.trim().length()>3){
			    	            			// pptx file name, slide content
			    	            			slidesRelationhashMap.put(this.getTxtFileName(), srun);
			    	            			
				    	            		if(i < maxMetaSearchNo){
				    	            			String [] metaData = this.getMetaData(srun.trim());
				    	            			authors += metaData[0];
				    	            			datePublished += metaData[1];
				    	            			
				    	            		}// end of if(i < maxSearchNo){
				    	            		
				    	            		srun =srun.replaceAll("[^\\p{L}\\p{N}\\s\\,\\.\\;\\?\\-\\'\\(\\)]|(^\\p{N}+\\.*\\p{N}*)", "").trim();
				    	            		if (!srun.matches("(\\[[0-9\\.]+\\])|Fig. [0-9\\.]+|Figure [0-9\\.]+") 
				    	            				&& !(srun.matches("\\s+")) && !(srun.matches("[^\\p{L}]+"))){
				        	            		//append text
				    	            			if (!srun.equals(""))
					    	            		stringBuffer.append(srun+".\n");	
					    	            		// extract format info
					    	            		String parentText = "";
					    	            		String parentIndentWithBullet ="";
					    	            		String indentWithBullet = xslfParagraph.isBullet()? (""+xslfParagraph.getLeftMargin() + 
					    	            				xslfParagraph.getBulletCharacter()):""+xslfParagraph.getLeftMargin();
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
	
						    	            	lastText = srun;
						    	            	lastIndentWithBullet = indentWithBullet;
						    	            	String [] record = {srun, parentText, parentIndentWithBullet};
						    	            	slidesRelationhashMap.put(srun, parentText);
				    	            	
						    	            	hashLevels.put(indentWithBullet, record);
				    	            		}// end of if (!srun.matches("(\\[[0-9\\.]+\\])|Fig. [0-9\\.]+|Figure [0-9\\.]+")){			    	            	
				    	            	}// end of if (!xslfParagraph.getText().matches("\\s+")){	
			    	            	}// end of if (xslfParagraph.getText() != null){
		    	            	}//end of if(srun.length()>2){
		    	            }// end of for (XSLFTextParagraph xslfParagraph : txShape.getTextParagraphs()){		    	            
	    	          }// end of (sh[j] instanceof XSLFTextShape)   
    	          }// end of for (int j = 0; j < sh.length; j++){
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
		} catch (Exception e) {

			return null;
		} 
   }	
        public static void main(String[] args) throws Exception{
        	
            AbstractSequenceClassifier<CoreLabel> classifier;
            String  serializedClassifier = "classifiers/english.muc.7class.distsim.crf.ser.gz";
            classifier = CRFClassifier.getClassifier(serializedClassifier);
            
        	Document pptxContent = new StripPPTXContent();
        	pptxContent.setClassifier(classifier); 
    		pptxContent.readContent("R:/Intel/Materials/Ch6.pptx");
    		if (pptxContent.getCont() != null){
	    		//System.out.println(pptxContent.getCont());
	    		//System.out.println(pptxContent.getDatePublished());	
	    		//System.out.println(pptxContent.getAuthors());	
	    		HashMap <String, String>RelationhashMap =pptxContent.getSlidesRelationhashMap();
	    		if (RelationhashMap != null){
	    		System.out.println(RelationhashMap.size());    	
	    		}	
    		}
	}

}
