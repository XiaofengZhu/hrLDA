package cc.mallet.pipe;

import java.io.*;

import cc.mallet.types.*;
/**
 * A pipe that does nothing to the instance fields but which has side effects on the dictionary.
 */

public class Noop extends Pipe implements Serializable
{
	public Noop ()
	{
	}

	/** Pass through input without change, but force the creation of
			Alphabet's, so it can be shared by future DictionariedPipe's.

			You might want to use this before ParallelPipes where the previous
			pipes do not need dictionaries, but later steps in each parallel
			path do, and they all must share the same dictionary.
	*/

	public Noop (Alphabet dataDict,
							 Alphabet targetDict)
	{
		super (dataDict, targetDict);
	}
	
	public Instance pipe (Instance carrier)
	{
		return carrier;
	}

	// Serialization 
	
	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 0;
	
	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt (CURRENT_SERIAL_VERSION);
	}
	
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt ();
	}

	
}
