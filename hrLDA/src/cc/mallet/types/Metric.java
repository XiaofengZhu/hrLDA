package cc.mallet.types;

import cc.mallet.types.SparseVector;


public interface Metric {

    public double distance( SparseVector a, SparseVector b);

}

