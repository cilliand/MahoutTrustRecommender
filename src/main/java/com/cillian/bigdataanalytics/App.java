package com.cillian.bigdataanalytics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.IRStatistics;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.eval.RecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.GenericRecommenderIRStatsEvaluator;
import org.apache.mahout.cf.taste.impl.eval.RMSRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.SpearmanCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.similarity.PreferenceInferrer;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

/**
 * Hello world!
 *
 */
public class App {
	
	public static FastByIDMap<FastIDSet> loadTrustData(){
		FastByIDMap<FastIDSet> trustMap = new FastByIDMap<FastIDSet>();
		try {
			// Read input
			BufferedReader br = new BufferedReader(new FileReader("trust.csv"));
			String line;
			while ((line = br.readLine()) != null) {
				String[] split = line.split(",");
				if(!trustMap.containsKey(Long.parseLong(split[0]))){
						trustMap.put(Long.parseLong(split[0]), new FastIDSet());
				}
				trustMap.get(Long.parseLong(split[0])).add(Long.parseLong(split[1]));
			}
			br.close();
		} catch (IOException e){
			System.out.println("Could not load trust.csv");
			System.exit(0);
		}
		return trustMap;
	}
	
	public static void main(String[] args) throws IOException, TasteException {
		
		final FastByIDMap<FastIDSet> trustMap = loadTrustData();
		TreeMap<Integer, Double> scores = new TreeMap<Integer, Double>();
		double lowestScore = 50; int lowestNModel = 0;
		DataModel model = new FileDataModel(new File("ratings.csv"));
		for(int i = 1; i < 3000; i+= 100){
		final int b = i;
		RecommenderBuilder recommenderBuilder = new RecommenderBuilder() {
			public Recommender buildRecommender(DataModel model) throws TasteException {
				UserSimilarity similarity = new TrustSimilarity(trustMap);
				UserNeighborhood neighborhood = new NearestNUserNeighborhood(1201, similarity, model);
				return new GenericUserBasedRecommender(model, neighborhood, similarity);
			}
		};
//		Recommender recommender = recommenderBuilder.buildRecommender(model);
//		List<RecommendedItem> recommendations = recommender.recommend(12, 5);
//		for (RecommendedItem recommendation : recommendations) {
//			System.out.println(recommendation);
//		}

		RecommenderEvaluator scoreEvaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
		double score = scoreEvaluator.evaluate(recommenderBuilder, null, model, 0.9, 0.1);

		if(score < lowestScore){
			lowestNModel = b;
			lowestScore = score;
		}
		//System.out.println("Score at "+b+" neighbours: " + score);
		
//		 RecommenderIRStatsEvaluator recPrecEvaluator = new
//		 GenericRecommenderIRStatsEvaluator();
//		 IRStatistics stats = recPrecEvaluator.evaluate(recommenderBuilder,
//		 null, model, null, 2,
//		 GenericRecommenderIRStatsEvaluator.CHOOSE_THRESHOLD, 1.0);
//		 System.out.println("Precision: " + stats.getPrecision());
//		 System.out.println("Recall: " + stats.getRecall());
		}
		System.out.println("Lowest score at "+lowestNModel+" neighbours: " + lowestScore);
	}
}
	

	


