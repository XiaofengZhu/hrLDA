package cc.mallet.types;

public interface Matrix extends ConstantMatrix
{

	public void setValue (int[] indices, double value);
	public void setSingleValue (int i, double value);
	public void incrementSingleValue (int i, double delta);

  public void setValueAtLocation (int loc, double value);

	public void setAll (double v);
	public void set (ConstantMatrix m);
	public void setWithAddend (ConstantMatrix m, double addend);
	public void setWithFactor (ConstantMatrix m, double factor);
	public void plusEquals (ConstantMatrix m);
	public void plusEquals (ConstantMatrix m, double factor);
	public void equalsPlus (double factor, ConstantMatrix m);
	public void timesEquals (double factor);
	public void elementwiseTimesEquals (ConstantMatrix m);
	public void elementwiseTimesEquals (ConstantMatrix m, double factor);
	public void divideEquals (double factor);
	public void elementwiseDivideEquals (ConstantMatrix m);
	public void elementwiseDivideEquals (ConstantMatrix m, double factor);

	public double oneNormalize ();
	public double twoNormalize ();
	public double absNormalize();
	public double infinityNormalize ();
	
}
