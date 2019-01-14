package matchers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

import castor.similarity.HSTree;
import castor.similarity.HSTreeCreator;
import castor.similarity.SimilarValue;
import utils.CSVParser;

public class BOM_JMDB_Matcher {

	public static void main(String[] args) {
		String file1 = args[0];
		String file2 = args[1];
		String output = args[2];
		
		match(file1, file2, output);
	}
	
	private static void match(String file1, String file2, String output) {

		List<String> bom_movies = new LinkedList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(file1)));
			String line;
			while ((line = br.readLine()) != null) {
				List<String> parsed = CSVParser.parseLine(line);
				bom_movies.add(parsed.get(0));
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

		System.out.println("BOM movies:" + bom_movies.size());
		HSTree tree = HSTreeCreator.buildHSTree(bom_movies);

		br = null;
		Random random = new Random();
		StringBuilder sb = new StringBuilder();
		try {
			br = new BufferedReader(new FileReader(new File(file2)));
			String line;
			while ((line = br.readLine()) != null) {
				List<String> parsed = CSVParser.parseLine(line);
				String id = parsed.get(0);
				String title = parsed.get(1).substring(2, parsed.get(1).length());
				String year = parsed.get(2).substring(2, parsed.get(2).length());

				Set<SimilarValue> similarValues = tree.hsSearch(title, 10);

				String newTitle = title;
				if (!similarValues.isEmpty()) {
					PriorityQueue<SimilarValue> heap = new PriorityQueue<SimilarValue>(similarValues.size(),
							Comparator.comparing(SimilarValue::getDistance).thenComparing(SimilarValue::getValue));
					heap.addAll(similarValues);

					int minDistance = heap.peek().getDistance();

					List<String> topSimilarTitles = new ArrayList<String>();
					while (!heap.isEmpty() && heap.peek().getDistance() == minDistance) {
						topSimilarTitles.add(heap.poll().getValue());
					}

					int randomIndex = random.nextInt(topSimilarTitles.size());
					newTitle = topSimilarTitles.get(randomIndex);

					System.out.println(title + "---->" + newTitle);
				}
				String tuple = id + ", \"" + newTitle + "\", \"" + year + "\"";
				sb.append(tuple + "\n");
			}
			
			FileWriter fileWriter = new FileWriter(output);
			PrintWriter printWriter = new PrintWriter(fileWriter);
			printWriter.print(sb.toString());
			printWriter.close();
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
	}
}
