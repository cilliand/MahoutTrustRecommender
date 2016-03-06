package com.cillian.bigdataanalytics;

import java.util.Collection;

import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.similarity.PreferenceInferrer;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

public class TrustSimilarity implements UserSimilarity, ItemSimilarity {
	// TODO Auto-generated method stub
	private final FastByIDMap<FastIDSet> trustMap;

	public TrustSimilarity(FastByIDMap<FastIDSet> trustMap) {
		this.trustMap = trustMap;
	}

	public double userSimilarity(long itemID1, long itemID2) throws TasteException {
		if (trustMap.get(itemID1) == null)
			return 0;
		else if (trustMap.get(itemID1).contains(itemID2))
			return 1;
		else
			return -1;
	}

	public void setPreferenceInferrer(PreferenceInferrer inferrer) {
		// TODO Auto-generated method stub

	}

	public double itemSimilarity(long itemID1, long itemID2) throws TasteException {
		if (trustMap.get(itemID1) == null)
			return 0;
		else if (trustMap.get(itemID1).contains(itemID2))
			return 1;
		else
			return -1;
	}

	public double[] itemSimilarities(long itemID1, long[] itemID2s) throws TasteException {
		double[] result = new double[itemID2s.length];
		for (int i = 0; i < itemID2s.length; i++) {
			result[i] = itemSimilarity(itemID1, itemID2s[i]);
		}
		return result;
	}

	public long[] allSimilarItemIDs(long itemID) throws TasteException {
		return trustMap.get(itemID).toArray();
	}

	public void refresh(Collection<Refreshable> alreadyRefreshed) {
		// TODO Auto-generated method stub

	}

}
