package cc.mallet.pipe;


import java.io.*;

import cc.mallet.types.*;

public class TokenSequence2FeatureSequenceWithBigrams extends Pipe
{
	Alphabet biDictionary;

	public TokenSequence2FeatureSequenceWithBigrams (Alphabet dataDict, Alphabet bigramAlphabet)
	{
		super (dataDict, null);
		biDictionary = bigramAlphabet;
	}

	public TokenSequence2FeatureSequenceWithBigrams (Alphabet dataDict)
	{
		super (dataDict, null);
		biDictionary = new Alphabet();
	}

	public TokenSequence2FeatureSequenceWithBigrams ()
	{
		super(new Alphabet(), null);
		biDictionary = new Alphabet();
	}

	public Alphabet getBigramAlphabet ()
	{
		return biDictionary;
	}

	public Instance pipe (Instance carrier)
	{
		TokenSequence ts = (TokenSequence) carrier.getData();
		FeatureSequence ret = new FeatureSequenceWithBigrams (getDataAlphabet(), biDictionary, ts);
		carrier.setData(ret);
		return carrier;
	}

	// Serialization

	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 0;

	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt (CURRENT_SERIAL_VERSION);
		out.writeObject(biDictionary);
	}

	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt ();
		biDictionary = (Alphabet) in.readObject();
	}



}
