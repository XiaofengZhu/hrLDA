
package cc.mallet.pipe;

import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;


public class Target2Label extends Pipe
{
  private final static long serialVersionUID = -461155063551297878L; //-8390758647439705273L; 

  // gsc: adding constructor that has an option for the data alphabet as well,
  // this avoids the data alphabet getting set to null towards the end of a 
  // SerialPipes object because the data alphabet might have already been set
  public Target2Label (Alphabet dataAlphabet, LabelAlphabet labelAlphabet) {
    super(dataAlphabet, labelAlphabet);
  }
  
	public Target2Label ()
	{
	  this(null, new LabelAlphabet());
	}

	public Target2Label (LabelAlphabet labelAlphabet)
	{
	  this(null, labelAlphabet);
	}
	
	public Instance pipe (Instance carrier)
	{
		if (carrier.getTarget() != null) {
			if (carrier.getTarget() instanceof Label)
				throw new IllegalArgumentException ("Already a label.");
			LabelAlphabet ldict = (LabelAlphabet) getTargetAlphabet();
			carrier.setTarget(ldict.lookupLabel (carrier.getTarget()));
		}
		return carrier;
	}

}
