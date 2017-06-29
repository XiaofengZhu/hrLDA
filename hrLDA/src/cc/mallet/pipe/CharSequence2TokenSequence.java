
package cc.mallet.pipe;


import java.io.*;
import java.net.URI;
import java.util.regex.Pattern;

import cc.mallet.extract.StringSpan;
import cc.mallet.extract.StringTokenization;
import cc.mallet.types.Instance;
import cc.mallet.types.SingleInstanceIterator;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;
import cc.mallet.util.CharSequenceLexer;
import cc.mallet.util.Lexer;

/**
 *  Pipe that tokenizes a character sequence.  Expects a CharSequence
 *   in the Instance data, and converts the sequence into a token
 *   sequence using the given regex or CharSequenceLexer.  
 *   (The regex / lexer should specify what counts as a token.)
 */
public class CharSequence2TokenSequence extends Pipe implements Serializable
{
	CharSequenceLexer lexer;
	
	public CharSequence2TokenSequence (CharSequenceLexer lexer)
	{
		this.lexer = lexer;
	}

	public CharSequence2TokenSequence (String regex)
	{
		this.lexer = new CharSequenceLexer (regex);
	}

	public CharSequence2TokenSequence (Pattern regex)
	{
		this.lexer = new CharSequenceLexer (regex);
	}

	public CharSequence2TokenSequence ()
	{
		this (new CharSequenceLexer());
	}

	public Instance pipe (Instance carrier)
	{
		String string = (String)carrier.getData();
		TokenSequence ts = new StringTokenization (string);
		ts.add (new StringSpan (string, 0, string.length()));
		// CharSequence string = (CharSequence) carrier.getData();
		// lexer.setCharSequence (string);
		// TokenSequence ts = new StringTokenization (string);		
		// while (lexer.hasNext()) {
		// 	lexer.next();
		// 	ts.add (new StringSpan (string, lexer.getStartOffset (), lexer.getEndOffset ()));
		// }
		// carrier.setData(ts);
		return carrier;
	}

	public static void main (String[] args)
	{
		try {
			for (int i = 0; i < args.length; i++) {
				Instance carrier = new Instance (new File(args[i]), null, null, null);
				SerialPipes p = new SerialPipes (new Pipe[] {
					new Input2CharSequenceFilter (),
					new CharSequence2TokenSequence(new CharSequenceLexer())});
				carrier = p.newIteratorFrom (new SingleInstanceIterator(carrier)).next();
				TokenSequence ts = (TokenSequence) carrier.getData();
				System.out.println ("===");
				System.out.println (args[i]);
				System.out.println (ts.toString());
			}
		} catch (Exception e) {
			System.out.println (e);
			e.printStackTrace();
		}
	}

	// Serialization 
	
	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 0;
	
	private void writeObject (ObjectOutputStream out) throws IOException {
		out.writeInt(CURRENT_SERIAL_VERSION);
		out.writeObject(lexer);
	}
	
	private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
		int version = in.readInt ();
		lexer = (CharSequenceLexer) in.readObject();
	}


	
}
