package cc.mallet.types;

public class FeatureCounts extends RankedFeatureVector
{
	// increment by 1 for each instance that has the feature, ignoring the feature's value
	static boolean countInstances = true;
	
	private static double[] calcFeatureCounts (InstanceList ilist)
	{
		int numInstances = ilist.size();
		int numClasses = ilist.getTargetAlphabet().size();
		int numFeatures = ilist.getDataAlphabet().size();
		double[] counts = new double[numFeatures];
		double count;
		for (int i = 0; i < ilist.size(); i++) {
			Instance inst = ilist.get(i);
			if (!(inst.getData() instanceof FeatureVector))
				throw new IllegalArgumentException ("Currently only handles FeatureVector data");
			FeatureVector fv = (FeatureVector) inst.getData ();
			if (ilist.getInstanceWeight(i) == 0)
				continue;
			for (int j = 0; j < fv.numLocations(); j++) {
				if (countInstances)
					counts[fv.indexAtLocation(j)] += 1;
				else
					counts[fv.indexAtLocation(j)] += fv.valueAtLocation(j);
			}					
		}
		return counts;
	}

	public FeatureCounts (InstanceList ilist)
	{
		super (ilist.getDataAlphabet(), calcFeatureCounts (ilist));
	}

	public FeatureCounts (Alphabet vocab, double[] counts)
	{
		super (vocab, counts);
	}

	public static class Factory implements RankedFeatureVector.Factory
	{
		public Factory ()
		{
		}
		
		public RankedFeatureVector newRankedFeatureVector (InstanceList ilist)
		{
			return new FeatureCounts (ilist);
		}
	}
	
}
