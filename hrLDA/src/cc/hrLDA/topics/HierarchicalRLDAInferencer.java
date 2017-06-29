package cc.hrLDA.topics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.io.*;
import java.sql.*;

import cc.hrLDA.topics.HierarchicalRLDA;
import cc.hrLDA.topics.HierarchicalRLDA.NCRPNode;
import cc.mallet.types.*;
import cc.mallet.util.Randoms;
import cc.mallet.util.MySqlDB;
import gnu.trove.*;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntProperty;

import org.modeshape.common.text.Inflector;

public class HierarchicalRLDAInferencer {
	
	private NCRPNode rootNode;
	int numWordsToDisplay=25;

	//global variable
    int numLevels;
    int numOfThreads;
    int numDocuments;
    int numTypes;
    int totalNodes;
    int duplicateCounts = 0;
    public int counter=0;
    HashSet<String> topicSet = new HashSet<String>();
	Inflector inflector = new Inflector();    

    double alpha; // smoothing on topic distributions
    double gamma; // "imaginary" customers at the next, as yet unused table
    double eta;   // smoothing on word distributions
    double etaSum;
    MySqlDB mySqlDB;


    HashMap <Integer, HashSet<NCRPNode>> golbalDocumentLeaves;
	
	int localLevels[];
	FeatureSequence fs;
	
	NCRPNode leaveNode;
    

	Randoms random;

	final private static String NS = "http://semiconductor-packaging/ontology/";
    final private static OntModel model = ModelFactory.createOntologyModel( OntModelSpec.RDFS_MEM );
	
	
	public HierarchicalRLDAInferencer(HierarchicalRLDA hLDA){
		this.setRootNode(hLDA.rootNode);
		this.numLevels = hLDA.numLevels;
		this.numOfThreads = hLDA.numOfThreads;
		this.numDocuments = hLDA.numDocuments;
		this.numTypes = hLDA.numTypes;
		this.mySqlDB = hLDA.mySqlDB;
		
		this.alpha = hLDA.alpha;
		this.gamma = hLDA.gamma;
		this.eta = hLDA.eta;
		this.etaSum = hLDA.etaSum;
		this.totalNodes =hLDA.totalNodes;

		this.golbalDocumentLeaves = hLDA.documentLeaves;
		this.random = hLDA.random;	
		topicSet.add("semiconductor-packaging");
	}

	 
	 public StringBuffer printTopicDistribution(HashSet<NCRPNode> leaves, int[][] levels){
			int[] tokenToNodeID;
			int token, nodeID;
			
			StringBuffer out = new StringBuffer();
			for (NCRPNode leaf : leaves){
				 NCRPNode node =leaf;
				 NCRPNode[] path = new NCRPNode[numLevels];
			     for (int level = numLevels - 1; level >= 0; level--) {
					 path[level] = node;
				     node = node.parent;
				}
				 
				double[] result = new double[totalNodes];
				 
				 
				int[] levelCounts = new int[numLevels];
			
				for (int level = 1; level < numLevels; level++){
					node = path[level];
					tokenToNodeID = levels[level];
						
					for (token = 0; token < tokenToNodeID.length; token++) {
						nodeID = tokenToNodeID[token];
							
						if (nodeID == path[level].nodeID){
								levelCounts[ level]++;
						}
					}
				}			 

				double sum =0.0;
				for (int level=0; level < numLevels; level++) {	 
					 sum+=(alpha + levelCounts[level]);
				}
				 
				for (int level=0; level < numLevels; level++) {	 
					 result[ path[level].nodeID ] = (double)(alpha + levelCounts[level]/sum) ;
				}
				 
				for(int i=0; i < numLevels; i++){
					 /*
				 out.append("Level:" + i + ",");
				 out.append("nodeID:" + path[i].nodeID + ",");
				 out.append("prob:" + (double)((alpha + levelCounts[i])/sum) + ",");
				 */
				 //out.append("Level:" + i + ",");
				 out.append("nodeID:" + path[i].nodeID + ",");
				 out.append((double)((alpha + levelCounts[i])/sum) + ",");
				 }			 
			 }
		 return(out);
	 }

	 
	 
	 public void printNode(NCRPNode node, int indent) {
		    //reset nodes counter
		    if(node.nodeID==0){
		    	counter=0;
		    }
			StringBuffer out = new StringBuffer();
			for (int i=0; i<indent; i++) {
				out.append("  ");
			}
			out.append("ID:" + node.nodeID + ",Topic:"+  "/ ");
			out.append(node.getAllTopWords());
			System.out.println(out);
			counter++;
		
			for (NCRPNode child: node.children) {
				printNode(child, indent + 1);
			}
	    }

	 public String cleanPhrase(String s){
		 s = s.replaceAll("\\s+", "_");
		 return s;
	 }
	 
	 public OntModel printRDFNode(NCRPNode node, OntClass ontparentClass) {
		 	
		 	int type =0;
			String topic ="";
			String [] topicWithType = new String [2];
			ArrayList <String []> result = new ArrayList <String []>();

			/*root node*/
			/*in latest algorithm, root does not contain anything except children list*/
		    if(ontparentClass == null){
		    	
		    	result = node.getAllTopHashWords();
		    	if (result != null){
			    	for (int i = 0; i < result.size(); i++){
			    		topicWithType = new String [2];
			    		topicWithType = result.get(i);
				    	//topicWithType = node.getTopHashWords(topicSet);//old topicWithType
				    	if (topicWithType != null){		  
				    		topic = cleanPhrase(topicWithType[0]);  
				    		//System.out.println("RECEIVING ... "+topic);
							type = Integer.parseInt(topicWithType[1].trim());
							
							/*a new topic*/
							if (!topic.equals("") && !topicSet.contains(topic)){
								topicSet.add(topic);	
								ontparentClass = model.createClass( NS+ topic);				

								
								
								try{
									ResultSet rs = mySqlDB.queryRelations(topic.replaceAll("_"," "));
									/*S -> O relation*/
									if (rs != null){
										while(rs.next()){
							               String relation=rs.getString(1);
							               String object=rs.getString(2);
							               String file_name=rs.getString(3);
							               if (!object.equals("")){
								               //column starts at 1
							            	   
//									               System.out.println("PARENT********************");
//									               System.out.println("TOPIC: "+topic);
//									               System.out.println("RELATION: "+relation);
//									               System.out.println("OBJECT: "+object);
//									               System.out.println("FILE_NAME: "+file_name);
//									               System.out.println("PARENT********************\n");
							            	   
								               OntProperty  property = model.createOntProperty( NS+ cleanPhrase(relation));
								               // bind relation with related object
								               ontparentClass.addProperty(property, cleanPhrase(object));
											}// end of if (!object.equals("")){
							          	}// end of while(rs.next()){								
									}// end of if (rs != null){
									
									
									/*O -> S OP relation*/
									ResultSet oRs = mySqlDB.queryObjectRelations(topic.replaceAll("_"," "));
																
									if (oRs != null){
										while(oRs.next()){
							               String relation="OP "+oRs.getString(1);
							               String object=oRs.getString(2);
							               String file_name=oRs.getString(3);
							               if (!object.equals("")){
								               //column starts at 1
							            	   
//									               System.out.println("PARENT********************");
//									               System.out.println("TOPIC: "+topic);
//									               System.out.println("OP RELATION: "+relation);
//									               System.out.println("OBJECT: "+object);
//									               System.out.println("FILE_NAME: "+file_name);
//									               System.out.println("PARENT********************\n");
							            	   
								               OntProperty  property = model.createOntProperty( NS+ cleanPhrase(relation));
								               // bind relation with related object
								               ontparentClass.addProperty(property, cleanPhrase(object));
											}else{
												   /*open print it out*/	
//									               System.out.println("PARENT********************");
//									               System.out.println("TOPIC: "+topic);
//									               System.out.println("THIS IS A TITLE");
//									               System.out.println("FILE_NAME: "+file_name);
//									               System.out.println("PARENT********************\n");		
												
											}// end of  if (!object.equals("")){
							          	}// end of while(oRs.next()){								
									}// end of if (oRs != null){
							
								}catch(Exception e){
									System.err.println("Parent node ---- Fail to query relation talbe!");
								}// end of try
								
								
								/*add doc paths*/
								ArrayList <String>arrayList = node.getDocs(type);
								for (String doc : arrayList){
									
									System.out.println(doc);
									ontparentClass.createIndividual( NS + doc.trim() );
								}//end of for (String doc : arrayList){
								
							}// end of if (!topic.equals("") && !topicSet.contains(topic)){
							
						}// end of if (topicWithType != null){	
			    	}// end of for (int i = 0; i < result.size(); i++){		    		
		    	}// end of if (result != null){
		    				

		    }// end of if(ontparentClass == null){ 
		    
		    
		    
		    /*for each child*/
			for (NCRPNode child: node.children) {		
						
		    	result = child.getAllTopHashWords();
		    	if (result != null){
			    	for (int j = 0; j < result.size(); j++){
			    		topicWithType = new String [2];
			    		topicWithType = result.get(j);
			    		
						if (topicWithType != null){
							topic = cleanPhrase(topicWithType[0]);
							//System.out.println("RECEIVING ... "+topic);
							type = Integer.parseInt(topicWithType[1].trim());
							
							/*a new topic*/
							if (!topic.equals("") && !topicSet.contains(topic)){
								topicSet.add(topic);
								OntClass ontChildClass = model.createClass( NS+ topic);			
								

								try{
									/*S -> O relation*/
									ResultSet rs = mySqlDB.queryRelations(topic.replaceAll("_"," "));
									if (rs != null){
										while(rs.next()){
							               String relation=rs.getString(1);
							               String object=rs.getString(2);
							               String file_name=rs.getString(3);
							               if (!object.equals("")){
								               //column starts at 1
							            	   
//									               System.out.println("CHILD********************");
//									               System.out.println("TOPIC: "+topic);
//									               System.out.println("RELATION: "+relation);
//									               System.out.println("OBJECT: "+object);
//									               System.out.println("FILE_NAME: "+file_name);
//									               System.out.println("CHILD********************\n");
							            	   
								               OntProperty  property = model.createOntProperty( NS+ cleanPhrase(relation));
								               // bind relation with related object
								               ontChildClass.addProperty(property, cleanPhrase(object));
											}// end of  if (!object.equals("")){
							          	}// end of while(rs.next()){
									}// end of if (rs != null){	

									/*O -> P OP relation*/
									ResultSet oRs = mySqlDB.queryObjectRelations(topic.replaceAll("_"," "));

									if (oRs != null){
										while(oRs.next()){
							               String relation="OP "+oRs.getString(1);
							               String object=oRs.getString(2);
							               String file_name=oRs.getString(3);
							               if (!object.equals("")){
								               //column starts at 1
							            	   
//								               System.out.println("CHILD********************");
//								               System.out.println("TOPIC: "+topic);
//								               System.out.println("OP RELATION: "+relation);
//								               System.out.println("OBJECT: "+object);
//								               System.out.println("FILE_NAME: "+file_name);
//								               System.out.println("CHILD********************\n");
							            	   
								               OntProperty  property = model.createOntProperty( NS+ cleanPhrase(relation));
								               // bind relation with related object
								               ontChildClass.addProperty(property, cleanPhrase(object));

							               }else{
							            	   /*only pint it out*/
//								               System.out.println("CHILD********************");
//								               System.out.println("TOPIC: "+topic);
//								               System.out.println("THIS IS A TITLE");
//								               System.out.println("FILE_NAME: "+file_name);
//								               System.out.println("CHILD********************\n");										
											}// end of if (!object.equals("")){
							          	}// end of while(oRs.next()){								
									}// end of if (oRs != null){												
								}catch(Exception e){
									System.err.println("Child node ---- Fail to query relation talbe!");
								}// end of try					

								/*add doc paths*/
								ArrayList <String>arrayList = child.getDocs(type);
								for (String doc : arrayList){
//									System.out.println(doc);
									ontChildClass.createIndividual( NS + doc.trim() );
								}// end of for (String doc : arrayList){

								/*current node is not rootnode*/
								if(node.nodeID!=0 && ontparentClass != null){
									ontChildClass.addSuperClass(ontparentClass);
								}
								
								printRDFNode(child, ontChildClass);					
							}else if (!topic.equals("")){
								// delete the child and upgrade its children to its location
								//But the two lines are wrong
								//node.addChildren(child.children);
								//node.remove(child);
								for (NCRPNode new_child: child.children)
									printRDFNode(new_child, ontparentClass);
								
							}// end of if (!topicSet.contains(topic)){
						}// end of if (topicWithType != null){
			    	}// end of for (int i = 0; i < result.size(); i++){		    		
		    	}// end of if (result != null)
		    	

			}// end of for (NCRPNode child: node.children) {
		
			return model;
	    }	    
	 
	  public void printNodeTofile(NCRPNode node, int indent, BufferedWriter writer) {
		 
		 //reset nodes counter
		    if(node.nodeID==0){
		    	counter=0;
		    }
			StringBuffer out = new StringBuffer();
			
			
			for (int i=0; i<indent; i++) {
				out.append("  ");
			}
			
            out.append("layer:"+indent+",");
			out.append("ID:" + node.nodeID + ",Tokens:"+ node.totalTokens + ",");
			out.append(node.getTopWords(numWordsToDisplay));
			out.append("\n");
			
			
			out.append(indent+",");
			out.append(node.nodeID + ","+ node.totalTokens + ",");
			out.append(node.getTopWords(numWordsToDisplay));
			out.append("\n");
			
			
			try {
				writer.write(out.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			counter++;
		
			for (NCRPNode child: node.children) {
				printNodeTofile(child, indent + 1, writer);
			}
	    }


	public NCRPNode getRootNode() {
		return rootNode;
	}

	public void setRootNode(NCRPNode rootNode) {
		this.rootNode = rootNode;
	}
}//end class

