
package cc.mallet.pipe;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.modeshape.common.text.Inflector;

import rita.wordnet.RiWordnet;
import cc.mallet.types.Instance;
import cc.mallet.util.JavaOllieWrapper;
import cc.mallet.util.MySqlDB;
import cc.mallet.util.MyTagger;

public class TokenSequenceNRelationsNoMS extends Pipe implements Serializable
{
	int [] gramSizes = null;

	HashSet<String> stoplist = null;
	// Would pass in a PApplet normally, but we don't need to here
	RiWordnet wordnet = new RiWordnet(null);
	String end="";
	static MySqlDB mySqlDB;
	static MyTagger myTagger;
	Inflector inflector;
	JavaOllieWrapper ollieWrapper;
	int numOfThreads = 1;

//	File carriarFile = new File ("carriarLog.txt");// each document	
//	BufferedWriter carrierBufferedWriter = new BufferedWriter(new PrintWriter(carriarFile));
	

//	File sentenceFile = new File ("sentenceLog.txt");// each sentence	
//	BufferedWriter sentenceBufferedWriter = new BufferedWriter(new PrintWriter(sentenceFile));	
//
//	File synonymyFile = new File ("synonymyLog.txt");// each record	
//	BufferedWriter synonymyBufferedWriter = new BufferedWriter(new PrintWriter(synonymyFile));			
//
//	File recordFile = new File ("recordLog.txt");// each record	
//	BufferedWriter recordBufferedWriter = new BufferedWriter(new PrintWriter(recordFile));

	private HashSet<String> newDefaultStopList ()
	{
		HashSet<String> sl = new HashSet<String>();
		for (int i = 0; i < stopwords.length; i++)
			sl.add (stopwords[i]);
		return sl;
	}
    
	public TokenSequenceNRelationsNoMS (int [] sizes, String end) throws Exception
	{
		this.gramSizes = sizes;
		this.stoplist = newDefaultStopList();
		this.end = end;
		this.inflector = new Inflector(); 
		this.ollieWrapper = new JavaOllieWrapper();
	}

	public TokenSequenceNRelationsNoMS (File stoplistFile, String encoding, int [] sizes, 
			boolean includeDefault, String end, 
			String databaseName,String userName,String password, 
			String tableName, String tempRelationTableName,
			int numOfThreads) throws Exception{
		this.gramSizes = sizes;
		if (! includeDefault) { this.stoplist = new HashSet<String>(); }
		else { this.stoplist = newDefaultStopList(); }

		addStopWords (fileToStringArray(stoplistFile, encoding));
		this.end = end;
		this.mySqlDB= new MySqlDB(databaseName, userName, password, 
				tableName, tempRelationTableName);
		mySqlDB.createTable();
		this.myTagger = new MyTagger();
		this.inflector = new Inflector(); 
		this.ollieWrapper = new JavaOllieWrapper();
		this.numOfThreads = numOfThreads;
	
	}

	public Instance pipe (Instance carrier)
	{
		String source = (String)carrier.getData();

//		TokenSequence tmpTS = new TokenSequence();		
		// Looks on the classpath for the default model files.
//		try{
//			carrierBufferedWriter.write(source+"\n********************\n");
//			carrierBufferedWriter.flush();
//		}catch(IOException e){
//			System.err.println("Writting to carrierLog failed");
//		}finally{
//
//		}
		
		numOfThreads = 1;
		try {
			String [] chunkArray = source.split("\n");
			PhraseRunnable [] runnables = new PhraseRunnable[numOfThreads];

			int chunksPerThread = chunkArray.length / numOfThreads;
			int offset = 0;
			
			
			if (numOfThreads > 1) {	
				for (int thread = 0; thread < numOfThreads; thread++) {
					System.out.println("offset: "+offset);
					// some chunks may be missing at the end due to integer division
					if (thread == numOfThreads - 1) {
						chunksPerThread = chunkArray.length - offset;
					}
					
					runnables[thread] = new PhraseRunnable(chunkArray, mySqlDB,
							stoplist, end, carrier.getName().toString(),
							myTagger, inflector,
							ollieWrapper,
							thread, offset, chunksPerThread);
					offset += chunksPerThread;
				}				
				
			}else{
				runnables[0] = new PhraseRunnable(chunkArray, mySqlDB,
						stoplist, end, carrier.getName().toString(),
						myTagger, inflector,
						ollieWrapper, 0, offset, chunksPerThread);
			}

			ExecutorService executor = Executors.newFixedThreadPool(numOfThreads);
			for (int thread = 0; thread < numOfThreads; thread++) {
				
				System.out.println("submitting thread " + thread);
				executor.submit(runnables[thread]);			
			}	
			
			
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				
			}
			
			boolean finished = false;
			while (! finished) {
				
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					
				}
				
				finished = true;
				
				// Are all the threads done?
				for (int thread = 0; thread < numOfThreads; thread++) {
//					System.out.println("thread " + thread + " done? " + runnables[thread].getIsFinished());
					finished = finished && runnables[thread].getIsFinished();
				}
				
			}			
			
			
			for (int thread = 0; thread < numOfThreads; thread++) {
				carrier.setData(runnables[thread].getTmpTS());
			}			
			executor.awaitTermination(1, TimeUnit.SECONDS);
			executor.shutdown();
			System.out.println("All tasks are finished!");	
		} catch (InterruptedException e) {
		}
		
		
/**		old script for extracting phrases
		int chunkID =0;
        for (String chunk: source.split("\n")){
        	chunkID ++;
	        String record = "";// each record that is going to be saved in the database.
	        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
	
			iterator.setText(chunk);
			int start = iterator.first();
			LogisticRegression confFunc;
			Iterable<OllieExtractionInstance> extrs;
			int sentenceID =0;
			for (int endP = iterator.next();
			    endP != BreakIterator.DONE;
			    start = endP, endP = iterator.next()) {
				
				String sentence = chunk.substring(start,endP).replaceAll("\\s+"," ").replaceAll("\"","").trim();
				sentenceID ++;
				if (sentence.split(" ").length>2 
					&& !sentence.matches("[^\\x00-\\x7F]") 
					&& !stoplist.contains(sentence)
					// && (
					// 		(this.end.equals("")) || 
					// 		(
					// 		! this.end.equals("") && sentence.toLowerCase().contains(this.end.toLowerCase())
					// 		)
					// 	)
					){
					if (Character.isUpperCase(sentence.codePointAt(0))){
						try{
							sentenceBufferedWriter.write("SENTENCE: "+sentence+"\n");
							sentenceBufferedWriter.flush();
	
							ArrayList <String[]> abbreviationLists = findIndexesForKeyword("\\([^(]+\\)", sentence);
							
							if (abbreviationLists != null){
								if (abbreviationLists.size() >0){
									for(String[] arrays : abbreviationLists){
										System.out.println(arrays[0] + " "+ arrays[1]);
										String s2 = inflector.singularize((Normalizer.normalize(arrays[1], Normalizer.Form.NFD)).replaceAll("[^\\x00-\\x7F]", ""));	
										synonymyBufferedWriter.write("SYNONYM: "+arrays[0] + " "+ s2+"\n"+sentence+"\n\n");
										synonymyBufferedWriter.flush();
			   		         			tmpTS.add(arrays[0], 2.0, chunkID, sentenceID);
			   		         			tmpTS.add(s2, 2.0, chunkID, sentenceID );
			   		         			
//			   		         			tmpTS.add(arrays[0]);
//			   		         			tmpTS.add(s2);							
										mySqlDB.insertData(arrays[0], "be short for", s2, carrier.getName().toString(), sentence);
									}
								}
							}						
						}catch(IOException e){
							System.err.println("Writting to log failed");
						}finally{
						}
				
						// save each record to recordLog.txt
						
						try{		
							
							if (sentence.split(" ").length < 3|| 
									(
										(this.end.equals("")) || 
										(
										! this.end.equals("") 
										&& sentence.toLowerCase().contains(this.end.toLowerCase())
										)
									)
								){
						        	String pptSentence = sentence.substring(0, sentence.length()-1);
						        	
						        	if (carrier.getName().toString().contains("ppt")){
										try{
											ResultSet rs = mySqlDB.queryRelations(pptSentence);
											ResultSet oRs = mySqlDB.queryObjectRelations(pptSentence);
	
											if ((rs != null) && (oRs != null)){
					
												if ( 
														((rs.getFetchSize()>0) || (oRs.getFetchSize()>0)) 
														&& (
																(this.end.equals("")) || 
																(
																! this.end.equals("") && sentence.toLowerCase().contains(this.end.toLowerCase())
																)
															)
													){
													
										        	record = pptSentence;
													recordBufferedWriter.write("WHOLE SENTENCE:"+record+"\n********************\n");
													recordBufferedWriter.flush();	
	
										        	pptSentence = pptSentence.trim().replaceAll("_"," ").replaceAll("=","");						        	
//										        	tmpTS.add(pptSentence);
										        	tmpTS.add(pptSentence, 1.0, chunkID, sentenceID);
										        	System.out.println("PPT(X) RELATION: "+pptSentence);													        	
												}		
											}				
										}catch(Exception e){
											System.err.println("Whole sentence area --- Fail to check if the sentence exists!");
										}	        	
									}
							}// end of if (sentence.split(" ").length < 6)
	
							if (
									(
										(this.end.equals("")) || 
										(
										! this.end.equals("") 
										&& sentence.toLowerCase().contains(this.end.toLowerCase())
										)
									)
								){
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
	
											if (s.length()>2 && !stoplist.contains(s) && !stoplist.contains(s2) && relation.length()>1&& s2.matches("^[A-Za-z\\s*\\-\\'']+[A-Za-z\\s*]+$") && relation.matches("^[A-Za-z\\s*\\-\\']+[A-Za-z\\s*]+$")&&conf >0.5){
	
									            recordBufferedWriter.write("ORIGINAL SUBJECT: "+s+"\n"); 
									            recordBufferedWriter.flush();
	
									            // begin new version POS
									            HashSet <String> keyPhraseSet = getKeyPhrase(s);
									            s2 = getObjectKeyPhrase(s2);
											    // end of od new version POS
									            s2 = inflector.singularize((Normalizer.normalize(s2, Normalizer.Form.NFD)).replaceAll("[^\\x00-\\x7F]", ""));  									    
												relation = (Normalizer.normalize(relation, Normalizer.Form.NFD)).replaceAll("[^\\x00-\\x7F]", "");		        	
							   		            // relation = (relation.length()>4)?inflector.singularize((Normalizer.normalize(relation, Normalizer.Form.NFD)).replaceAll("[^\\x00-\\x7F]", "")) : (Normalizer.normalize(relation, Normalizer.Form.NFD)).replaceAll("[^\\x00-\\x7F]", "");					   		             

	
											    for (String keyPhrase: keyPhraseSet){
											    	// System.out.println(keyPhrase);
	
									   		        s = keyPhrase.replaceAll("[^\\x00-\\x7F]", "");							            		
		         			
							    		        	s= s.replaceAll(" ","_");
							    		        	s2= s2.trim().replaceAll(" ","_");
							    		        	if (!s.equals("") && !s2.equals("") && !relation.equals("")){
	
							    		        		if (!tempHashSet.contains(s+s2)){
					      			     	      			tempHashSet.add(s+s2);
								   		         			tmpTS.add(s.toLowerCase(), 2.0,chunkID, sentenceID );
//								   		         			System.out.println("TokenSequenceNRelationsNoMS "+chunkID+" "+sentenceID );
//								   		         			tmpTS.add(s.toLowerCase());
								   		         			// do not add objects
//								   		         			tmpTS.add(s2);
							    		        		}
	
											            s=s.replaceAll("_"," ");
											            relation = relation.replaceAll("_"," ");
											            s2 = s2.trim().replaceAll("_"," ");
											            mySqlDB.insertData(s, relation, s2, carrier.getName().toString(), sentence);
									 	         	    System.out.println("Subject= " + s);
											            System.out.println("Relationship= " + relation);
											            System.out.println("Object=" + s2);
//											            System.out.println("Conf=" + conf);  
//											   		    System.out.println("OP Relationship= " + "OP "+relation);	
											   		    record = "SUBJECT: "+s+ "  RELATION: "+relation+"  OBJECT: "+s2+"\nSENTENCE: "+sentence;
														
											            recordBufferedWriter.write(record+"\n********************\n");
											            recordBufferedWriter.flush();
	
											            if (relation.contains("short for") || relation.contains("also called")){
	
															synonymyBufferedWriter.write("SYNONYM FROM RELATION EXTRACTION: "+s + " "+ s2+"\n"+sentence+"\n\n");
															synonymyBufferedWriter.flush();									            
														}
											            			            
											            System.out.println();	
										     	}								    	
											    }									        
	
								            }// end of if (s.length()>8 && s2.matches("^[A-Za-z\\-]+$") && relation.matches("^[A-Za-z\\-]+$") &&conf >0.4){	
								            	            
								        }// end of for (OllieExtractionInstance inst : extrs)	
								    }// end of if (extrs != null){
	
							}// end of if (sentence.toLowerCase().contains(this.end.toLowerCase())){
	
						}catch(IOException e){
							System.err.println("Writting to recordLog failed");
						}finally{
	
						}				
						
					}// end of if (Character.isUpperCase(sentence.codePointAt(0))){
		  		}// end of if (sentence.split(" ").length>2){
	
			}// end of for (int end = iterator.next();...
			
			
	
			carrier.setData(tmpTS);
		}
*/
		return carrier;
	}

	

 
	public void addStopWords (String[] words)
	{
		for (int i = 0; i < words.length; i++)
			stoplist.add (words[i]);
	}
	private String[] fileToStringArray (File f, String encoding)
	{
		ArrayList<String> wordarray = new ArrayList<String>();

		try {

			BufferedReader input = null;
			if (encoding == null) {
				input = new BufferedReader (new FileReader (f));
			}
			else {
				input = new BufferedReader( new InputStreamReader( new FileInputStream(f), encoding ));
			}
			String line;

			while (( line = input.readLine()) != null) {
				String[] words = line.split ("\\s+");
				for (int i = 0; i < words.length; i++)
					wordarray.add (words[i]);
			}

		} catch (IOException e) {
			throw new IllegalArgumentException("Trouble reading file "+f);
		}
		return (String[]) wordarray.toArray(new String[]{});
	}		

	// Serialization 
	
	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 0;
	
	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt (CURRENT_SERIAL_VERSION);
		out.writeInt (gramSizes.length);
		for (int i = 0; i < gramSizes.length; i++)
			out.writeInt (gramSizes[i]);
	}
	
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt ();
		int size = in.readInt();
		gramSizes = new int[size];
		for (int i = 0; i < size; i++)
			gramSizes[i] = in.readInt();
	}
	static final String[] stopwords =
	{
		",",
		".",
		":",
		";",
		"a",
		"...",
		"able",
		"about",
		"above",
		"according",
		"accordingly",
		"across",
		"actually",
		"after",
		"afterwards",
		"again",
		"against",
		"algorithm",
		"all",
		"allow",
		"allows",
		"almost",
		"alone",
		"along",
		"already",
		"also",
		"although",
		"always",
		"am",
		"among",
		"amongst",
		"an",
		"another",
		"any",
		"anybody",
		"anyhow",
		"anyone",
		"anything",
		"anyway",
		"anyways",
		"anywhere",
		"apart",
		"appear",
		"appreciate",
		"appropriate",
		"are",
		"around",
		"art",
		"as",
		"aside",
		"ask",
		"asking",
		"associated",
		"assumption",
		"at",
		"available",
		"away",
		"awfully",
		"b",
		"be",
		"became",
		"because",
		"become",
		"becomes",
		"becoming",
		"been",
		"before",
		"beforehand",
		"behind",
		"being",
		"believe",
		"below",
		"beside",
		"besides",
		"best",
		"better",
		"between",
		"beyond",
		"billion",
		"both",
		"brief",
		"but",
		"by",
		"c",
		"came",
		"can",
		"cannot",
		"cant",
		"cause",
		"causes",
		"certain",
		"certainly",
		"changes",
		"China",
		"clearly",
		"co",
		"column",
		"com",
		"come",
		"comes",
		"computer",
		"concerning",
		"consequently",
		"consider",
		"considering",
		"contain",
		"containing",
		"contains",
		"corresponding",
		"could",
		"course",
		"creep",
		"currently",
		"d",
		"definitely",
		"described",
		"despite",
		"did",
		"different",
		"do",
		"does",
		"doing",
		"done",
		"down",
		"downwards",
		"during",
		"e",
		"each",
		"edu",
		"eg",
		"eight",
		"either",
		"else",
		"elsewhere",
		"enough",
		"entirely",
		"especially",
		"et",
		"etc",
		"even",
		"ever",
		"every",
		"everybody",
		"everyone",
		"everything",
		"everywhere",
		"ex",
		"exactly",
		"example",
		"except",
		"f",
		"face",
		"far",
		"few",
		"fifth",
		"first",
		"five",
		"followed",
		"following",
		"follows",
		"for",
		"former",
		"formerly",
		"forth",
		"four",
		"from",
		"further",
		"furthermore",
		"g",
		"get",
		"gets",
		"getting",
		"given",
		"gives",
		"go",
		"goes",
		"going",
		"gone",
		"got",
		"gotten",
		"greetings",
		"h",
		"had",
		"happens",
		"hardly",
		"has",
		"have",
		"having",
		"he",
		"hello",
		"help",
		"hence",
		"her",
		"here",
		"hereafter",
		"hereby",
		"herein",
		"hereupon",
		"hers",
		"herself",
		"hi",
		"him",
		"himself",
		"his",
		"hither",
		"hopefully",
		"how",
		"howbeit",
		"however",
		"i",
		"ie",
		"if",
		"ignored",
		"image",
		"immediate",
		"in",
		"inasmuch",
		"inc",
		"indeed",
		"indicate",
		"indicated",
		"indicates",
		"inner",
		"insofar",
		"instead",
		"into",
		"internet",
		"inward",
		"is",
		"it",
		"its",
		"itself",
		"j",
		"just",
		"k",
		"keep",
		"keeps",
		"kept",
		"know",
		"knows",
		"known",
		"l",
		"last",
		"lately",
		"later",
		"latter",
		"latterly",
		"least",
		"less",
		"lest",
		"let",
		"like",
		"liked",
		"likely",
		"little",
		"look",
		"looking",
		"looks",
		"ltd",
		"m",
		"mag",
		"mainly",
		"many",
		"may",
		"maybe",
		"me",
		"mean",
		"meanwhile",
		"merely",
		"method",
		"mi",
		"might",
		"million",
		"more",
		"moreover",
		"most",
		"mostly",
		"much",
		"must",
		"my",
		"myself",
		"n",
		"name",
		"namely",
		"nd",
		"near",
		"nearly",
		"necessary",
		"need",
		"needs",
		"neither",
		"never",
		"nevertheless",
		"new",
		"next",
		"next step",
		"nine",
		"no",
		"nobody",
		"non",
		"none",
		"noone",
		"nor",
		"normally",
		"not",
		"nothing",
		"novel",
		"now",
		"nowhere",
		"o",
		"obviously",
		"off",
		"often",
		"oh",
		"ok",
		"okay",
		"old",
		"on",
		"once",
		"one",
		"ones",
		"only",
		"onto",
		"or",
		"other",
		"others",
		"otherwise",
		"ought",
		"our",
		"ours",
		"ourselves",
		"out",
		"outside",
		"over",
		"overall",
		"own",
		"p",
		"particular",
		"particularly",
		"per",
		"perhaps",
		"pho",
		"placed",
		"please",
		"plus",
		"possible",
		"presumably",
		"probably",
		"provides",
		"q",
		"que",
		"quite",
		"qv",
		"r",
		"rather",
		"rd",
		"re",
		"really",
		"reasonably",
		"regarding",
		"regardless",
		"regards",
		"relationship",
		"relatively",
		"research",
		"respectively",
		"right",
		"row",
		"s",
		"said",
		"same",
		"saw",
		"say",
		"saying",
		"says",
		"second",
		"secondly",
		"section",
		"see",
		"seeing",
		"seem",
		"seemed",
		"seeming",
		"seems",
		"seen",
		"self",
		"selves",
		"sensible",
		"sent",
		"serious",
		"seriously",
		"seven",
		"several",
		"shall",
		"she",
		"should",
		"since",
		"six",
		"size",
		"so",
		"some",
		"somebody",
		"somehow",
		"someone",
		"something",
		"sometime",
		"sometimes",
		"somewhat",
		"somewhere",
		"soon",
		"sorry",
		"specified",
		"specify",
		"specifying",
		"still",
		"sub",
		"such",
		"sup",
		"sure",
		"t",
		"table",
		"take",
		"taken",
		"tell",
		"term",
		"tends",
		"technology",
		"time",
		"th",
		"the",
		"than",
		"thank",
		"thanks",
		"thanx",
		"that",
		"thats",
		"the",
		"their",
		"theirs",
		"them",
		"themselves",
		"then",
		"thence",
		"there",
		"thereafter",
		"thereby",
		"therefore",
		"therein",
		"theres",
		"thereupon",
		"these",
		"they",
		"think",
		"third",
		"this",
		"thorough",
		"thoroughly",
		"those",
		"though",
		"three",
		"through",
		"throughout",
		"thru",
		"thus",
		"to",
		"together",
		"too",
		"took",
		"toward",
		"towards",
		"trend",
		"tried",
		"tries",
		"truly",
		"try",
		"trying",
		"twice",
		"two",
		"u",
		"un",
		"under",
		"unfortunately",
		"unless",
		"unlikely",
		"until",
		"unto",
		"up",
		"upon",
		"us",
		"use",
		"used",
		"useful",
		"uses",
		"using",
		"usually",
		"uucp",
		"v",
		"value",
		"various",
		"very",
		"via",
		"viz",
		"vs",
		"w",
		"want",
		"wants",
		"was",
		"way",
		"we",
		"welcome",
		"well",
		"went",
		"were",
		"what",
		"whatever",
		"when",
		"whence",
		"whenever",
		"where",
		"whereafter",
		"whereas",
		"whereby",
		"wherein",
		"whereupon",
		"wherever",
		"whether",
		"which",
		"while",
		"whither",
		"who",
		"whoever",
		"whole",
		"whom",
		"whose",
		"why",
		"will",
		"willing",
		"wish",
		"with",
		"within",
		"without",
		"wonder",
		"would",
		"would",
		"x",
		"y",
		"yes",
		"yet",
		"you",
		"your",
		"yours",
		"yourself",
		"yourselves",
		"z",
		"zero",
		// stop words for paper abstracts
		"abstract",
		"paper",
		"presents",
		"discuss",
		"discusses",
		"conclude",
		"concludes",
		"based",
		"approach",
		"work",
		"Actinium",
		"Aluminum", 
		"Americium",
		"Antimony", 
		"Argon", 
		"Arsenic",
		"Astatine", 
		"Barium", 
		"Berkelium", 
		"Beryllium", 
		"Bismuth", 
		"Bohrium ",
		"Boron", 
		"Bromine", 
		"Cadmium", 
		"Calcium", 
		"Californium", 
		"Carbon", 
		"Cerium", 
		"Cesium", 
		"Chlorine", 
		"Chromium", 
		"Chromium", 
		"Chromium ",
		"Cobalt", 
		"Copper", 
		"cu",
		"Curium ",
		"Darmstadtium", 
		"Dubnium",
		"Dysprosium", 
		"Einsteinium", 
		"Erbium", 
		"Europium", 
		"Fermium", 
		"Fluorine", 
		"Francium ",
		"Gadolinium", 
		"Gallium", 
		"Germanium ",
		"Gold", 
		"Hafnium", 
		"Hassium",
		"Helium", 
		"Holmium", 
		"Hydrogen", 
		"Indium", 
		"Iodine", 
		"Iridium", 
		"Iron", 
		"Krypton", 
		"Lanthanum", 
		"Lawrencium", 
		"Lead", 
		"Lithium", 
		"Lutetium", 
		"Magnesium", 
		"Manganese ",
		"Meitnerium", 
		"Mendelevium", 
		"Mercury", 
		"Molybdenum ",
		"Neodymium", 
		"Neon", 
		"Neptunium", 
		"Nickel", 
		"Niobium", 
		"Nitrogen ",
		"Nobelium", 
		"Osmium ",
		"Oxygen", 
		"Palladium", 
		"Phosphorus", 
		"Platinum", 
		"Plutonium",
		"Polonium",
		"Potassium", 
		"Praseodymium", 
		"Promethium", 
		"Protactinium", 
		"Radium", 
		"Radon", 
		"Rhenium", 
		"Rhodium", 
		"Rubidium", 
		"Ruthenium", 
		"Rutherfordium", 
		"Samarium", 
		"Scandium", 
		"Seaborgium", 
		"Selenium", 
		// "Silicon", 
		"Silver", 
		"Ag",
		"Sodium", 
		"Strontium", 
		"Sulfur", 
		"Tantalum", 
		"Technetium", 
		"Tellurium", 
		"Terbium", 
		"Thallium", 
		"Thorium", 
		"Thulium", 
		"Tin", 
		"Titanium", 
		"Tungsten", 
		"Ununbium", 
		"Ununhexium", 
		"Ununoctium",
		"Ununpentium", 
		"Ununquadium", 
		"Ununseptium", 
		"Ununtrium",
		"Ununium", 
		"Uranium", 
		"Vanadium", 
		"Xenon",
		"Ytterbium", 
		"Yttrium", 
		"Zinc", 
		"Zirconium",
		"See also",
		"References",
		"External link",
		"Official website",
		"History and origin"		
	};
}
