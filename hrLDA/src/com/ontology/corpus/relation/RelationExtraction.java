package com.ontology.corpus.relation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hrLDA.readdocs.FileList;

public class RelationExtraction {

	final private static String NS = "http://semiconductor-packaging/ontology/";
    final private static OntModel model = ModelFactory.createOntologyModel( OntModelSpec.RDFS_MEM );	
    OntClass ontparentClass = model.createClass( NS+ "Bird");
    /**
     * 
     * @param file
     * @return text content
     * @throws Exception
     */
    public String readTXT(File file) {
    	try{
        return FileUtils.readFileToString(file);    
		} catch (Exception e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
			return "";
		} 
   }
    @SuppressWarnings("static-access")
	public void Convert(String folderpath)throws Exception{
    	File readfile =null;
    	int counter=0;
    	String regex="(([A-Z][\\'\\-a-z]+\\s)+)\\(([^\\(\\)]+?)\\)\\s?(i|wa)s a (.+) .?bird (of|in) the (.+?)\\..+"; 
    	
//    	String regex="(([A-Z][\\'\\-a-z]+\\s)+)\\((.+)\\)\\s?((i)|(wa))?s a (.+) .?bird";//Bendire's Thrasher (Toxostoma bendirei) a perching bird native
//      String regex="([A-Z][\\'\\-a-z]+\\s)+(i)|(wa)s (.\\,\\(\\))+ bird of the (.\\,)+ \\."; 
//    	String regex="The Acadian Flycatcher \\((Empidonax virescens)\\) is a small insect-eating bird of the tyrant flycatcher family\\.";
        Pattern pattern = Pattern.compile(regex);        		
        Matcher m;
        
        
        String full_filename, filename, suffix, cont, matchedContent,birdName, alias,feature,family;
        
        
        try{
        	
        	FileList fileList = new FileList("txt,ppt,pptx,pdf,doc,docx,xls,xlsx");
			Vector<String> result=fileList.getList(folderpath);
        	for (String vector :result){        		        		
        		readfile = new File(vector); 
				full_filename=readfile.getName();
				filename=full_filename.contains(".")?full_filename.substring(0, full_filename.lastIndexOf(".")):"";				  
				suffix =full_filename.contains(".")?full_filename.substring(full_filename.lastIndexOf(".")+1):"";
				cont="";	
				matchedContent ="";
				alias="";
				family="";
				feature="";
//            	System.out.println(vector);
//            	System.out.println("name=" + filename); 
            	cont = readTXT(readfile);
//            	System.out.println(cont);
	            
	            m = pattern.matcher(cont);
	            if (m.find()) {   
	            	birdName = m.group(1); // bird name 
	            	alias = m.group(3); // alias
	            	feature=m.group(5);//feature
	            	family= m.group(7);//family
	            	
	            	System.out.println("birdName: "+birdName);
	            	System.out.println("alias: "+alias);
	            	System.out.println("feature: "+feature);
	            	System.out.println("family: "+family);
//	            	System.out.println("MatchedContent: "+matchedContent);
            	}
	            counter++;
	       }		            
        	System.out.println("Converting "+counter+" Files Done!");
        	System.out.println(); 
        	
        }catch(Exception e){
            System.out.println("Strip failed.");            
            e.printStackTrace();
            
        }
}
    	
	    
	// is a xxx bird in xxx
	public void SConvert(String folderpath)throws Exception{
    	File readfile =null;
    	int counter=0;
    	String regex="(([A-Z][\\'\\-a-z]+\\s)+)\\(([^\\(\\)]+?)\\)[^\\.]* (is )?a (.+?)\\..?";    	
        Pattern pattern = Pattern.compile(regex);        		
        Matcher m;
        
        
        String full_filename, filename, suffix, cont, matchedContent,birdName, alias,features,feature,family;
        
        
        try{
        	
        	FileList fileList = new FileList("txt,ppt,pptx,pdf,doc,docx,xls,xlsx");
			Vector<String> result=fileList.getList(folderpath);
        	for (String vector :result){        		        		
        		readfile = new File(vector); 
				full_filename=readfile.getName();
				filename=full_filename.contains(".")?full_filename.substring(0, full_filename.lastIndexOf(".")):"";				  
				suffix =full_filename.contains(".")?full_filename.substring(full_filename.lastIndexOf(".")+1):"";
				cont="";	
				matchedContent ="";
				alias="";
				family="";
				features="";
				feature="";
            	cont = readTXT(readfile);
            	System.out.println("name=" + vector);
//            	System.out.println(cont);
	            m = pattern.matcher(cont);
	            if (m.find()) {   
	            	birdName = m.group(1); // bird name 
	            	alias = m.group(3); // alias
	            	features=m.group(5);//feature
//	            	family= m.group(7);//family
	            	OntClass ontChildClass = model.createClass( NS+ birdName.replace("The ", "").trim().replaceAll(" ", "_"));
	            	ontChildClass.addSuperClass(ontparentClass);
	            	
	                OntProperty  propertyA = model.createOntProperty( NS+"alias");
	                ontChildClass.addProperty(propertyA, alias.trim().replaceAll(" ", "_"));	            	
	            	System.out.println("birdName: "+birdName);
	            	System.out.println("alias: "+alias);
//	            	System.out.println("features: "+features);
//	            	System.out.println("family: "+family);
//	            	System.out.println("MatchedContent: "+matchedContent);
	            	
	            	String regexF="(.+) (in|of) the (.+?) family";    	
	                Pattern patternF = Pattern.compile(regexF);        		
	                Matcher mF = patternF.matcher(features);
	                if(mF.find()){
	                	feature=mF.group(1);
	                	System.out.println("feature: "+feature);
	                	family=mF.group(3);
	                	System.out.println("family: "+family);	
	                	String strings[] = feature.split(" ");
	                	if (strings.length>2){
			                OntProperty  property = model.createOntProperty( NS+"size");
			                ontChildClass.addProperty(property, strings[0].trim().replaceAll(" ", "_"));

			                OntProperty  propertyT = model.createOntProperty( NS+"type");
			                ontChildClass.addProperty(propertyT, (strings[1]+" "+strings[2]).trim().replaceAll(" ", "_"));		                	                		
	                	}else{
			                OntProperty  propertyT = model.createOntProperty( NS+"type");
			                ontChildClass.addProperty(propertyT, feature.trim().replaceAll(" ", "_"));		                
	                	}	                
		                OntProperty  propertyF = model.createOntProperty( NS+"family");
		                ontChildClass.addProperty(propertyF, family.trim().replaceAll(" ", "_"));		                
	                }
            	}
	            counter++;
	       }	
        	
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream("resultB.rdf"),"UTF-8");
			
			try {
				model.write( out, "RDF/XML" );
			} finally {
				// TODO: handle exception
				  if (out != null) {
					    try {out.close();} catch (IOException ignore) {}
					  }
			}        	
        	System.out.println("Converting "+counter+" Files Done!");
        	System.out.println(); 
        	
        }catch(Exception e){
            System.out.println("Strip failed.");            
            e.printStackTrace();
            
        }
}
    	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		RelationExtraction relationExtraction = new RelationExtraction();
		relationExtraction.SConvert("../testCorpus/BirdsOfTheUnitedStates/");

	}

}
