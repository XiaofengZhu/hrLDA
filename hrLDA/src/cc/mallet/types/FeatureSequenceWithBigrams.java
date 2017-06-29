package cc.mallet.types;


import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import cc.mallet.pipe.TokenSequenceRemoveStopwords;

/** A FeatureSequence with a parallel record of bigrams, kept in a separate dictionary
 */

public class FeatureSequenceWithBigrams extends FeatureSequence
{
	public final static String deletionMark = "NextTokenDeleted";
	Alphabet biDictionary;
	int[] biFeatures;
	int[] biWeights;
	int[] biChunkIDs;
	int[] biSentenceIDs;

	public FeatureSequenceWithBigrams (Alphabet dict, Alphabet bigramDictionary, TokenSequence ts)
	{
		super (dict, ts.size());
		int len = ts.size();
		this.biDictionary = bigramDictionary;
		this.biFeatures = new int[len];
		this.biWeights = new int [len];
		this.biChunkIDs = new int [len];
		this.biSentenceIDs = new int [len];
		
		Token t, pt = null;
		for (int i = 0; i < len; i++) {
			t = ts.get(i);
//			super.add(t.getText());
			super.add(t.getText(), t.getWeight(), t.getChunkID(), t.getSentenceID());
//			System.out.println("THIS IS A TEST!!!!! "+t.getText()+ t.getWeight()+ t.getChunkID()+ t.getSentenceID());
			this.weights [i] = t.getWeight();
			this.chunkIDs [i] = t.getChunkID();
			this.sentenceIDs [i] = t.getSentenceID();			
			if (pt != null && pt.getProperty(deletionMark) == null)
				biFeatures[i] = biDictionary == null ? 0 : biDictionary.lookupIndex(pt.getText()+"_"+t.getText(), true);
			else
				biFeatures[i] = -1;
			pt = t;
		}
	}

	public Alphabet getBiAlphabet ()	{	return biDictionary; }

	public final int getBiIndexAtPosition (int pos)
	{
		return biFeatures[pos];
	}
	public final double getBiWeightAtPosition (int pos)
	{
		return biWeights[pos];
	}
	public final int getBiChunkIDAtPosition (int pos)
	{
		return biChunkIDs[pos];
	}
	public final int getBiSentenceIDAtPosition (int pos)
	{
		return biSentenceIDs[pos];
	}
	public Object getObjectAtPosition (int pos)
	{
		return biFeatures[pos] == -1 ? null : (biDictionary == null ? null : biDictionary.lookupObject (biFeatures[pos]));
	}

	// Serialization

	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 0;
	private static final int NULL_INTEGER = -1;

	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt (CURRENT_SERIAL_VERSION);
		out.writeObject (biDictionary);
		out.writeInt (biFeatures.length);
		for (int i = 0; i < biFeatures.length; i++)
			out.writeInt (biFeatures[i]);
	}

	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt ();
		biDictionary = (Alphabet) in.readObject ();
		int featuresLength = in.readInt();
		biFeatures = new int[featuresLength];
		for (int i = 0; i < featuresLength; i++)
			biFeatures[i] = in.readInt ();
	}


}
