package cc.mallet.pipe;

import java.io.*;
import java.util.HashSet;
import java.util.ArrayList;
import java.io.*;
import java.text.Normalizer;

import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import cc.mallet.types.FeatureSequenceWithBigrams;

import cc.mallet.util.MySqlDB;

import rita.wordnet.RiWordnet;


import cc.mallet.util.JavaOllieWrapper;

import java.net.MalformedURLException;
import java.text.BreakIterator;
import java.util.Locale;

import edu.knowitall.ollie.Ollie;
import edu.knowitall.ollie.OllieExtraction;
import edu.knowitall.ollie.OllieExtractionInstance;
import edu.knowitall.ollie.confidence.OllieConfidenceFunction;
import edu.knowitall.tool.conf.impl.LogisticRegression;
import edu.knowitall.tool.parse.MaltParser;
import edu.knowitall.tool.parse.graph.DependencyGraph;

import cc.mallet.util.LookupService;

import org.modeshape.common.text.Inflector;

public class TokenSequenceNRelations extends Pipe implements Serializable
{
	int [] gramSizes = null;

	HashSet<String> stoplist = null;
	// Would pass in a PApplet normally, but we don't need to here
	RiWordnet wordnet = new RiWordnet(null);
	String end="";
	MySqlDB mySqlDB;
	Inflector inflector;
	JavaOllieWrapper ollieWrapper;	
	LookupService lookupService;

	private HashSet<String> newDefaultStopList ()
	{
		HashSet<String> sl = new HashSet<String>();
		for (int i = 0; i < stopwords.length; i++)
			sl.add (stopwords[i]);
		return sl;
	}
    
	public TokenSequenceNRelations (int [] sizes, String end) throws Exception
	{
		this.gramSizes = sizes;
		stoplist = newDefaultStopList();
		this.end = end;
		this.inflector = new Inflector(); 
		this.ollieWrapper = new JavaOllieWrapper();
		this.lookupService = new LookupService("c4fe4d42-2c7e-471d-859f-d412aee7b432","bing-body/2013-12/5","http://weblm.research.microsoft.com/rest.svc/");
	}

	public TokenSequenceNRelations (File stoplistFile, String encoding, int [] sizes, boolean includeDefault, String end, String databaseName,String userName,String password, String tableName ) throws Exception{
		this.gramSizes = sizes;
		if (! includeDefault) { stoplist = new HashSet<String>(); }
		else { stoplist = newDefaultStopList(); }

		addStopWords (fileToStringArray(stoplistFile, encoding));
		this.end = end;
		this.mySqlDB= new MySqlDB(databaseName, userName, password, tableName, null);
		mySqlDB.createTable();
		this.inflector = new Inflector(); 
		this.ollieWrapper = new JavaOllieWrapper();
		this.lookupService = new LookupService("c4fe4d42-2c7e-471d-859f-d412aee7b432","bing-body/2013-12/5","http://weblm.research.microsoft.com/rest.svc/");
	}

	public Instance pipe (Instance carrier)
	{
		String newTerm = null;
		TokenSequence tmpTS = new TokenSequence();
		String source = (String)carrier.getData();
		// Looks on the classpath for the default model files.
   
		// ollieWrapper = new JavaOllieWrapper();
        source = source.replaceAll("\\s+", " ");
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);

		iterator.setText(source);
		int start = iterator.first();
		LogisticRegression confFunc;
		Iterable<OllieExtractionInstance> extrs;
		for (int endP = iterator.next();
		    endP != BreakIterator.DONE;
		    start = endP, endP = iterator.next()) {
			String sentence = source.substring(start,endP);
			if (sentence.length()>5){
				sentence = sentence.replaceAll("(?u)null", "");
		        extrs = ollieWrapper.extract(sentence);
		        confFunc = OllieConfidenceFunction.loadDefaultClassifier();
		        // print the extractions.
		        for (OllieExtractionInstance inst : extrs) {
		            double conf = confFunc.getConf(inst);
		            OllieExtraction extr = inst.extr();

		            String s = (extr.arg1().text()+"").trim();
		            String s2 = (extr.arg2().text()+"").trim();
		            String relation = (extr.rel().text()+"").trim();
		            int coutnoun =0;
		            s = s.replaceAll("[^\\p{L}\\s*\\-\\_]", "");
		            double score = 0.0;
		            double tempScore = 0.0;

		            if (s.length()>2 && s2.length()>2 && relation.length()>1 &&conf >0.6){
		            	String [] strings = s.split(" ");
		            	StringBuffer stringBuffer = new StringBuffer();
		            	String tempString="";
		            	boolean isnoun = false;

			            // System.out.println("Arg1=" + s);
			            // System.out.println("Rel=" + relation);
			            // System.out.println("Arg2=" + s2);
			            // System.out.println("Conf=" + conf);  
			            // System.out.println();	            	
		            	char[] chars = new char[2];// store the first letter of s
		            	for (String string: strings){//break down the subject into an array of words

	            		if(string.matches("^[A-Za-z\\-]+$")){
							if(!isnoun){//find out the first noun	            		
			            		if(! stoplist.contains(string.toLowerCase())){
			            			
									String[] partsofspeechT = wordnet.getPos(string);
									if (partsofspeechT.length >0){
										for (String ps:partsofspeechT){
											if (ps.equals("n")){
												isnoun =true;
												stringBuffer.append(string);	
						            			try{
						            				tempScore = lookupService.getConditionalProbability(string);
						            				System.out.println("tempScore: "+tempScore+"the first noun is "+string);
						            			}catch (Exception e){
						            				System.out.println("No score for "+string);
						            			}
						            			tempString = stringBuffer.toString();
						            			score = tempScore;
						            			chars[0] = tempString.charAt(0);
												break;// break from for (String ps:partsofspeechT)									
											}// end of if (ps.equals("n"))
										}// end of for (String ps:partsofspeechT)								
									}// end of if (partsofspeechT.length >0)

								}// end of if(! stoplist.contains(string.toLowerCase())) 
							}else{// after find out the first noun, use MS ngram service to get a better long string
			            		if(! stoplist.contains(string.toLowerCase())){							
									if (string.length()>2){
											
											tempString += " "+string;											
											chars[1] = string.charAt(0);

					            			try{
					            				tempScore = lookupService.getConditionalProbability(tempString);
					            				// System.out.println("tempScore: "+tempScore + " tempString: "+tempString);
					            			}catch (Exception e){
					            				System.out.println("No score for "+tempString);
					            			}	
					            			if ((tempScore > score || ((chars[0]>='A'  &&  chars[0]<='Z')&&(chars[1]>='A'  &&  chars[1]<='Z'))) && !(stringBuffer.toString()).contains(string)){
					            				stringBuffer.append(" "+string);
					            				score = tempScore;
					            			}else break; // break from (String string: strings)       													
									}else break; //end of if (string.length()>2)
								}else break;
							}// end of 	if (!isnoun)
	         			}//if(string.matches("^[A-Za-z]+$")){
	            		}// end of for (String string: strings)
	            		s = stringBuffer.toString();


	            		if (s.length()>3){
		            		s = s.trim().replaceAll(" ","_");
		            		s = (chars[0]>='A'  &&  chars[0]<='Z')? (Normalizer.normalize(s, Normalizer.Form.NFD)).replaceAll("[^\\x00-\\x7F]", "") : inflector.singularize((Normalizer.normalize(s, Normalizer.Form.NFD)).replaceAll("[^\\x00-\\x7F]", ""));
		            		relation = (relation.length()>4)?inflector.singularize((Normalizer.normalize(relation, Normalizer.Form.NFD)).replaceAll("[^\\x00-\\x7F]", "")) : (Normalizer.normalize(relation, Normalizer.Form.NFD)).replaceAll("[^\\x00-\\x7F]", "");
		            		s2 = inflector.singularize((Normalizer.normalize(s2, Normalizer.Form.NFD)).replaceAll("[^\\x00-\\x7F]", ""));            			
	            			tmpTS.add(s);

				            s=s.replaceAll("_"," ");
				            relation = relation.replaceAll("_"," ");
				            s2 = s2.trim().replaceAll("_"," ");

				            mySqlDB.insertData(s, relation, s2, carrier.getName().toString(), null);
				            System.out.println("Arg1=" + s);
				            System.out.println("Rel=" + relation);
				            System.out.println("Arg2=" + s2);
				            System.out.println("Conf=" + conf);  
				            System.out.println("*********************************************");        		

		            	}// end of if (s.length()>3 && conf >0.6)            		
	           	            
		        	}// end of if (s.length()>2 && s2.length()>2 && relation.length()>1)	

				}// end of for (OllieExtractionInstance inst : extrs)
			}// end of if (sentence.length()>5)
	
		}// end of for (int end = iterator.next();...

		carrier.setData(tmpTS);

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
		"Zirconium"		
	};
}
