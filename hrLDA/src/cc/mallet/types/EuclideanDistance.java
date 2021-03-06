package cc.mallet.types;

import cc.mallet.types.SparseVector;

public class EuclideanDistance implements Metric {

	public double distance(SparseVector a, SparseVector b)    {
		double dist = 0;
		double diff;
		
		if (a==null || b==null) {
		    throw new IllegalArgumentException("Distance from a null vector is undefined.");
		}

		int leftLength = a.numLocations();
		int rightLength = b.numLocations();
		int leftIndex = 0;
		int rightIndex = 0;
		int leftFeature, rightFeature;

		// We assume that features are sorted in ascending order.
		// We'll walk through the two feature lists in order, checking
		//  whether the two features are the same.

		while (leftIndex < leftLength && rightIndex < rightLength) {

			leftFeature = a.indexAtLocation(leftIndex);
			rightFeature = b.indexAtLocation(rightIndex);

			if (leftFeature < rightFeature) {
				diff = a.valueAtLocation(leftIndex);
				leftIndex ++;
			}
			else if (leftFeature == rightFeature) {
				diff = a.valueAtLocation(leftIndex) - b.valueAtLocation(rightIndex);
				leftIndex ++;
				rightIndex ++;
			}
			else {
				diff = b.valueAtLocation(rightIndex);
				rightIndex ++;
			}

			dist += diff * diff;
		}

		// Pick up any additional features at the end of the two lists.
		while (leftIndex < leftLength) {
			diff = a.valueAtLocation(leftIndex);
			dist += diff * diff;
			leftIndex++;
		}

		while (rightIndex < rightLength) {
			diff = b.valueAtLocation(rightIndex);
			dist += diff * diff;
			rightIndex++;
		}

		return Math.sqrt(dist);
	}
}
