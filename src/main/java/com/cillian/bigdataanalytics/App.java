package com.cillian.bigdataanalytics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
import org.apache.mahout.cf.taste.impl.recommender.slopeone.SlopeOneRecommender;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
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

	public static FastByIDMap<FastIDSet> loadTrustData() {
		FastByIDMap<FastIDSet> trustMap = new FastByIDMap<FastIDSet>();
		try {
			// Read input
			BufferedReader br = new BufferedReader(new FileReader("trust.csv"));
			String line;
			while ((line = br.readLine()) != null) {
				String[] split = line.split(",");
				if (!trustMap.containsKey(Long.parseLong(split[0]))) {
					trustMap.put(Long.parseLong(split[0]), new FastIDSet());
				}
				trustMap.get(Long.parseLong(split[0])).add(Long.parseLong(split[1]));
			}
			br.close();
		} catch (IOException e) {
			System.out.println("Could not load trust.csv");
			System.exit(0);
		}
		return trustMap;
	}

	static DataModel model;
	static int maxNeighbours;
	static BufferedWriter bw;
	public static void main(String[] args) throws IOException, TasteException {

		model = new FileDataModel(new File("ratings.csv"));
		maxNeighbours = 1000;
		bw = new BufferedWriter(new FileWriter("output.csv"));
		testUserBasedSimilarity();
		testTrustSimilarity();
		bw.flush();
		bw.close();
		
		testItemBasedSimilarity();
		testSlopeOneRecommender();
		
		
	}

	public static void testUserBasedSimilarity() throws TasteException, IOException {
		
		System.out.println("User Based Simiarlity");
		System.out.println("----------------------");
		for (int i = 10; i <= maxNeighbours; i += 10) {
			final int numNeighbours = i;
			RecommenderBuilder recommenderBuilder = new RecommenderBuilder() {
				public Recommender buildRecommender(DataModel model) throws TasteException {
					UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
					UserNeighborhood neighborhood = new NearestNUserNeighborhood(numNeighbours, similarity, model);
					return new GenericUserBasedRecommender(model, neighborhood, similarity);
				}
			};

			RecommenderEvaluator scoreEvaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
			double score = scoreEvaluator.evaluate(recommenderBuilder, null, model, 0.9, 0.1);
			String a = "EuclideanDistanceSimilarity";
			String b = "Pearson";
			int diff = (a.length() - b.length())+20;
			System.out.format("Pearson Score: %"+diff+"s Neighbours: %3d\n", score, numNeighbours);
			bw.write("Pearson,"+score+","+numNeighbours+"\n");
			//System.out.println("Pearons Score: " + score + " Neighbours: " + numNeighbours);
		}
		// Spearman
		for (int i = 10; i <= maxNeighbours; i += 10) {
			final int numNeighbours = i;
			RecommenderBuilder recommenderBuilder = new RecommenderBuilder() {
				public Recommender buildRecommender(DataModel model) throws TasteException {
					UserSimilarity similarity = new SpearmanCorrelationSimilarity(model);
					UserNeighborhood neighborhood = new NearestNUserNeighborhood(numNeighbours, similarity, model);
					return new GenericUserBasedRecommender(model, neighborhood, similarity);
				}
			};

			RecommenderEvaluator scoreEvaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
			double score = scoreEvaluator.evaluate(recommenderBuilder, null, model, 0.9, 0.1);
			String a = "EuclideanDistanceSimilarity";
			String b = "Spearman";
			int diff = (a.length() - b.length())+20;
			bw.write("Spearman,"+score+","+numNeighbours+"\n");
			System.out.format("Spearman Score: %"+diff+"s Neighbours: %3d\n", score, numNeighbours);
		}
		// LogLikelihood
		for (int i = 10; i <= maxNeighbours; i += 10) {
			final int numNeighbours = i;
			RecommenderBuilder recommenderBuilder = new RecommenderBuilder() {
				public Recommender buildRecommender(DataModel model) throws TasteException {
					UserSimilarity similarity = new LogLikelihoodSimilarity(model);
					UserNeighborhood neighborhood = new NearestNUserNeighborhood(numNeighbours, similarity, model);
					return new GenericUserBasedRecommender(model, neighborhood, similarity);
				}
			};

			RecommenderEvaluator scoreEvaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
			double score = scoreEvaluator.evaluate(recommenderBuilder, null, model, 0.9, 0.1);
			String a = "EuclideanDistanceSimilarity";
			String b = "LogLikelihoodSimilarity";
			int diff = (a.length() - b.length())+20;
			bw.write("LogLikelihoodSimilarity,"+score+","+numNeighbours+"\n");
			System.out.format("LogLikelihoodSimilarity Score: %"+diff+"s Neighbours: %3d\n", score, numNeighbours);
			 
		}
		
		//EuclideanDistanceSimilarity
		for (int i = 10; i <= maxNeighbours; i += 10) {
			final int numNeighbours = i;
			RecommenderBuilder recommenderBuilder = new RecommenderBuilder() {
				public Recommender buildRecommender(DataModel model) throws TasteException {
					UserSimilarity similarity = new EuclideanDistanceSimilarity(model);
					UserNeighborhood neighborhood = new NearestNUserNeighborhood(numNeighbours, similarity, model);
					return new GenericUserBasedRecommender(model, neighborhood, similarity);
				}
			};

			RecommenderEvaluator scoreEvaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
			double score = scoreEvaluator.evaluate(recommenderBuilder, null, model, 0.9, 0.1);
			bw.write("EuclideanDistanceSimilarity,"+score+","+numNeighbours+"\n");
			System.out.format("EuclideanDistanceSimilarity Score: %20s Neighbours: %3d\n", score, numNeighbours);
		}
		
		// Tanimoto
				for (int i = 10; i <= maxNeighbours; i += 10) {
					final int numNeighbours = i;
					RecommenderBuilder recommenderBuilder = new RecommenderBuilder() {
						public Recommender buildRecommender(DataModel model) throws TasteException {
							UserSimilarity similarity = new TanimotoCoefficientSimilarity(model);
							UserNeighborhood neighborhood = new NearestNUserNeighborhood(numNeighbours, similarity, model);
							return new GenericUserBasedRecommender(model, neighborhood, similarity);
						}
					};

					RecommenderEvaluator scoreEvaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
					double score = scoreEvaluator.evaluate(recommenderBuilder, null, model, 0.9, 0.1);
					String a = "EuclideanDistanceSimilarity";
					String b = "TanimotoCoefficient";
					int diff = (a.length() - b.length())+20;
					bw.write("TanimotoCoefficient,"+score+","+numNeighbours+"\n");
					System.out.format("TanimotoCoefficient Score: %"+diff+"s Neighbours: %3d\n", score, numNeighbours);
					 
				}
				
	}

	public static void testTrustSimilarity() throws TasteException, IOException {
		System.out.println("Trust Based Simiarlity");
		System.out.println("----------------------");
		final FastByIDMap<FastIDSet> trustMap = loadTrustData();
		for (int i = 10; i <= maxNeighbours; i += 10) {
			final int numNeighbours = i;
			RecommenderBuilder recommenderBuilder = new RecommenderBuilder() {
				public Recommender buildRecommender(DataModel model) throws TasteException {
					UserSimilarity similarity = new TrustSimilarity(trustMap);
					UserNeighborhood neighborhood = new NearestNUserNeighborhood(numNeighbours, similarity, model);
					return new GenericUserBasedRecommender(model, neighborhood, similarity);
				}
			};

			RecommenderEvaluator scoreEvaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
			double score = scoreEvaluator.evaluate(recommenderBuilder, null, model, 0.9, 0.1);
			bw.write("TrustSimilarity,"+score+","+numNeighbours+"\n");
			System.out.println("TrustSimilarity Score: " + score + " Neighbours: " + numNeighbours);
		}
	}

	public static void testItemBasedSimilarity() throws TasteException {
			System.out.println("Item Based Simiarlity");
			System.out.println("----------------------");
			RecommenderBuilder recommenderBuilder = new RecommenderBuilder() {
				public Recommender buildRecommender(DataModel model) throws TasteException {
					ItemSimilarity similarity = new PearsonCorrelationSimilarity(model);
					return new GenericItemBasedRecommender(model, similarity);
				}
			};

			RecommenderEvaluator scoreEvaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
			double score = scoreEvaluator.evaluate(recommenderBuilder, null, model, 0.9, 0.1);

			System.out.println("Pearson Score: " + score);
			
//			recommenderBuilder = new RecommenderBuilder() {
//				public Recommender buildRecommender(DataModel model) throws TasteException {
//					ItemSimilarity similarity = new SpearmanCorrelationSimilarity(model);
//					return new GenericItemBasedRecommender(model, similarity);
//				}
//			};
//
//			scoreEvaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
//			score = scoreEvaluator.evaluate(recommenderBuilder, null, model, 0.9, 0.1);
//
//			System.out.println("Spearman Score: " + score);
			
			recommenderBuilder = new RecommenderBuilder() {
				public Recommender buildRecommender(DataModel model) throws TasteException {
					ItemSimilarity similarity = new LogLikelihoodSimilarity(model);
					return new GenericItemBasedRecommender(model, similarity);
				}
			};

			scoreEvaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
			score = scoreEvaluator.evaluate(recommenderBuilder, null, model, 0.9, 0.1);

			System.out.println("LogLikelihoodSimilarity Score: " + score);
			
			recommenderBuilder = new RecommenderBuilder() {
				public Recommender buildRecommender(DataModel model) throws TasteException {
					ItemSimilarity similarity = new EuclideanDistanceSimilarity(model);
					return new GenericItemBasedRecommender(model, similarity);
				}
			};

			scoreEvaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
			score = scoreEvaluator.evaluate(recommenderBuilder, null, model, 0.9, 0.1);

			System.out.println("EuclideanDistanceSimilarity Score: " + score);
			
			recommenderBuilder = new RecommenderBuilder() {
				public Recommender buildRecommender(DataModel model) throws TasteException {
					ItemSimilarity similarity = new TanimotoCoefficientSimilarity(model);
					return new GenericItemBasedRecommender(model, similarity);
				}
			};

			scoreEvaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
			score = scoreEvaluator.evaluate(recommenderBuilder, null, model, 0.9, 0.1);

			System.out.println("Tanimoto Score: " + score);

			
		}
	
		public static void testSlopeOneRecommender() throws TasteException{
			RecommenderBuilder recommenderBuilder = new RecommenderBuilder() {
				public Recommender buildRecommender(DataModel model) throws TasteException {
					Recommender recommender = new SlopeOneRecommender(model);
					return recommender;
				}
			};

			RecommenderEvaluator scoreEvaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
			double score = scoreEvaluator.evaluate(recommenderBuilder, null, model, 0.9, 0.1);

			System.out.println("SlopeOne Score: " + score);
		}
}
