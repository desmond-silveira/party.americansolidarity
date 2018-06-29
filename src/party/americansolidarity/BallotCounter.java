/**
 * Copyright © 2018 Desmond Silveira.  All rights reserved.
 * <p/>
 * This software is free to use and extend to all registered members of the American Solidarity Party.  Any
 * extensions must give credit to the author and preserve this notice.
 */
package party.americansolidarity;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This program reads a CSV file of ballots and returns the tallied results using
 * <ul>
 *   <li>Approval Voting</li>
 *   <li>Proportional Approval Voting</li>
 *   <li>Satisfaction Approval Voting</li>
 * </ul>
 * The format of the CSV file consists of a header line with all of the candidates followed by lines
 * representing each ballot. A non-empty entry in the same index location (column) as the candidate in the
 * header line indicates a vote for that candidate.  Example:
 * <pre>
 * Hillary Clinton,Donald Trump,Gary Johnson,Jill Stein,Mike Maturen
 * ,2,,,5
 * 1,,,,
 * ,,3,4,5
 *</pre>
 * indicates that there are five candidates and three ballots with the first ballot containing votes for
 * Trump and Maturen, the second ballot containing a vote for Clinton, and the third ballot containing votes
 * for Johnson, Stein, and Maturen.
 * <p/>
 * Note that this format is easily exportable from <a href="http://www.surveymonkey.com">SurveyMonkey</a>.
 * 
 * @author Desmond Silveira
 */
public class BallotCounter {
	
	private static final System.Logger LOGGER = System.getLogger(BallotCounter.class.getName());
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.out.println("Usage:  BallotCounter <filepath>");
		}
		List<Set<Candidate>> list = readBallots(args[2]);
		
		System.out.println("APPROVAL VOTING");
		System.out.println("=============================");
		for (Map.Entry<Candidate, Integer> entry : sortDescByValue(countApproval(list))) {
			System.out.format("%1$-22s%2$7d\n", entry.getKey().getName(), entry.getValue());
		}

		
		System.out.println("\nPROPORTIONAL APPROVAL VOTING");
		System.out.println("=============================");
		Map<Candidate, Double> results = countProportionalApproval(list);
		LOGGER.log(Level.TRACE, "Final Results");
		LOGGER.log(Level.TRACE, "-------------");
		for (Map.Entry<Candidate, Double> entry : sortDescByValue(results)) {
			System.out.format("%1$-22s%2$7.3f\n", entry.getKey().getName(), entry.getValue());
		}
		
		System.out.println("\nSATISFACTION APPROVAL VOTING");
		System.out.println("=============================");
		for (Map.Entry<Candidate, Double> entry : sortDescByValue(countSatisfactionApproval(list))) {
			System.out.format("%1$-22s%2$7.3f\n", entry.getKey().getName(), entry.getValue());
		}
	}

	private static Map<Candidate, Integer> countApproval(List<Set<Candidate>> ballots) {
		Map<Candidate, Integer> results = new HashMap<>();
		for (Set<Candidate> ballot : ballots) {
			if (!ballot.isEmpty()) {
				for (Candidate c : ballot) {
					results.put(c, results.getOrDefault(c, 0) + 1);
				}
			}
		}
		return results;
	}
	
	private static Map<Candidate, Double> countProportionalApproval(List<Set<Candidate>> ballots) {
		Map<Candidate, Double> results = new HashMap<>();
		for (int round = 1; round <= Candidate.count(); round++) {
			LOGGER.log(Level.TRACE, "Round {0}", round);
			Map<Candidate, Double> roundResults = new HashMap<>();
			for (Set<Candidate> ballot : ballots) {
				if (!ballot.isEmpty()) {
					int count = getApprovedCandidateCount(ballot, results.keySet());
					for (Candidate c : ballot) {
						if (!results.containsKey(c)) {
							roundResults.put(c, roundResults.getOrDefault(c, 0.0) + 1.0 / (1.0 + count));
						}
					}
				}
			}
			Set<Map.Entry<Candidate, Double>> sortedResults = sortDescByValue(roundResults);
			if (LOGGER.isLoggable(Level.TRACE)) {
				for (Map.Entry<Candidate, Double> entry : sortedResults) {
					LOGGER.log(Level.TRACE, "{0} {1}", entry.getKey().getName(), entry.getValue());//"%1$-65s%2$7.3f\n"
				}
			}
			Map.Entry<Candidate, Double> winner = sortedResults.iterator().next();
			results.put(winner.getKey(), winner.getValue());
		}

		return results;
	}

	private static Map<Candidate, Double> countSatisfactionApproval(List<Set<Candidate>> ballots) {
		Map<Candidate, Double> results = new HashMap<>();
		for (Set<Candidate> ballot : ballots) {
			if (!ballot.isEmpty()) {
				for (Candidate c : ballot) {
					results.put(c, results.getOrDefault(c, 0.0) + 1.0 / ballot.size());
				}
			}
		}
		return results;
	}
	
	private static int getApprovedCandidateCount(Set<Candidate> ballot, Set<Candidate> combination) {
		Set<Candidate> intersection = new HashSet<>(ballot);
		intersection.retainAll(combination);
		return intersection.size();
	}
	
	private static List<Set<Candidate>> readBallots(String filepathStr) throws IOException {
		Path path = Paths.get(filepathStr);
		List<String> lines = Files.readAllLines(path);
		String[] candidateNames = lines.get(0).split(",");
		for (int i = 0; i < candidateNames.length; i++) {
			new Candidate(i, candidateNames[i]);
		}
		return lines.stream().skip(1).map(mapper).collect(Collectors.toList());
	}
	
	private static Function<String, Set<Candidate>> mapper = (line) -> {
		Set<Candidate> ballot = new HashSet<>();
		String[] votes = line.split(",");
		for (int i = 0; i < votes.length; i++) {
			if (votes[i] != null && !votes[i].isEmpty()) {
				ballot.add(Candidate.fromValue(i));
			}
		}
		return ballot;
	};

	private static <K, V extends Comparable<? super V>> Set<Entry<K, V>> sortDescByValue(Map<K, V> map) {
		return map.entrySet().stream().sorted(Map.Entry.<K, V>comparingByValue().reversed())
				.limit(100)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new))
				.entrySet();
	}
	
	private static class Candidate {

		private final String name;
		
		private static final Map<Integer, Candidate> lookup = new HashMap<Integer, Candidate>();
		
		Candidate(int i, String name) {
			this.name = name;
			lookup.put(i, this);
		}
		
		String getName() {
			return name;
		}
		
		static Candidate fromValue(Integer i) {
			return lookup.get(i);
		}
		
		static int count() {
			return lookup.size();
		}
	}
}
