package cc.mallet.pipe;

import java.io.*;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class TokenSequence2FeatureSequence extends Pipe
{
	public TokenSequence2FeatureSequence (Alphabet dataDict)
	{
		super (dataDict, null);
	}

	public TokenSequence2FeatureSequence ()
	{
		super(new Alphabet(), null);
	}
	
	public Instance pipe (Instance carrier)
	{
		TokenSequence ts = (TokenSequence) carrier.getData();
		FeatureSequence ret =
			new FeatureSequence ((Alphabet)getDataAlphabet(), ts.size());
		for (int i = 0; i < ts.size(); i++) {
			ret.add (ts.get(i).getText());
		}
		carrier.setData(ret);
		return carrier;
	}

}
