package cc.mallet.pipe;

import java.sql.ResultSet;
import java.text.BreakIterator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.modeshape.common.text.Inflector;

import cc.mallet.types.TokenSequence;
import cc.mallet.util.IndexWrapper;
import cc.mallet.util.JavaOllieWrapper;
import cc.mallet.util.MySqlDB;
import cc.mallet.util.MyTagger;
import edu.knowitall.ollie.OllieExtraction;
import edu.knowitall.ollie.OllieExtractionInstance;
import edu.knowitall.ollie.confidence.OllieConfidenceFunction;
import edu.knowitall.tool.conf.impl.LogisticRegression;

public class PhraseRunnable implements Runnable{

	private boolean isFinished = false;
	private  MySqlDB mySqlDB;
	HashSet<String> stoplist = null;
	private  int counter =0;
	private  int threadID;
	
	private String [] subResultArray;
	TokenSequence tmpTS = new TokenSequence();
	
	MyTagger myTagger;
	Inflector inflector;
	JavaOllieWrapper ollieWrapper;
	String end, fileName;

	int startChunk;
	int numChunks;	
	
	public int getCounter() {
		return this.counter;
	}
	public boolean getIsFinished() {
		return this.isFinished;
	}
	public TokenSequence getTmpTS(){
		return this.tmpTS;
	}
	public PhraseRunnable(String [] subResultArray, MySqlDB mySqlDB, 
			HashSet<String> stoplist, String end, String fileName,
			MyTagger myTagger, Inflector inflector,
			JavaOllieWrapper ollieWrapper,
			int threadID, int startChunk, int numChunks){
		this.subResultArray = subResultArray;
    	this.mySqlDB = mySqlDB;
    	this.stoplist = stoplist;
    	this.end = end;
    	this.fileName = fileName;
    	this.myTagger = myTagger;
    	this.inflector = inflector;
    	this.ollieWrapper = ollieWrapper;
    	this.threadID = threadID;
    	this.startChunk = startChunk;
    	this.numChunks = numChunks;    	
    }
	
	public boolean isAbbreviationSentence (String sentence){
		if (sentence.split(" ").length>=3 
				&& !sentence.matches("[^\\x00-\\x7F]") 
				//we don't need this
				//&& !stoplist.contains(sentence)
			)return true;
		else return false;
	}
	
	public boolean isTitleSentence(String sentence){
		 if (sentence.replaceAll("[\\.=]","").split(" ").length <=6 && 
					(
						(this.end.equals("")) || 
						(
						! this.end.equals("") 
						&& sentence.toLowerCase().contains(this.end.toLowerCase())
						)
					)
			 ) return true;		
		 else return false;
	}
	
	public boolean isGoodSentence(String sentence){
		if ((
				(this.end.equals("")) || 
				(
				! this.end.equals("") 
				&& sentence.toLowerCase().contains(this.end.toLowerCase())
				)
			))return true;
		else return false;
	}
	
	public boolean checkEndStringSentenceStatus(String sentence){
		if(
				(this.end.equals("")) || 
				(
				! this.end.equals("") && sentence.toLowerCase().contains(this.end.toLowerCase())
				)
			)return true;
		else return false;
	}
	
	public boolean isGoodTriplet(String s, String s2, String relation, double conf){
		if (s.length()>2 && !stoplist.contains(s)
						 && !stoplist.contains(s2)
						 && relation.length()>1&& s2.matches("^[A-Za-z\\s*\\-\\'']+[A-Za-z\\s*]+$") 
						 && relation.matches("^[A-Za-z\\s*\\-\\']+[A-Za-z\\s*]+$")
						// &&conf >=0.5
		)return true;
		else return false;
	}
	
	public int getLengthOfShortestWord(String phrase){
		int length = Integer.MAX_VALUE;
		for (String word: phrase.split(" ")){
			length = word.length() < length?word.length():length;
		}
		return length;
	}
	
	public boolean isGoodKeyPhrase(String s, String s2, String relation){
		if (getLengthOfShortestWord(s)>= 2 && getLengthOfShortestWord(s2)>= 2)
		return true;
		else return false;
	}
	
	public boolean addTotmpTS (String s){
		if (s.replaceAll(" ", "").length() >= 3){
			if (stoplist.contains(s)) return false;
			if (s.matches("[0-9-_]+")) return false;
			else if (s.matches("[a-zA-Z][a-zA-Z\\s+-_\\']+[0-9]*")) return true;
			else return false;
		}
		else return false;
	}
	 public String cleanPhrase(String s){
		 s = s.trim().replaceAll("\\s+", "_").replaceAll("[\\.=]","").replaceAll("[^\\x00-\\x7F]", "")
				 .toLowerCase();
		 return s;
	 }	
	@Override
	public void run() {
        
		//System.out.println("threadID: " + this.threadID);
		//System.out.println("fileName: " + this.fileName);
		
		  for (int chunkID = startChunk;  chunkID < subResultArray.length 
				  && chunkID < startChunk + numChunks; chunkID++){
			  			  
			//String record = "";// each record that is going to be saved in the database.
			BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
			
			String chunk = subResultArray[chunkID];
			iterator.setText(chunk);
			int start = iterator.first();
			LogisticRegression confFunc;
			Iterable<OllieExtractionInstance> extrs;
			int sentenceID =0;
			for (int endP = iterator.next();
			    endP != BreakIterator.DONE;
			    start = endP, endP = iterator.next()) {
				
				String sentence = chunk.substring(start,endP).replaceAll("\\s+"," ")
						.replaceAll("\"","").trim();
				sentenceID ++;
				
				// start to find titles and acronyms
				
				if (isTitleSentence(sentence) &&  fileName.contains("ppt"))
					/**find title Lists*/
					{
				        	String titleSentence = cleanPhrase(sentence);	
							if (addTotmpTS(titleSentence)){
					        	tmpTS.add(titleSentence, 1.0, chunkID, sentenceID);	
				        	}					        	
					}// end of if (isPPTSentence(sentence))		
				
				/**find abbreviation Lists*/
				else if (isAbbreviationSentence(sentence)
					 && checkEndStringSentenceStatus(sentence)
					){
					
						/**Abbreviation*/
						//if (Character.isUpperCase(sentence.codePointAt(0))){
							
							ArrayList <String[]> abbreviationLists = findIndexesForKeyword("\\([^(]+\\)", sentence);
							
							if (abbreviationLists != null){
								if (abbreviationLists.size() >0){
									System.out.println("calculating the abbreviation list");
									for(String[] arrays : abbreviationLists){
										System.out.println(arrays[0] + " be short for "+ arrays[1]);
										String s1 = cleanPhrase(arrays[0]).toUpperCase();
										String s2 = cleanPhrase(arrays[1]);	
										
										if (addTotmpTS(s1)){
						         			tmpTS.add(s1, 2.0, chunkID, sentenceID);
						         			// don't add objects
						         			tmpTS.add(s2, 2.0, chunkID, sentenceID );
							
											mySqlDB.insertData(s1, "be short for", s2, this.fileName, sentence);	
										}// end of if (addTotmpTS(s1))

									}// end of for(String[] arrays : abbreviationLists)
								}// end of if (abbreviationLists.size() >0)
							}// end of if (abbreviationLists != null)
						//}// end of if (Character.isUpperCase(sentence.codePointAt(0)))	
						/** end of find acronyms*/							

						/**find key phrases*/
						if (isGoodSentence(sentence)){
								sentence = sentence.replaceAll("(?u)null", "");
						        extrs = ollieWrapper.extract(sentence);
						        		
						        if (extrs != null){
							        // print the extractions.
							        confFunc = OllieConfidenceFunction.loadDefaultClassifier();
							        for (OllieExtractionInstance inst : extrs) {
							        	HashSet tempHashSet = new HashSet();
							            double conf = confFunc.getConf(inst);
							            OllieExtraction extr = inst.extr();
			
							            String s = (extr.arg1().text()+"").trim();
							            String s2 = (extr.arg2().text()+"").trim();
							            String relation = (extr.rel().text()+"").trim();            
			
								    	// Without using  MS ngram service
			
										if (isGoodTriplet(s, s2, relation, conf)){
								            // begin new version POS
								            HashSet <String> keyPhraseSet = getKeyPhrase(s);
								            s2 = getObjectKeyPhrase(s2);
								            s2 = inflector.singularize((Normalizer.normalize(s2.trim().toLowerCase(), Normalizer.Form.NFD))
								            		.replaceAll("[^\\x00-\\x7F]", ""));  									    
											relation = (Normalizer.normalize(relation, Normalizer.Form.NFD))
													.replaceAll("[^\\x00-\\x7F]", "");		        				   		             
			
			
										    for (String keyPhrase: keyPhraseSet){
										    	// System.out.println(keyPhrase);
			
						    		        	if (isGoodKeyPhrase(s, s2, relation)){
						    		        		
									   		        s = keyPhrase.replaceAll("[^\\x00-\\x7F]", "");							            		
										 			s = inflector.singularize((Normalizer.normalize(s.trim().toLowerCase(), 
										 					Normalizer.Form.NFD)));
							    		        	s= s.replaceAll(" ","_");
							    		        	s2= s2.replaceAll(" ","_");	
							    		        	
						    		        		if (!tempHashSet.contains(s+s2)){
						    		        			if (addTotmpTS(s)){
				      			     	      				tempHashSet.add(s+s2);
								   		         			tmpTS.add(s, 2.0,chunkID, sentenceID );
								   		         			// long key phrase; we put the focus word into tempTS as well
								   		         			if (s.contains("_"))
								   		         				tmpTS.add(s.substring(s.lastIndexOf('_')+1, s.length()),
								   		         						2.0,chunkID, sentenceID );
											   		        //System.out.println("TokenSequenceNRelationsNoMS "+chunkID+" "+sentenceID );
								   		         			// do not add objects
											   		        //tmpTS.add(s2, 1.0,chunkID, sentenceID );
								   		         			
												            s=s.replaceAll("_"," ");
												            relation = relation.replaceAll("_"," ");
												            s2 = s2.trim().replaceAll("_"," ");
												            mySqlDB.insertData(s, relation, s2, this.fileName, sentence);
										 	         	    System.out.println("Subject= " + s);
												            System.out.println("Relationship= " + relation);
												            System.out.println("Object=" + s2);
															//System.out.println("Conf=" + conf);  
															//System.out.println("OP Relationship= " + "OP "+relation);	
												   		    //record = "SUBJECT: "+s+ "  RELATION: "+relation+"  OBJECT: "+s2+"\nSENTENCE: "+sentence;												            			            
												            System.out.println();	     			
							   		         			}// end of if (addTotmpTS(s))
						    		        		}// end of if (!tempHashSet.contains(s+s2)){

						    		        	}// end of if (isGoodKeyPhrase(s, s2, relation))								    	
										    }// end of for (String keyPhrase: keyPhraseSet){									        
			
							            }// end of if (isGoodTriplet(s, s2, relation, conf))
							            	            
							        }// end of for (OllieExtractionInstance inst : extrs)	
							    }// end of if (extrs != null){
			
						}// end of if (isGoodSentence(sentence))
						/** end of find key phrases*/		
						
				}// end of if (isAbbreviationSentence(sentence)				
													
			}// end of for (int endP = iterator.next()...
		
		}// end of for (int chunkID = startChunk;...
		
	  	this.isFinished = true;		
		
	}

	
	public String getObjectKeyPhrase(String s){
		String tagged = myTagger.Tag(s);
	    String [] tags = tagged.split(" ");

	    String phrase =s;
	    if (tags.length >0){
        	String tag = tags[0].split("_")[1];
        	String word = tags[0].split("_")[0];   
        	if (tag.equals("DT")){
        		phrase = s.substring(s.indexOf(word)+word.length()+1,s.length());
        	} 
        }	    
	    return phrase;
	}
	public HashSet<String> getKeyPhrase(String s){
		String tagged = myTagger.Tag(s);
	    String [] tags = tagged.split(" ");
	    boolean isNewNoun = false;
	    boolean isIn = false;
	    String phrase ="";
	    String candidatePart = "";
	    HashSet<String> keyPhraseSet = new HashSet<String>();
	    
	    if (tags.length >0){
	        if ( (tagged.contains("_CC ") && tagged.contains("_IN")) 
	        		|| tagged.contains("_TO ") || tagged.contains("VBG") ){
	        	phrase = s;
	        	String tag = tags[0].split("_")[1];
	        	String word = tags[0].split("_")[0];   
	        	if (tag.equals("DT")){
	        		phrase = s.substring(s.indexOf(word)+word.length()+1,s.length());
	        	}    	
	        }else{
	            for (int i = tags.length-1; i >= 0; i--){
	            	String tagWords = tags[i];
	            	if (tagWords.length() > 1){
	                	String tag = tagWords.split("_")[1];
	                	String word = tagWords.split("_")[0];

	                	if (tag.length()>1){
	                    	switch(tag.substring(0,2)){
	                		case "JJ":
	                			if (isNewNoun) {
	                				phrase = word + " "+phrase;
	                			}else{
	                				isIn = false;
	                			}
	                			break;
	                		case "VB":
	                			if (tag.equals("VBN") && isNewNoun) {
	                				phrase = word + " "+phrase;
	                			}else{
	                				isNewNoun = false;
	                				isIn = false;
	                			}
	                			break; 
	                		case "RB":
	                			if (isNewNoun) {
	                				phrase = word + " "+phrase;
	                			}else{
	                				isIn = false;
	                			}
	                			break;                			
	                		case "NN":
	                			isNewNoun = true;
	                			if (isIn && !candidatePart.equals("")) {
	                				phrase = candidatePart + " "+phrase;
	                				candidatePart = "";
	                			}
	                			if (tag.equals("NNS")){
	                				word = inflector.singularize(word);
	                			}
	                			phrase = word + " "+phrase;
	                			isIn = false;
	                			break;
	                		case "IN":
	                			if (isNewNoun) {        				
	                				candidatePart = word;
	                				isIn = true;                				
	                			}else{
	                				isIn = false;
	                			}
	                			break;   
	                		case "PO":
	                			if (isNewNoun) {        				
	                				phrase = word + " "+phrase;                				
	                			}else{
	                				isIn = false;
	                			}
	                			break;	                			 
	                		case "DT":
	                			if (!isNewNoun) {        				
	                				isIn = false;
	                			}
	                			break;                 			
	            			default:
	        					if (!phrase.trim().equals("") && !keyPhraseSet.contains(phrase.trim())){
	        						keyPhraseSet.add(phrase.trim());
	        					}
	        					phrase = "";
	            				isNewNoun = false;
	            				isIn = false;
	            				break;       			
	                    	}// end of switch()
	        				         		
	                	}// end of if (tag.length()>1){
	          		
	            	}// end of if (tagWords.length() > 1){

	            }// end of for for (int i = tags.length-1; i >= 0; i--){
	        }// end of if (tagged.contains("_CC ") && tagged.contains("_IN")){
	    }// end of if (tags.length >0)


	    
		if (!phrase.trim().equals("") && !keyPhraseSet.contains(phrase.trim())){
			keyPhraseSet.add(phrase.trim());
		}// for a whole long noun phrase
		return keyPhraseSet;
	}
   public static ArrayList <String[]> findIndexesForKeyword(String keyword, String searchString) {
        String regex = keyword;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(searchString);
 
        List<IndexWrapper> wrappers = new ArrayList<IndexWrapper>();
        ArrayList <String[]> abbreviationList = new ArrayList <String[]>();
 
        while(matcher.find() == true){
            int end = matcher.end();
            int start = matcher.start();
            IndexWrapper wrapper = new IndexWrapper(start, end);
            wrappers.add(wrapper);
        }
        
		for (IndexWrapper index : wrappers){
			boolean isAbbreviation = true;
			String candidateAbbreviation = searchString.substring(index.getStart()+1, index.getEnd()-1).trim();
			
			String [] words = searchString.substring(0,index.getStart()).split(" ");
			String completeString = "";
			if (candidateAbbreviation.length() >1 && words.length > 0){
				for (int i= words.length-1, j =candidateAbbreviation.length()-1; j >= 0 && i >= 0; j--, i--){
					
					if (words[i].length() >1){
						if (Character.toLowerCase(words[i].charAt(0)) 
							!= Character.toLowerCase(candidateAbbreviation.charAt(j))){
							isAbbreviation = false;
							break;
						}else{
							completeString = words[i]+" "+completeString;
						}
					}

				}
			}else{
				isAbbreviation = false;
			}

			if (isAbbreviation){
				abbreviationList.add(new String[] {candidateAbbreviation, completeString.trim()});
			}
		}        
        return abbreviationList;
    }	
}// end of FileReaderRunnable
