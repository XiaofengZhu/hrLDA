package cc.mallet.types;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 *  A simple {@link Sequence} implementation where all of the
 *  elements must be Labels.   Provides a convenient type-safe accessor {@link #getLabels}.
 *  Instances of LabelsSequence are immutable.
 *
 */
public class LabelsSequence implements Sequence, AlphabetCarrying, Serializable
{
	Labels[] seq;
	
  /**
	 *  Create a LabelsSequence from an array.  The array is shallow-copied.
	 */
	public LabelsSequence (Labels[] seq)
	{
		for (int i = 0; i < seq.length-1; i++)
			if (!Alphabet.alphabetsMatch(seq[i], seq[i+1])) 
				throw new IllegalArgumentException ("Alphabets do not match");
		this.seq = new Labels[seq.length];
		System.arraycopy (seq, 0, this.seq, 0, seq.length);
	}

  public LabelsSequence (LabelSequence seq)
  {
    this.seq = new Labels[seq.size()];
    for (int i = 0; i < seq.length; i++) {
      this.seq[i] = new Labels (new Label[] { seq.getLabelAtPosition (i) });
    }
  }

	public Alphabet getAlphabet () { return seq[0].getAlphabet(); }
	public Alphabet[] getAlphabets () { return seq[0].getAlphabets(); }

  public int size () { return seq.length; }

	public Object get (int i) { return seq[i]; }

	public Labels getLabels (int i) { return seq[i]; }

	public String toString ()
	{
		String ret = "LabelsSequence:\n";
		for (int i = 0; i < seq.length; i++) {
			ret += i+": ";
			ret += seq[i].toString();
			ret += "\n";
		}
		return ret;
	}

  // Serialization

  private static final long serialVersionUID = 1;
  private static final int CURRENT_SERIAL_VERSION = 0;

  private void writeObject (ObjectOutputStream out) throws IOException {
    out.writeInt(CURRENT_SERIAL_VERSION);
    out.defaultWriteObject ();
  }

  private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
    int version = in.readInt ();
    in.defaultReadObject ();
  }


}
