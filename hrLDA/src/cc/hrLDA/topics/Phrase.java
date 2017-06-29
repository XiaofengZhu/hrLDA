package cc.hrLDA.topics;

public class Phrase {

	private int type;
	private double weight;
	private int chunkID;
	private int sentenceID;
	private int docID;
	

	public int getDocID() {
		return docID;
	}

	public void setDocID(int docID) {
		this.docID = docID;
	}

	public Phrase(int type, double weight, int chunkID, int sentenceID, int docID){
		this.type = type;
		this.weight = weight;
		this.chunkID = chunkID;
		this.sentenceID = sentenceID;
		this.docID = docID;
	}
	
	public int getType() {
		return type;
	}	
	public double getWeight() {
		return weight;
	}
	public int getChunkID() {
		return chunkID;
	}
	public int getSentenceID() {
		return sentenceID;
	}

}
