package cc.mallet.types;

import cc.mallet.types.Label;

/**
	 A collection of labelings, either for a multi-label problem (all
	 labels are part of the same label dictionary), or a factorized
	 labeling, (each label is part of a different dictionary).
 */
public class Labelings implements AlphabetCarrying
{
	Labeling[] labels;
	
	public Labelings (Labeling[] labels)
	{
		for (int i = 0; i < labels.length-1; i++)
			if (!Alphabet.alphabetsMatch(labels[i], labels[i+1])) 
				throw new IllegalArgumentException ("Alphabets do not match");
		this.labels = new Labeling[labels.length];
		System.arraycopy (labels, 0, this.labels, 0, labels.length);
	}

	public Alphabet getAlphabet () { return labels[0].getAlphabet(); }
	public Alphabet[] getAlphabets () { return labels[0].getAlphabets(); }

	int size () { return labels.length; }

	Labeling get (int i) { return labels[i]; }
	
}
