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

import org.simmetrics.StringMetric;
import org.simmetrics.metrics.SmithWatermanGotoh;
import org.simmetrics.simplifiers.Simplifiers;

import com.opencsv.CSVReader;

import castor.similarity.SimilarValue;
import castor.utils.TimeWatch;

public class DBLP_GoogleScholar_MatcherOneMatch {
	
	public static final int MAX_MATCHES = 10;

	public static void main(String[] args) {		
		String file1 = args[0];
		String file2 = args[1];
		String output = args[2];
		
		TimeWatch tw = TimeWatch.start();
		match(file1, file2, output);
		System.out.println(tw.time());
	}
	
	private static void match(String file1, String file2, String output) {
		StringMetric metric = 
				with(new SmithWatermanGotoh())
				.simplify(Simplifiers.removeDiacritics())
				.simplify(Simplifiers.toLowerCase())
				.build();
		
		String[] stopwords = {"a", "an", "and", "are", "as", "at", "be", "but", "by","for", "if", "in", "into", "is", "it","no", "not", "of", "on", "or", "such","that", "the", "their", "then", "there", "these","they", "this", "to", "was", "will", "with"};

		Set<String> stopWordSet = new HashSet<String>(Arrays.asList(stopwords));
		
		
		
		List<String> list1 = new LinkedList<String>();
		Map<String,Set<String>> list2Blocks = new HashMap<String,Set<String>>();
		
		BufferedReader br = null;
		CSVReader reader = null;
		try {
//			// Read file 1
//			br = new BufferedReader(new FileReader(new File(file1)));
//			String line;
//			// Skip header
//			line = br.readLine();
//			while ((line = br.readLine()) != null) {
//				List<String> parsed = CSVParser.parseLine(line);
//				String title = parsed.get(0);
//				list1.add(title);
//			}
//			
//			// Read file 2
//			br = new BufferedReader(new FileReader(new File(file2)));
//			// Skip header
//			line = br.readLine();
//			while ((line = br.readLine()) != null) {
//				List<String> parsed = CSVParser.parseLine(line);
//				String title = parsed.get(1);
//				String[] tokens = title.split("[\\p{Punct}\\s]+");
//				for (String token : tokens) {
//					if (!stopWordSet.contains(token.toLowerCase())) {
//						if (!list2Blocks.containsKey(token)) {
//							list2Blocks.put(token, new HashSet<String>());
//						}
//						list2Blocks.get(token).add(title);
//					}
//				}
//			}
			
			
			// Read file 1
			reader = new CSVReader(new FileReader(file1));
		    String [] nextLine;
		    // Skip header
		    reader.readNext();
		    while ((nextLine = reader.readNext()) != null) {
		    	String title = nextLine[1];
		    	list1.add(title);
		    }
		    
		    // Read file 2
		    reader = new CSVReader(new FileReader(file2));
		    // Skip header
		    reader.readNext();
		    while ((nextLine = reader.readNext()) != null) {
		    	String title = nextLine[1];
				String[] tokens = title.split("[\\p{Punct}\\s]+");
				for (String token : tokens) {
					if (!stopWordSet.contains(token.toLowerCase())) {
						if (!list2Blocks.containsKey(token)) {
							list2Blocks.put(token, new HashSet<String>());
						}
						list2Blocks.get(token).add(title);
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
				if (list2Blocks.containsKey(token)) {
					for (String entity2 : list2Blocks.get(token)) {
						float score1 = metric.compare(entity1, entity2);
						float score2 = mySimilarity(entity1, entity2);
						
						float similarityScore = (score1 + score2) / 2; 
						
						if (similarityScore > 0.65) {
//							System.out.println(entity1+" - " + entity2 + " - " + similarityScore);
							heap.add(new SimilarValue(entity2, (int)(similarityScore*100)));
						}
					}
				}
			}
			
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
	
	private static float mySimilarity(final String string1, final String string2) {
        if (string1.length() >= string2.length()) {
            return (float) string2.length() / (float) string1.length();
        } else {
            return (float) string1.length() / (float) string2.length();
        }
    }

}
