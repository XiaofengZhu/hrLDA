package cc.mallet.pipe;

import java.io.*;

import cc.mallet.types.FeatureSequence;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;

// This class does not insist on getting its own Alphabet because it can rely on getting
// it from the FeatureSequence input.
/**
 * Convert the data field from a feature sequence to a feature vector.
 */
public class FeatureSequence2FeatureVector extends Pipe implements Serializable
{
	boolean binary;

	public FeatureSequence2FeatureVector (boolean binary)
	{
		this.binary = binary;
	}

	public FeatureSequence2FeatureVector ()
	{
		this (false);
	}
	
	
	public Instance pipe (Instance carrier)
	{
		FeatureSequence fs = (FeatureSequence) carrier.getData();
		carrier.setData(new FeatureVector (fs, binary));
		return carrier;
	}

	// Serialization 
	
	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 1;
	
	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt (CURRENT_SERIAL_VERSION);
		out.writeBoolean (binary);
	}
	
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt ();
		if (version > 0)
			binary = in.readBoolean();
	}
}
