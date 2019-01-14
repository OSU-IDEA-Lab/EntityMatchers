package matchers;

import static org.simmetrics.builders.StringMetricBuilder.with;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.simmetrics.StringMetric;
import org.simmetrics.metrics.SmithWatermanGotoh;
import org.simmetrics.simplifiers.Simplifiers;

import castor.similarity.SimilarValue;
import castor.utils.TimeWatch;
import utils.CSVParser;

public class IMDB_OMDB_Matcher {
	
	public static final int MAX_MATCHES = 10;

	public static void main(String[] args) {		
		String file1 = args[0];
		String file2 = args[1];
		String output = args[2];
		
		TimeWatch tw = TimeWatch.start();
		match2(file1, file2, output);
		System.out.println(tw.time());
	}

	private static void match(String file1, String file2, String output) {
//		StringMetric metric = StringMetrics.smithWatermanGotoh();
		StringMetric metric = 
				with(new SmithWatermanGotoh())
				.simplify(Simplifiers.removeDiacritics())
				.simplify(Simplifiers.toLowerCase())
				.build();
//		StringMetric metric = StringMetrics.jaroWinkler();

		
		List<String> list1 = new LinkedList<String>();
		List<String> list2 = new LinkedList<String>();
		
		BufferedReader br = null;
		try {
			// Read file 1
			br = new BufferedReader(new FileReader(new File(file1)));
			String line;
			// Skip header
			line = br.readLine();
			while ((line = br.readLine()) != null) {
				List<String> parsed = CSVParser.parseLine(line);
				String title = parsed.get(1);
				list1.add(title);
			}
			
			// Read file 2
			br = new BufferedReader(new FileReader(new File(file2)));
			// Skip header
			line = br.readLine();
			while ((line = br.readLine()) != null) {
				List<String> parsed = CSVParser.parseLine(line);
				String title = parsed.get(1);
				list2.add(title);
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
		}
		
		System.out.println("Start matching");
		StringBuilder sb = new StringBuilder();
		
		int c = 0;
		for(String movie1 : list1) {
			if (c > 10)
				break;
			c++;
			
			
			PriorityQueue<SimilarValue> heap = new PriorityQueue<SimilarValue>(10,
					Comparator.comparing(SimilarValue::getDistance).thenComparing(SimilarValue::getValue).reversed());
			
			for (String movie2 : list2) {
				float score1 = metric.compare(movie1, movie2);
				float score2 = mySimilarity(movie1, movie2);
				
				float similarityScore = (score1 + score2) / 2; 
				
				
				if (similarityScore > 0.65) {
//					System.out.println(movie1+" - " + movie2 + " - " + similarityScore);
					heap.add(new SimilarValue(movie2, (int)(similarityScore*100)));
				}
			}
			
			int counter = 0;
			while(!heap.isEmpty() && counter < MAX_MATCHES) {
				String movie2 = heap.poll().getValue();
				System.out.println(movie1+" - "+movie2);
				sb.append("\"" + movie1.replace("\"", "\"\"") + "\", \"" + movie2.replace("\"", "\"\"") + "\"\n");
				counter++;
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
	
	private static void match2(String file1, String file2, String output) {
//		StringMetric metric = StringMetrics.smithWatermanGotoh();
		StringMetric metric = 
				with(new SmithWatermanGotoh())
				.simplify(Simplifiers.removeDiacritics())
				.simplify(Simplifiers.toLowerCase())
				.build();
//		StringMetric metric = StringMetrics.jaroWinkler();
		
//		String[] stopwords = {"a", "as", "able", "about", "above", "according", "accordingly", "across", "actually", "after", "afterwards", "again", "against", "aint", "all", "allow", "allows", "almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "an", "and", "another", "any", "anybody", "anyhow", "anyone", "anything", "anyway", "anyways", "anywhere", "apart", "appear", "appreciate", "appropriate", "are", "arent", "around", "as", "aside", "ask", "asking", "associated", "at", "available", "away", "awfully", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "believe", "below", "beside", "besides", "best", "better", "between", "beyond", "both", "brief", "but", "by", "cmon", "cs", "came", "can", "cant", "cannot", "cant", "cause", "causes", "certain", "certainly", "changes", "clearly", "co", "com", "come", "comes", "concerning", "consequently", "consider", "considering", "contain", "containing", "contains", "corresponding", "could", "couldnt", "course", "currently", "definitely", "described", "despite", "did", "didnt", "different", "do", "does", "doesnt", "doing", "dont", "done", "down", "downwards", "during", "each", "edu", "eg", "eight", "either", "else", "elsewhere", "enough", "entirely", "especially", "et", "etc", "even", "ever", "every", "everybody", "everyone", "everything", "everywhere", "ex", "exactly", "example", "except", "far", "few", "ff", "fifth", "first", "five", "followed", "following", "follows", "for", "former", "formerly", "forth", "four", "from", "further", "furthermore", "get", "gets", "getting", "given", "gives", "go", "goes", "going", "gone", "got", "gotten", "greetings", "had", "hadnt", "happens", "hardly", "has", "hasnt", "have", "havent", "having", "he", "hes", "hello", "help", "hence", "her", "here", "heres", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "hi", "him", "himself", "his", "hither", "hopefully", "how", "howbeit", "however", "i", "id", "ill", "im", "ive", "ie", "if", "ignored", "immediate", "in", "inasmuch", "inc", "indeed", "indicate", "indicated", "indicates", "inner", "insofar", "instead", "into", "inward", "is", "isnt", "it", "itd", "itll", "its", "its", "itself", "just", "keep", "keeps", "kept", "know", "knows", "known", "last", "lately", "later", "latter", "latterly", "least", "less", "lest", "let", "lets", "like", "liked", "likely", "little", "look", "looking", "looks", "ltd", "mainly", "many", "may", "maybe", "me", "mean", "meanwhile", "merely", "might", "more", "moreover", "most", "mostly", "much", "must", "my", "myself", "name", "namely", "nd", "near", "nearly", "necessary", "need", "needs", "neither", "never", "nevertheless", "new", "next", "nine", "no", "nobody", "non", "none", "noone", "nor", "normally", "not", "nothing", "novel", "now", "nowhere", "obviously", "of", "off", "often", "oh", "ok", "okay", "old", "on", "once", "one", "ones", "only", "onto", "or", "other", "others", "otherwise", "ought", "our", "ours", "ourselves", "out", "outside", "over", "overall", "own", "particular", "particularly", "per", "perhaps", "placed", "please", "plus", "possible", "presumably", "probably", "provides", "que", "quite", "qv", "rather", "rd", "re", "really", "reasonably", "regarding", "regardless", "regards", "relatively", "respectively", "right", "said", "same", "saw", "say", "saying", "says", "second", "secondly", "see", "seeing", "seem", "seemed", "seeming", "seems", "seen", "self", "selves", "sensible", "sent", "serious", "seriously", "seven", "several", "shall", "she", "should", "shouldnt", "since", "six", "so", "some", "somebody", "somehow", "someone", "something", "sometime", "sometimes", "somewhat", "somewhere", "soon", "sorry", "specified", "specify", "specifying", "still", "sub", "such", "sup", "sure", "ts", "take", "taken", "tell", "tends", "th", "than", "thank", "thanks", "thanx", "that", "thats", "thats", "the", "their", "theirs", "them", "themselves", "then", "thence", "there", "theres", "thereafter", "thereby", "therefore", "therein", "theres", "thereupon", "these", "they", "theyd", "theyll", "theyre", "theyve", "think", "third", "this", "thorough", "thoroughly", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "took", "toward", "towards", "tried", "tries", "truly", "try", "trying", "twice", "two", "un", "under", "unfortunately", "unless", "unlikely", "until", "unto", "up", "upon", "us", "use", "used", "useful", "uses", "using", "usually", "value", "various", "very", "via", "viz", "vs", "want", "wants", "was", "wasnt", "way", "we", "wed", "well", "were", "weve", "welcome", "well", "went", "were", "werent", "what", "whats", "whatever", "when", "whence", "whenever", "where", "wheres", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whos", "whoever", "whole", "whom", "whose", "why", "will", "willing", "wish", "with", "within", "without", "wont", "wonder", "would", "would", "wouldnt", "yes", "yet", "you", "youd", "youll", "youre", "youve", "your", "yours", "yourself", "yourselves", "zero"};
		String[] stopwords = {"a", "an", "and", "are", "as", "at", "be", "but", "by","for", "if", "in", "into", "is", "it","no", "not", "of", "on", "or", "such","that", "the", "their", "then", "there", "these","they", "this", "to", "was", "will", "with"};

		Set<String> stopWordSet = new HashSet<String>(Arrays.asList(stopwords));


		
		List<String> list1 = new LinkedList<String>();
//		List<String> list2 = new LinkedList<String>();
		Map<String,Set<String>> list2Blocks = new HashMap<String,Set<String>>();
		
		BufferedReader br = null;
		try {
			// Read file 1
			br = new BufferedReader(new FileReader(new File(file1)));
			String line;
			// Skip header
			line = br.readLine();
			while ((line = br.readLine()) != null) {
				List<String> parsed = CSVParser.parseLine(line);
				String title = parsed.get(1);
				list1.add(title);
			}
			
			// Read file 2
			br = new BufferedReader(new FileReader(new File(file2)));
			// Skip header
			line = br.readLine();
			while ((line = br.readLine()) != null) {
				List<String> parsed = CSVParser.parseLine(line);
				String title = parsed.get(1);
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
		}
		
		System.out.println("Start matching");
		StringBuilder sb = new StringBuilder();
		
		int c = 0;
		for(String movie1 : list1) {
//			if (c > 1000)
//				break;
			if ((c % 100) == 0)
				System.out.println(c);
			c++;
			
			PriorityQueue<SimilarValue> heap = new PriorityQueue<SimilarValue>(10,
					Comparator.comparing(SimilarValue::getDistance).thenComparing(SimilarValue::getValue).reversed());
			
			
			String[] tokens = movie1.split("[\\p{Punct}\\s]+");
			for (String token : tokens) {
				if (list2Blocks.containsKey(token)) {
					for (String movie2 : list2Blocks.get(token)) {
						float score1 = metric.compare(movie1, movie2);
						float score2 = mySimilarity(movie1, movie2);
						
						float similarityScore = (score1 + score2) / 2; 
						
						if (similarityScore > 0.65) {
//							System.out.println(movie1+" - " + movie2 + " - " + similarityScore);
							heap.add(new SimilarValue(movie2, (int)(similarityScore*100)));
						}
					}
				}
			}
			
			Set<String> matches = new LinkedHashSet<String>();
			int counter = 0;
			while(!heap.isEmpty() && counter < MAX_MATCHES) {
				String movie2 = heap.poll().getValue();
				matches.add(movie2);
				counter++;
			}
			
			for (String movie2 : matches) {
//				System.out.println(movie1+" - "+movie2);
				sb.append("\"" + movie1.replace("\"", "\"\"") + "\", \"" + movie2.replace("\"", "\"\"") + "\"\n");
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
