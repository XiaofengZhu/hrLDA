package cc.mallet.types;

// Immutable

public interface Sequence<E>
{
	public int size ();
	public E get (int index);
}
