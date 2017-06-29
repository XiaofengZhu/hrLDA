package cc.hrLDA.topics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.io.*;

import cc.hrLDA.topics.HierarchicalRLDA.NCRPNode;
import cc.mallet.types.*;
import cc.mallet.util.Randoms;
import cc.mallet.util.MySqlDB;
import gnu.trove.*;
import rita.wordnet.RiWordnet;

public class HierarchicalRLDA {

    InstanceList instances;
    InstanceList testing;

    NCRPNode rootNode;
    
    
    int numLevels;
    int numOfThreads;
    int numDocuments;
    int numTypes;

    double alpha; // smoothing on topic distributions
    double gamma; // "imaginary" customers at the next, as yet unused table
    double eta;   // smoothing on word distributions
    double etaSum;

    HashMap <Integer, HashSet<NCRPNode>> documentLeaves;
	RiWordnet wordnet = new RiWordnet(null);
	String end="";
	MySqlDB mySqlDB;
	int[][] tdm;

	int currentLevel = 0;
	int totalNodes = 0;// total num of NCRP nodes
    int numOfPhrases = 0;
    int numOfPseudoPhrases = 0;
    
	Randoms random;

	boolean showProgress = true;
	
	int displayTopicsInterval = 50;
	int numWordsToDisplay = 5;

	String stateFilePath = "hslda.state";
	String perplexityFilePath = "hSLDA.txt";	
	File perplexityFile;// 	
	BufferedWriter perplexityBufferedWriter;
	File stateFile;// 	
	BufferedWriter stateBufferedWriter;

    public HierarchicalRLDA () {
		alpha = 10.0;
		gamma = 1.0;
		eta = 0.1;
    }

	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	public void setGamma(double gamma) {
		this.gamma = gamma;
	}

	public void setEta(double eta) {
		this.eta = eta;
	}

	public void setStateFilePath(String stateFilePath) {
		this.stateFilePath = stateFilePath;
	}
	
    public void setPerplexityFilePath(String perplexityFilePath) {
		this.perplexityFilePath = perplexityFilePath;
	}

	public void setTopicDisplay(int interval, int words) {
		displayTopicsInterval = interval;
		numWordsToDisplay = words;
	}
	public void setEnd(String end){
		this.end = end;
	}
	/**  
	 *  This parameter determines whether the sampler outputs 
	 *   shows progress by outputting a character after every iteration.
	 */
	public void setProgressDisplay(boolean showProgress) {
		this.showProgress = showProgress;
	}
	public int getNumOfPhrases() {
		return numOfPhrases;
	}
    public void initialize(InstanceList instances, InstanceList testing,
						   int numLevels, int numOfThreads, Randoms random, 
						   String databaseName,String userName,String password, 
						   String tableName, String tempRelationTableName) {
		this.instances = instances;
		this.testing = testing;
		this.numLevels = numLevels;
		this.numOfThreads = numOfThreads;
		this.random = random;
		this.mySqlDB= new MySqlDB(databaseName, userName, password, tableName, tempRelationTableName);
		
		if (instances.size() ==0){
			System.err.println("Input data is empty");
			System.exit(2);
		}
		if (! (instances.get(0).getData() instanceof FeatureSequence)) {
			throw new IllegalArgumentException("Input must be a FeatureSequence, using the --"
					+ "feature-sequence option when impoting data, for example");
		}

		try {
			perplexityFile = new File (perplexityFilePath);
			stateFile = new File (stateFilePath);
			perplexityBufferedWriter = new BufferedWriter(new PrintWriter(perplexityFile));
			stateBufferedWriter = new BufferedWriter(new PrintWriter(stateFile)); 
		} catch (FileNotFoundException e) {
			System.err.println("Unable to create stateFile and perplexityFile");
		}
		
		numDocuments = instances.size();
		numTypes = instances.getDataAlphabet().size();
		if (numTypes <=0){
			System.err.println("no data for the seeds");
			System.exit(2);
		}
		
		// Initialize paths
		etaSum = eta * numTypes;
		
		rootNode = new NCRPNode(numTypes);// word types
		NCRPNode node = null;
		tdm = new int[numTypes][numDocuments];
		documentLeaves = new HashMap <Integer, HashSet<NCRPNode>> ();

		// Initialize and fill the topic pointer arrays for 
		//  every document. Set everything to the single path that 
		//  we added earlier.
		
		currentLevel = 0;// 1st level, but since we only assign table and haven't sample topics yet
		for (int doc=0; doc < numDocuments; doc++) {
            FeatureSequence fs = (FeatureSequence) instances.get(doc).getData();
            int seqLen = fs.getLength();
            
			System.out.println("doc: "+doc+": "+instances.get(doc).getName().toString());	
			
			HashSet <NCRPNode> currentDocLeaves = new HashSet <NCRPNode>();
			
			for (int token=0; token < seqLen; token++) {
				int type = fs.getIndexAtPosition(token);
				double weight = fs.getWeightAtPosition(token);
				int chunkID =fs.getChunkIDAtPosition (token);
				int sentenceID = fs.getSentenceIDAtPosition(token);
				Phrase phrase = new Phrase(type, weight, chunkID, sentenceID, doc);
				numOfPhrases++;
				numOfPseudoPhrases++;
				tdm[type][doc] ++;
				
				NCRPNode parentNode = rootNode;	
							
				node = parentNode.selectPath(phrase);
				node.totalTokens++;
				node.typeCounts[type]++;
				node.hashSet.add(phrase);
				
				//update doc leaves
				currentDocLeaves.add(node);
			}
						
			documentLeaves.put(doc, currentDocLeaves);

			System.out.println("END OF DOCUMENT "+doc+" ----------------------------------------------------------------");
			System.out.println();
			
		}
		
		/**print the initial topic tree*/
		/**
		System.out.println("rootNode nodeID: "+rootNode.nodeID
				+" rootNode nodeLevel: "+rootNode.level				
				+" rootNode children size: "+rootNode.children.size());
	

		printTree(rootNode);
		*/
		
	}
    
	public void estimate(int numIterations) {
		double [][] perplexityOfParentLevles = new double [numIterations+1][numLevels+1];
				
		while (currentLevel < numLevels){
			currentLevel++;
			System.out.println("currentLevel: "+currentLevel);
			
			// assign tables for level [2- numOfLevels+1]
			// assigning tale only occurs once for one level
			if (currentLevel > 1)
			assignTableForAllDocs();

			//sample tables
			sampleTableForAllDocs(numIterations);
			try {
				perplexityBufferedWriter.write("numOfPseudoPhrases, "+numOfPseudoPhrases+"\n");
			} catch (IOException e1) {
				System.err.println("Unable to write numOfPseudoPhrases");
			}
			
			//sample topics
			for (int iteration = 1; iteration <= numIterations; iteration++) {
				
				for (int doc=0; doc < numDocuments; doc++) {
					sampleTopicForOneDoc(doc);				
				}// end of for (int doc=0; doc < numDocuments; doc++) {
				
				// remove this last level restriction, in order to get the perplexity of the whole tree				
				if (currentLevel == numLevels){
					
					try {
						
						double perplexityOnLeafLevel = printPerplexityState(currentLevel);	
						double perplexity = perplexityOnLeafLevel;
						double [] perplexityOfParentInAllLevles = perplexityOfParentLevles [iteration];
						for (double pp : perplexityOfParentInAllLevles){
							perplexity += pp;
						}

						if (iteration < displayTopicsInterval || iteration % displayTopicsInterval == 0) {
							//? numOfPseudoPhrases or numOfPhrases
							System.out.println("perplexity: "+perplexity/numOfPseudoPhrases);
//							System.out.println("perplexity: "+perplexity/numOfPhrases);
							
							
							perplexityBufferedWriter.write(iteration+", "+perplexity/numOfPseudoPhrases+"\n");
//							perplexityBufferedWriter.write(iteration+", "+perplexity/numOfPhrases+"\n");
							perplexityBufferedWriter.flush();
							
						}// end of if (iteration % displayTopicsInterval == 0) {
					} catch (IOException e) {
						System.out.println("Unable to calculate the perplexity of leaf levle "
								+"or unable to write the current perplexity");
					}// end of try catch{}
					
				}else{					
					try {
						perplexityOfParentLevles [iteration][currentLevel] = 
								printPerplexityState(currentLevel);				
	
					} catch (IOException e) {
						System.out.println("Unable to calculate the perplexity of parent Levels ");
					}// end of try catch{}					
				}								
			}// end of for (int iteration = 1; iteration <= numIterations; iteration++) {
			
								
		}//while (currentLevel < numLevels){
		
		/**print the final state*/
		printNodes();
		System.out.println("rootNode nodeID: "+rootNode.nodeID
		+" rootNode nodeLevel: "+rootNode.level				
		+" rootNode children size: "+rootNode.children.size());		
		printTree(rootNode);
		
    }// end of estimate(int numIterations)
    
    public void assignTableForOneDoc (int doc){
    	HashSet <NCRPNode> oddCurrentDocLeaves = documentLeaves.get(doc);
    	HashSet <NCRPNode> currentDocLeaves = new HashSet <NCRPNode>();
    	
		for (NCRPNode leafnode : oddCurrentDocLeaves){
			assignTable(leafnode, currentDocLeaves, doc);
		}
		documentLeaves.put(doc, currentDocLeaves);
    }
    
    /*assign tables to from level [1: numOfLevels]*/
    public void assignTable(NCRPNode leafnode, HashSet <NCRPNode> currentDocLeaves, int doc){	
    	NCRPNode node = null;
    	int type;
		int topWordType = leafnode.getTopWordType();
		HashSet <Phrase> phraseHashSet = leafnode.hashSet;
		
		HashSet <Phrase> removephraseHashSet = new HashSet <Phrase>();
		for (Phrase phrase: phraseHashSet){
			
			if (phrase.getDocID() == doc){
				
				type = phrase.getType();    	
				
				if (type != topWordType){
					numOfPseudoPhrases++;
					node = leafnode.selectPath(phrase);
					node.totalTokens++;
					node.typeCounts[type]++;
					node.hashSet.add(phrase);
					
					removephraseHashSet.add(phrase);
					
					//update doc leaves
					currentDocLeaves.add(node); 					
				}
   
			}
		}// end of for (Phrase phrase: phraseHashSet){	
		
		for (Phrase phrase: removephraseHashSet){
			type = phrase.getType(); 
			leafnode.totalTokens--;
			leafnode.typeCounts[type]--;
			leafnode.hashSet.remove(phrase);
		}
		
		
    }// end of arrangeTable 
     
    
    public void sampleTableForAllDocs(int numIterations){
		boolean quit = false;
		boolean [] quitCurrentDoc = new boolean [numDocuments];
		for (int iteration = 1; iteration <= numIterations ||  quit; iteration++) {
			if (!quit){
				for (int doc=0; doc < numDocuments; doc++) {
					if (!quitCurrentDoc[doc]){
						HashSet <NCRPNode> oddCurrentDocLeaves = documentLeaves.get(doc);
						
						sampleTableForOneDoc(doc);	
						HashSet <NCRPNode> newCurrentDocLeaves = documentLeaves.get(doc);
						
						if (oddCurrentDocLeaves.size() == newCurrentDocLeaves.size()) {
							quitCurrentDoc[doc] = true;								
						}				
					}// end of if (!quitCurrentDoc[doc])
						
				}// end of for (int doc=0; doc < numDocuments; doc++) {
				
				quit = true;
				for (boolean q : quitCurrentDoc){quit = quit & q;}
				
			}else {
				System.out.println("QUIT FROM numIterations: " +iteration);
				break;
			}// end of if (!quit){
		}// end of for (int iteration = 1; iteration <= numIterations ||  quit; iteration++)
		System.out.println("QUIT VALUE: " +quit);   	
    }// end of sampleTableForAllDocs(int numIterations)

    public void assignTableForAllDocs(){
		for (int doc=0; doc < numDocuments; doc++) {
			assignTableForOneDoc(doc);
		}    	
    }// end of assignTableForAllDocs()  
    
    public void sampleTableForOneDoc (int doc){
    	HashSet <NCRPNode> oddCurrentDocLeaves = documentLeaves.get(doc);
    	HashSet <NCRPNode> currentDocLeaves = new HashSet <NCRPNode>();
    	
		for (NCRPNode leafnode : oddCurrentDocLeaves){
			sampleTable(leafnode, currentDocLeaves, doc);
		}
		documentLeaves.put(doc, currentDocLeaves);
    }
    
    public void sampleTable(NCRPNode leafnode, HashSet <NCRPNode> currentDocLeaves, int doc){	
    	NCRPNode node = null;
    	int type;
		HashSet <Phrase> phraseHashSet = leafnode.hashSet;
		HashMap <Phrase, NCRPNode> phraseToNode = new HashMap <Phrase, NCRPNode>();
		for (Phrase phrase: phraseHashSet){
			
			if (phrase.getDocID() == doc){
				phraseToNode.put(phrase, leafnode);  
			}
		}// end of for (Phrase phrase: phraseHashSet){
		
		for (Phrase phrase: phraseToNode.keySet()){
			node = phraseToNode.get(phrase);

			type = phrase.getType();

			node.hashSet.remove(phrase);			
			node.typeCounts[type]--;
			node.totalTokens--;			
			
			NCRPNode oldNode = node;
				
			node = leafnode.parent.selectPath(phrase);
			node.totalTokens++;
			node.typeCounts[type]++;
			node.hashSet.add(phrase);
				
			//update doc leaves
			currentDocLeaves.add(node); 
			
			if (oldNode.totalTokens == 0) oldNode.clean();

		}// end of for (Phrase phrase: phraseHashSet){		
		
    }// end of arrangeTable  
    
   
    
    public void sampleTopicForOneDoc(int doc) {
    	HashSet <NCRPNode> oddCurrentDocLeaves = documentLeaves.get(doc);    	
    	HashSet <NCRPNode> currentDocLeaves = new HashSet <NCRPNode>();
    	
		NCRPNode node;
		int type;
		double sum;
	
		HashMap <NCRPNode, Integer> leafCounts = new HashMap <NCRPNode, Integer>();
		HashMap <NCRPNode, Double> leafWeights = new HashMap <NCRPNode, Double>();
		
		HashMap <Phrase, NCRPNode> phraseToNode = new HashMap <Phrase, NCRPNode>();
		for (NCRPNode leafnode : oddCurrentDocLeaves){
			
			HashSet <Phrase> phraseHashSet = leafnode.hashSet;

			for (Phrase phrase: phraseHashSet){
				
				if (phrase.getDocID() == doc){
					// Initialize leaf counts
					phraseToNode.put(phrase, leafnode);
					
					if (! leafCounts.containsKey(leafnode)) {
						leafCounts.put(leafnode, 1);			
					}
					else {
						leafCounts.put(leafnode, leafCounts.get(leafnode) + 1);					
					}					
				}
				
			}//end of for (Phrase phrase: phraseArrayList){					
			
		}//end of for (NCRPNode leafnode : oddCurrentDocLeaves){
		
		// sum of all leaf weights; for normalization
		sum = 0.0;	
		// sampling...
		for (Phrase phrase: phraseToNode.keySet()){
			node = phraseToNode.get(phrase);

			type = phrase.getType();
			leafCounts.put(node, leafCounts.get(node) - 1);	

			node.hashSet.remove(phrase);			
			node.typeCounts[type]--;
			node.totalTokens--;	
			
			NCRPNode oldNode = node;
			
			// calculate the current leaf weight

			for (NCRPNode leafnode : node.parent.children){

					if ( leafCounts.get(leafnode)!= null && leafnode != null){
						double weightValue = (alpha + leafCounts.get(leafnode) * 
								(eta + leafnode.typeCounts[type]) /
								(etaSum + leafnode.totalTokens));
						leafWeights.put (leafnode, 
								weightValue);
						sum += leafWeights.get(leafnode);
					}

			}// end of for (NCRPNode leafnode : node.parent.children){
			
			node = (NCRPNode)random.nextDiscrete(leafWeights, sum);
			
			if (node != null){
				node.hashSet.add(phrase);
				node.typeCounts[type]++;
				node.totalTokens++;	
				
				if (! leafCounts.containsKey(node)) {
					leafCounts.put(node, 1);			
				}else {
					leafCounts.put(node, leafCounts.get(node) + 1);					
				}	
				
				if (!currentDocLeaves.contains(node)){
					currentDocLeaves.add(node);
				}
			}else{
				//never happen?
				oldNode.hashSet.add(phrase);
				oldNode.typeCounts[type]++;
				oldNode.totalTokens++;	
				
				if (! leafCounts.containsKey(oldNode)) {
					leafCounts.put(oldNode, 1);			
				}else {
					leafCounts.put(oldNode, leafCounts.get(node) + 1);					
				}
				
				if (!currentDocLeaves.contains(oldNode)){
					currentDocLeaves.add(oldNode);
				}
			}// end of if (node != null)
			
			if (oldNode.totalTokens ==0) {
				oldNode.clean();
			}
	
		}//end of for (NCRPNode leafnode : oddCurrentDocLeaves){
		
		documentLeaves.put(doc, currentDocLeaves);
	}//end of sampleTopics(int doc)

	/**
	 *  Write a text file describing the current sampling state. 
	 */
    public double printPerplexityState(int level) throws IOException {
    	
		int doc = 0;
		double sumPerplexity =0.0;
		
		for (Instance instance: instances) {
			FeatureSequence fs = (FeatureSequence) instance.getData();
			int seqLen = fs.getLength();
			
	    	HashSet <NCRPNode> oddCurrentDocLeaves = documentLeaves.get(doc);  
			int type;
			//seqLen ?sumCountsInLeaves, we need to think about it.
			double sumCountsInLeaves = 0.0;
		
			HashMap <NCRPNode, Integer> wordCountsInCurrentLeaf = new HashMap <NCRPNode, Integer>();
			
			for (NCRPNode leafnode : oddCurrentDocLeaves){
				HashSet <Phrase> phraseHashSet = leafnode.hashSet;
				
				for (Phrase phrase: phraseHashSet){
					
					if (phrase.getDocID() == doc){
						//seqLen ?sumCountsInLeaves, we need to think about it.
						sumCountsInLeaves++;
						
						if (! wordCountsInCurrentLeaf.containsKey(leafnode)) {
							wordCountsInCurrentLeaf.put(leafnode, 1);			
						}
						else {
							wordCountsInCurrentLeaf.put(leafnode, wordCountsInCurrentLeaf.get(leafnode) + 1);					
						}					
					}
					
				}//end of for (Phrase phrase: phraseArrayList){					
				
			}//end of for (NCRPNode leafnode : oddCurrentDocLeaves){
			
			for (NCRPNode leafnode : oddCurrentDocLeaves){

				if (wordCountsInCurrentLeaf.get(leafnode) != null){
//					System.out.println("wordCountsInCurrentLeaf.get(leafnode): "+wordCountsInCurrentLeaf.get(leafnode) 
//							+ " sumCountsInLeaves"+sumCountsInLeaves);					
					double topicWeightInCurrentDoc = (double)wordCountsInCurrentLeaf
							.get(leafnode)/sumCountsInLeaves; //seqLen ?sumCountsInLeaves, we need to think about it.
					HashSet <Phrase> phraseHashSet = leafnode.hashSet;
					
					for (Phrase phrase: phraseHashSet){
						
						if (phrase.getDocID() == doc){
							// Initialize leaf counts
							type = phrase.getType();
							
							if (level < numLevels-1){
								// change to calculate all words in parent levels though they will disappear in child levels
//								if (type == leafnode.getTopWordType()){
									double typeWeightInTopic = (double) leafnode.typeCounts[type]/ leafnode.totalTokens;
//									System.out.println("typeWeightInTopic: "+typeWeightInTopic 
//											+ " topicWeightInCurrentDoc: "+topicWeightInCurrentDoc);
									sumPerplexity += - Math.log(topicWeightInCurrentDoc * typeWeightInTopic);									
//								}else{}
							}else{
								double typeWeightInTopic = (double) leafnode.typeCounts[type]/ leafnode.totalTokens;
//								System.out.println(level+ " typeWeightInTopic: "+typeWeightInTopic 
//										+ " topicWeightInCurrentDoc: "+topicWeightInCurrentDoc);
								sumPerplexity += - Math.log(topicWeightInCurrentDoc * typeWeightInTopic);								
							}							

						}
						
					}//end of for (Phrase phrase: phraseArrayList){						
				}			
				
			}//end of for (NCRPNode leafnode : oddCurrentDocLeaves){
			doc++;
		}

		return sumPerplexity;
	}	    

    
	/**
	 *  Writes the current sampling state to the file specified in <code>stateFile</code>.
	 */
    public void printNodes() {
		printNode(rootNode, 0);
    }


    public void printNode(NCRPNode node, int indent) {
		StringBuffer out = new StringBuffer();
		for (int i=0; i<indent; i++) {
			out.append("  ");
		}
		out.append("totalTokens: "+node.totalTokens + "/" + " ");
		out.append(node.getAllTopWords());
	
		try {
			stateBufferedWriter.write(out.toString()+"\n");
			stateBufferedWriter.flush();
		} catch (IOException e) {
			System.err.println("unable to write current topic disctribution state");
		}
		
		for (NCRPNode child: node.children) {
			printNode(child, indent + 1);
		}
    }
    public void printTree (NCRPNode parentNode){

		for (NCRPNode node: parentNode.children){
			int tableIDAtSomeLevel = node.getTableID();

			System.out.println("nodeID: "+node.nodeID
					+" nodeLevel: "+node.level+ " tableIDAtSomeLevel: " +tableIDAtSomeLevel	
					+" parent nodeID: "+node.parent.nodeID
					+" children size: "+node.children.size()
					+" nodeTotalTokens: "+node.totalTokens					
					+" topWords "+node.getAllTopWords());
			printTree (node);			
		}	
    }

	/** 
	 *  This method is primarily for testing purposes. The {@link cc.mallet.topics.tui.HierarchicalLDATUI}
	 *   class has a more flexible interface for command-line use.
	 */
    public static void main (String[] args) {
		try {
			InstanceList instances = InstanceList.load(new File(args[0]));
			InstanceList testing = InstanceList.load(new File(args[1]));

			HierarchicalRLDA sampler = new HierarchicalRLDA();
			sampler.initialize(instances, testing, 5, 1, new Randoms(),args[2],args[3],args[4],args[5], args[6]);
			sampler.estimate(250);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    public class NCRPNode {
		//int customers;
		ArrayList<NCRPNode> children;
		NCRPNode parent;
		int level;
		int tableID;

		int totalTokens;
		int[] typeCounts;

		public HashSet <Phrase> hashSet;

		public int nodeID;

		public NCRPNode(NCRPNode parent, int dimensions, int level) {
			//this.customers = 0;
			this.parent = parent;
			this.children = new ArrayList<NCRPNode>();
			this.level = level;
   
			this.totalTokens = 0;
			this.typeCounts = new int[dimensions];
			
			this.hashSet = new HashSet<Phrase>();

			this.nodeID = totalNodes;
			totalNodes++;
		}
		

		public HashSet<Phrase> getHashSet() {
			return hashSet;
		}

		public void setHashSet(HashSet<Phrase> hashSet) {
			this.hashSet = hashSet;
		}

		public int getTableID() {
			return this.tableID;
		}

		public void setTableID(int tableID) {
			this.tableID = tableID;
		}


		public NCRPNode(int dimensions) {
			this(null, dimensions, 0);
		}

		public NCRPNode addChild() {
			NCRPNode node = new NCRPNode(this, typeCounts.length, level + 1);
			this.children.add(node);
			node.setTableID(this.children.size()-1);
			return node;
		}
		
		public boolean isLeaf() {
			return this.level == numLevels - 1;
		}

		public NCRPNode getNewLeaf() {
			NCRPNode node = this;
			for (int l=level; l<numLevels - 1; l++) {
				node = node.addChild();
			}
			return node;
		}

		public void clean() {
			NCRPNode node = this;
			node.parent.remove(node);
			NCRPNode parent = node.parent;
			
//			System.out.println("current child ID: "+node.nodeID);
			node = null;
//			for (NCRPNode n: parent.children){
//				System.out.print(n.nodeID+" ");
//			}
//			System.out.println();
			
		}

		public void remove(NCRPNode child) {
			this.children.remove(child);
//			System.out.print(this.nodeID + " remove "+child.nodeID+"  current child: ");
//			for (NCRPNode n: this.children){
//				System.out.print(n.nodeID+" ");
//			}
//			System.out.println();			
		}

		/**need fixing*/
		public NCRPNode selectExisting() {
			double[] weights = new double[children.size()];
	    
			int i = 0;
			for (NCRPNode child: children) {
				weights[i] = (double) child.totalTokens / (gamma + this.totalTokens);
				i++;
			}

			int choice = random.nextDiscrete(weights);
			return children.get(choice);
		}

		public double getMap(NCRPNode node, Phrase phrase){
			double probably = 0.0;
			HashSet <Phrase> phraseList = node.getHashSet();
			
			for (Phrase phraseInst : phraseList){
				double tempProbably = 0.0;
				if (phraseInst.getType() == phrase.getType()){
					probably = 1.0 - gamma;
					break;
				}else{
					if (phraseInst.getDocID() == phrase.getDocID()){
						if (phraseInst.getChunkID() == phrase.getChunkID()){// in the same chunk
							tempProbably =(double) (node.totalTokens - (1 - 1 / Math.abs(1+ gamma + phraseInst.getSentenceID() - phrase.getSentenceID()))) / 
									(gamma + this.totalTokens);
						}else{
							tempProbably =(double)(node.totalTokens - (1 - 1 / Math.abs(1+ gamma + phraseInst.getSentenceID() - phrase.getSentenceID()))) / 
									(gamma + (1 + Math.abs(phraseInst.getChunkID() - phrase.getChunkID())) * this.totalTokens);
						}
						probably = (tempProbably >probably) ? tempProbably: probably;
						
					}else
						probably = gamma;//distance = ---> MAX_VALUE					
				}
			}
			

			
			return probably;			
		}
		
		public NCRPNode selectPath(Phrase phrase) {
			double[] weights = new double[this.children.size() + 1];
		    
			weights[0] = gamma / (gamma + this.totalTokens);
			int i = 1;
			double sumWeight = weights[0];
			for (NCRPNode child: children) {
				weights[i] = getMap(child, phrase);
				// has been changed to find the closest word
				sumWeight += weights[i];
				i++;
			}
			
			int choice = random.nextDiscrete(weights, sumWeight);

			if (choice == 0) {
				return(addChild());
			}
			else {
				return this.children.get(choice - 1);
			}
		}

		public ArrayList <String []> getAllTopHashWords() {
			if(numTypes > 0){
				
				ArrayList <String []> result = new ArrayList <String []>();
				IDSorter[] sortedTypes = new IDSorter[numTypes];
				
				int notNullTypeCounts = 0;
				for (int type=0; type < numTypes; type++) {
					sortedTypes[type] = new IDSorter(type, this.typeCounts[type]);	
					if (this.typeCounts[type]>0){
						notNullTypeCounts++;
						}
				}
				
				if (notNullTypeCounts >0){
					
					
					Arrays.sort(sortedTypes);
				    
					Alphabet alphabet = instances.getDataAlphabet();
					
					for (int i=0; i<notNullTypeCounts; i++) {
						String [] topcWithType= new String [2];
						topcWithType[0] = alphabet.lookupObject(sortedTypes[i].getID())+"";
						topcWithType[1] = sortedTypes[i].getID()+"";
						//System.out.println("NEW PRINTING ... "+topcWithType[0]);
						result.add(topcWithType);
					}
					return result;					
				}else
					return null;

			}else{
				System.out.println("No enough topic words!");
				return null;
			}
		}
		
		
		public String[] getTopHashWords(HashSet<String> topiclist) {
			if(numTypes > 0){	
				String [] topcWithType= new String [2];
				IDSorter[] sortedTypes = new IDSorter[numTypes];
		    
				int notNullTypeCounts = 0;
				for (int type=0; type < numTypes; type++) {
					sortedTypes[type] = new IDSorter(type, this.typeCounts[type]);	
					if (this.typeCounts[type]>0){
						notNullTypeCounts++;
						}
				}
				if (notNullTypeCounts >0){
					Arrays.sort(sortedTypes);
		    
					Alphabet alphabet = instances.getDataAlphabet();
					String topic="";
					int i=0;
	
					do {
						topic = alphabet.lookupObject(sortedTypes[i].getID())+"";		
	
						i++;
					}while(topiclist.contains(topic) && (i < numTypes) && (i<100));	

					if (topiclist.contains(topic)){
						return null;
					}else{
						topcWithType[0] = topic;
						topcWithType[1] = sortedTypes[i-1].getID()+"";
						return topcWithType;
					}
					}else
						return null;					

			}else{
				System.out.println("No enough topic words on current level!");
				return null;
			}
		}	
		
		public int getTopWordType (){
			
			if(numTypes > 0){
				IDSorter[] sortedTypes = new IDSorter[numTypes];
		    
				int notNullTypeCounts = 0;
				for (int type=0; type < numTypes; type++) {
					sortedTypes[type] = new IDSorter(type, this.typeCounts[type]);	
					if (this.typeCounts[type]>0){
						notNullTypeCounts++;						
						}
				}
				if (notNullTypeCounts >0){
					Arrays.sort(sortedTypes);
		    
					return sortedTypes[0].getID();
				}else
					return 0;					
			}else{
				System.out.println("No enough topic words!");
				return 0;
			}	
		}
		
		public String[] getTopWords() {
			if(numTypes > 0){
				String [] topcWithType= new String [2];
				IDSorter[] sortedTypes = new IDSorter[numTypes];
		    
				int notNullTypeCounts = 0;
				for (int type=0; type < numTypes; type++) {
					sortedTypes[type] = new IDSorter(type, this.typeCounts[type]);	
					if (this.typeCounts[type]>0){
						notNullTypeCounts++;						
						}
				}
				if (notNullTypeCounts >0){
					Arrays.sort(sortedTypes);
		    
					Alphabet alphabet = instances.getDataAlphabet();
					StringBuffer out = new StringBuffer();
					out.append(alphabet.lookupObject(sortedTypes[0].getID()) + " ");
	
					topcWithType[0] = out.toString();
					topcWithType[1] = sortedTypes[0].getID()+"";
					return topcWithType;
				}else
					return null;					
			}else{
				System.out.println("No enough topic words!");
				return null;
			}			
			
		}
		public String getTopWords(int numWords) {
			if(numTypes > 0){
				IDSorter[] sortedTypes = new IDSorter[numTypes];
		    
				int notNullTypeCounts = 0;
				for (int type=0; type < numTypes; type++) {
					sortedTypes[type] = new IDSorter(type, this.typeCounts[type]);	
					if (this.typeCounts[type]>0){
						notNullTypeCounts++;
						}
				}
				
				if (notNullTypeCounts >0){
					Arrays.sort(sortedTypes);
		    
					Alphabet alphabet = instances.getDataAlphabet();
					StringBuffer out = new StringBuffer();
					int iteration = (numWords < notNullTypeCounts)? numWords : notNullTypeCounts;
	
					for (int i=0; i<iteration; i++) {
						out.append(alphabet.lookupObject(sortedTypes[i].getID()) + " ");
					}
					return out.toString();					
			}else
				return null;
			}else{
				System.out.println("No enough topic words!");
				return null;
			}
		}

		public String getAllTopWords() {
			if(numTypes > 0){
				StringBuffer out = new StringBuffer();
				IDSorter[] sortedTypes = new IDSorter[numTypes];
				
				int notNullTypeCounts = 0;
				for (int type=0; type < numTypes; type++) {
					sortedTypes[type] = new IDSorter(type, this.typeCounts[type]);	
					if (this.typeCounts[type]>0){
						notNullTypeCounts++;
						}
				}
				
				if (notNullTypeCounts >0){
					Arrays.sort(sortedTypes);
				    
					Alphabet alphabet = instances.getDataAlphabet();
					
					int iteration = notNullTypeCounts;
					for (int i=0; i<iteration; i++) {
						out.append(alphabet.lookupObject(sortedTypes[i].getID()) + " ");
					}
					return out.toString();					
				}else
					return null;

			}else{
				System.out.println("No enough topic words!");
				return null;
			}
		}
		
		public ArrayList<String> getDocs(int type) {
			ArrayList<String> arraylist= new ArrayList<String>();
			IDSorter[] sortedTypes = new IDSorter[numDocuments];
			for (int doc=0; doc < numDocuments; doc++) {
				sortedTypes[doc] = new IDSorter(doc, tdm[type][doc]);
			}
			Arrays.sort(sortedTypes);
			for (IDSorter sTypes : sortedTypes){
				if (tdm[type][sTypes.getID()]>0){
					arraylist.add(instances.get(sTypes.getID()).getName().toString());
				}
							}
			return arraylist;

		}		

    }
}
