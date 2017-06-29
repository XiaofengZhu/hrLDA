
package cc.mallet.util;

import java.util.Iterator;

public interface Lexer extends Iterator
{
	public int getStartOffset ();

	public int getEndOffset ();

	public String getTokenString ();


	// Iterator interface methods

	public boolean hasNext ();

	// Returns token text as a String
	public Object next ();

	public void remove ();

}
