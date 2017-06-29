package cc.mallet.types;
import java.io.*;

import cc.mallet.types.FeatureVectorSequence.Iterator;


public class LabelSequence extends FeatureSequence implements AlphabetCarrying, Serializable
{
	public LabelSequence (LabelAlphabet dict, int[] features)
	{
		super (dict, features);
	}
	
	public LabelSequence (LabelAlphabet dict, int capacity)
	{
		super (dict, capacity);
	}

	private static int[] getFeaturesFromLabels (Label[] labels)
	{
		int[] features = new int[labels.length];
		for (int i = 0; i < labels.length; i++)
			features[i] = labels[i].getIndex();
		return features;
	}
	
	public LabelSequence (Label[] labels)
	{
		super (labels[0].getLabelAlphabet(), getFeaturesFromLabels (labels));
	}

	public LabelSequence (Alphabet dict)
	{
		super (dict);
	}
	
	public LabelAlphabet getLabelAlphabet ()	{	return (LabelAlphabet) dictionary; }

	public Label getLabelAtPosition (int pos)
	{
		return ((LabelAlphabet)dictionary).lookupLabel (features[pos]);
	}

	public class Iterator implements java.util.Iterator {
		int pos;
		public Iterator () {
			pos = 0;
		}
		public Object next() {
			return getLabelAtPosition(pos++);
		}
		public int getIndex () {
			return pos;
		}
		public boolean hasNext() {
			return pos < features.length;
		}
		public void remove () {
			throw new UnsupportedOperationException ();
		}
	}
	
	public Iterator iterator ()
	{
		return new Iterator();
	}

	
	
	// ???
	//public Object get (int pos) {	return getLabelAtPosition (pos); }


	// Serialization 
	
	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 0;
	
	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt(CURRENT_SERIAL_VERSION);
	}
	
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt ();
	}



	
}
