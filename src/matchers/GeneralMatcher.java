package matchers;

import static org.simmetrics.builders.StringMetricBuilder.with;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.simmetrics.StringMetric;
import org.simmetrics.metrics.SmithWatermanGotoh;
import org.simmetrics.simplifiers.Simplifiers;

import com.opencsv.CSVReader;

import castor.similarity.SimilarValue;
import castor.utils.TimeWatch;

public class GeneralMatcher {

	public static void main(String[] args) {		
		String file1 = args[0];
		int attributeNumber1 = Integer.parseInt(args[1]);
		String file2 = args[2];
		int attributeNumber2 = Integer.parseInt(args[3]);
		String output = args[4];
		int maxMatches = Integer.parseInt(args[5]);
		double minSimilarityScore = Double.parseDouble(args[6]);
		
		TimeWatch tw = TimeWatch.start();
//		match(file1, attributeNumber1, file2, attributeNumber2, output, false, maxMatches, minSimilarityScore);
		matchParallel(file1, attributeNumber1, file2, attributeNumber2, output, false, maxMatches, minSimilarityScore);
		System.out.println(tw.time());
	}
	
	/*
	 * Match entities in the specified column of file1 with entities in the specified column of file2. 
	 * If oneMatch=false, it matches at most the number of entities specified by MAX_MATCHES.
	 * If oneMatch=true, it matches only one entity (the most similar one). If there are ties, it chooses one randomly.
	 */
	public static void match(String file1, int attributeNumber1, String file2, int attributeNumber2, String output, boolean oneMatch, int maxMatches, double minSimilarityScore) {
		StringMetric metric = 
				with(new SmithWatermanGotoh())
				.simplify(Simplifiers.removeDiacritics())
				.simplify(Simplifiers.toLowerCase())
				.build();
		
		List<String> list1 = new LinkedList<String>();
		Map<String,Set<String>> list2Blocks = new HashMap<String,Set<String>>();
		readFiles(file1, attributeNumber1, file2, attributeNumber2, list1, list2Blocks);
		
		System.out.println("Start matching");
		StringBuilder sb = new StringBuilder();
		
		int c = 0;
		for(String entity1 : list1) {
//			if (c > 1000)
//				break;
			if ((c % 100) == 0)
				System.out.println(c);
			c++;
			
			PriorityQueue<SimilarValue> heap = new PriorityQueue<SimilarValue>(10,
					Comparator.comparing(SimilarValue::getDistance).thenComparing(SimilarValue::getValue).reversed());
			
			
			String[] tokens = entity1.split("[\\p{Punct}\\s]+");
			for (String token : tokens) {
				token = token.toLowerCase();
				if (list2Blocks.containsKey(token)) {
					for (String entity2 : list2Blocks.get(token)) {
						float score1 = metric.compare(entity1, entity2);
						float score2 = mySimilarity(entity1, entity2);
						
						float similarityScore = (score1 + score2) / 2; 
						
						if (similarityScore >= minSimilarityScore) {
//							System.out.println(entity1+" - " + entity2 + " - " + similarityScore);
							heap.add(new SimilarValue(entity2, (int)(similarityScore*100)));
						}
					}
				}
			}
			
			if (oneMatch) {
				if (!heap.isEmpty()) {
					int maxScore = heap.peek().getDistance();
					
					List<String> topMatches = new ArrayList<String>();
					while (!heap.isEmpty() && heap.peek().getDistance() == maxScore) {
						topMatches.add(heap.poll().getValue());
					}
					Random rand = new Random();
					String matchedEntity = topMatches.get(rand.nextInt(topMatches.size()));
					
		//			System.out.println(entity1+"-"+matchedEntity);
					sb.append("\"" + entity1.replace("\"", "\"\"") + "\", \"" + matchedEntity.replace("\"", "\"\"") + "\"\n");
				}
			} else {
				Set<String> matches = new LinkedHashSet<String>();
				int counter = 0;
				while(!heap.isEmpty() && counter < maxMatches) {
					String entity2 = heap.poll().getValue();
					matches.add(entity2);
					counter++;
				}
				
				for (String entity2 : matches) {
	//				System.out.println(entity1+"-"+entity2);
					sb.append("\"" + entity1.replace("\"", "\"\"") + "\", \"" + entity2.replace("\"", "\"\"") + "\"\n");
				}
			}
		}
		
		FileWriter fileWriter;
		try {
			fileWriter = new FileWriter(output);
			PrintWriter printWriter = new PrintWriter(fileWriter);
			printWriter.print(sb.toString());
			printWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Match entities in the specified column of file1 with entities in the specified column of file2. 
	 * If oneMatch=false, it matches at most the number of entities specified by MAX_MATCHES.
	 * If oneMatch=true, it matches only one entity (the most similar one). If there are ties, it chooses one randomly.
	 * Uses parallelization in Java 8.
	 */
	public static void matchParallel(String file1, int attributeNumber1, String file2, int attributeNumber2, String output, boolean oneMatch, int maxMatches, double minSimilarityScore) {
		StringMetric metric = 
				with(new SmithWatermanGotoh())
				.simplify(Simplifiers.removeDiacritics())
				.simplify(Simplifiers.toLowerCase())
				.build();
		
		List<String> list1 = new LinkedList<String>();
		Map<String,Set<String>> list2Blocks = new HashMap<String,Set<String>>();
		readFiles(file1, attributeNumber1, file2, attributeNumber2, list1, list2Blocks);
		
		System.out.println("Start matching");
		
		Stream<String> matchedMovies = list1.parallelStream().map(new Function<String, String>() {
			@Override
            public String apply(String entity1) {
				StringBuilder sb = new StringBuilder();
				
				PriorityQueue<SimilarValue> heap = new PriorityQueue<SimilarValue>(10,
						Comparator.comparing(SimilarValue::getDistance).thenComparing(SimilarValue::getValue).reversed());
				
				String[] tokens = entity1.split("[\\p{Punct}\\s]+");
				for (String token : tokens) {
					token = token.toLowerCase();
					if (list2Blocks.containsKey(token)) {
						for (String entity2 : list2Blocks.get(token)) {
							float score1 = metric.compare(entity1, entity2);
							float score2 = mySimilarity(entity1, entity2);
							
							float similarityScore = (score1 + score2) / 2; 
							
							if (similarityScore >= minSimilarityScore) {
								heap.add(new SimilarValue(entity2, (int)(similarityScore*100)));
							}
						}
					}
				}
				
				if (oneMatch) {
					if (!heap.isEmpty()) {
						int maxScore = heap.peek().getDistance();
						
						List<String> topMatches = new ArrayList<String>();
						while (!heap.isEmpty() && heap.peek().getDistance() == maxScore) {
							topMatches.add(heap.poll().getValue());
						}
						Random rand = new Random();
						String matchedEntity = topMatches.get(rand.nextInt(topMatches.size()));
						
						sb.append("\"" + entity1.replace("\"", "\"\"") + "\", \"" + matchedEntity.replace("\"", "\"\"") + "\"\n");
					}
				} else {
					Set<String> matches = new LinkedHashSet<String>();
					int counter = 0;
					while(!heap.isEmpty() && counter < maxMatches) {
						String entity2 = heap.poll().getValue();
						matches.add(entity2);
						counter++;
					}
					
					for (String entity2 : matches) {
						sb.append("\"" + entity1.replace("\"", "\"\"") + "\", \"" + entity2.replace("\"", "\"\"") + "\"\n");
					}
				}
				
				return sb.toString();
            }
		});
		
		System.out.println("Collecting");
		String matches = matchedMovies.collect(Collectors.joining());
		
		FileWriter fileWriter;
		try {
			fileWriter = new FileWriter(output);
			PrintWriter printWriter = new PrintWriter(fileWriter);
			printWriter.print(matches);
			printWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void readFiles(String file1, int attributeNumber1, String file2, int attributeNumber2, List<String> list1, Map<String,Set<String>> list2Blocks) {
		String[] stopwords = {"a", "an", "and", "are", "as", "at", "be", "but", "by","for", "if", "in", "into", "is", "it","no", "not", "of", "on", "or", "such","that", "the", "their", "then", "there", "these","they", "this", "to", "was", "will", "with"};
		Set<String> stopWordSet = new HashSet<String>(Arrays.asList(stopwords));
		
		BufferedReader br = null;
		CSVReader reader = null;
		try {
			// Read file 1
			reader = new CSVReader(new FileReader(file1));
		    String [] nextLine;
		    // Skip header
		    reader.readNext();
		    while ((nextLine = reader.readNext()) != null) {
		    	String value = nextLine[attributeNumber1];
		    	list1.add(value);
		    }
		    
		    // Read file 2
		    reader = new CSVReader(new FileReader(file2));
		    // Skip header
		    reader.readNext();
		    while ((nextLine = reader.readNext()) != null) {
		    	String value = nextLine[attributeNumber2];
				String[] tokens = value.split("[\\p{Punct}\\s]+");
				for (String token : tokens) {
					token = token.toLowerCase();
					if (!stopWordSet.contains(token)) {
						if (!list2Blocks.containsKey(token)) {
							list2Blocks.put(token, new HashSet<String>());
						}
						list2Blocks.get(token).add(value);
					}
				}
		    }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private static float mySimilarity(final String string1, final String string2) {
        if (string1.length() >= string2.length()) {
            return (float) string2.length() / (float) string1.length();
        } else {
            return (float) string1.length() / (float) string2.length();
        }
    }

}
