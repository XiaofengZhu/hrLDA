package cc.mallet.pipe;


import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import cc.mallet.types.FeatureSequenceWithBigrams;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import cc.mallet.util.CharSequenceLexer;

public class TokenSequenceRemoveNonAlpha extends Pipe {
	boolean markDeletions = false;
	public TokenSequenceRemoveNonAlpha (boolean markDeletions)
	{
		this.markDeletions = markDeletions;
	}

	public TokenSequenceRemoveNonAlpha ()
	{
		this (false);
	}

	public Instance pipe (Instance carrier)
	{
		TokenSequence ts = (TokenSequence) carrier.getData();
		// xxx This doesn't seem so efficient.  Perhaps have TokenSequence
		// use a LinkedList, and remove Tokens from it? -?
		// But a LinkedList implementation of TokenSequence would be quite inefficient -AKM
		TokenSequence ret = new TokenSequence ();
		Token prevToken = null;
		for (int i = 0; i < ts.size(); i++) {
			Token t = ts.get(i);
			String s = t.getText();
			if (CharSequenceLexer.LEX_ALPHA.matcher(s).matches()) {
				ret.add (t);
				prevToken = t;
			}	else if (markDeletions && prevToken != null)
				prevToken.setProperty (FeatureSequenceWithBigrams.deletionMark, t.getText());
		}
		carrier.setData(ret);
		return carrier;
	}

	// Serialization

	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 0;

	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt (CURRENT_SERIAL_VERSION);
		out.writeBoolean(markDeletions);
	}

	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt ();
		markDeletions = in.readBoolean();
	}

}
