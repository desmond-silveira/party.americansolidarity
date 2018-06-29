/**
 * Copyright © 2018 Desmond Silveira.  All rights reserved.
 *
 * This software is free to use and extend to all registered members of the
 * American Solidarity Party.  Any extensions must give credit to the author
 * and preserve this notice.
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
 * This program reads a CSV file of ballots and returns the tallied results
 * using
 * <ul>
 * <li>Approval Voting</li>
 * <li>Proportional Approval Voting</li>
 * <li>Satisfaction Approval Voting</li>
 * </ul>
 * The format of the CSV file consists of a header line with all of the
 * candidates followed by lines representing each ballot. A non-empty entry in
 * the same index location (column) as the candidate in the header line
 * indicates a vote for that candidate. Example:
 *
 * <pre>
 * Hillary Clinton,Donald Trump,Gary Johnson,Jill Stein,Mike Maturen
 * ,2,,,5
 * 1,,,,
 * ,,3,4,5
 * </pre>
 *
 * indicates that there are five candidates and three ballots with the first
 * ballot containing votes for Trump and Maturen, the second ballot containing a
 * vote for Clinton, and the third ballot containing votes for Johnson, Stein,
 * and Maturen.
 * <p/>
 * Note that this format is easily exportable from
 * <a href="http://www.surveymonkey.com">SurveyMonkey</a>.
 *
 * @author Desmond Silveira
 */
public class BallotCounter {

  private static final System.Logger LOGGER = System.getLogger(BallotCounter.class.getName());

  /**
   * Reads ballot file, tallies ballots, and prints results.
   *
   * @param args command line arguments
   * @throws IOException if there was an error reading the file
   */
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage:  BallotCounter <filepath>");
      System.exit(1);
    }
    List<Set<Candidate>> list = null;
    try {
      list = readBallots(args[0]);
    } catch (IOException e) {
      System.err.println(e.getMessage());
      System.exit(2);
    }

    System.out.println("APPROVAL VOTING");
    System.out.println("=============================");
    for (Entry<Candidate, Integer> entry : sortDescByValue(countApproval(list))) {
      System.out.format("%1$-22s%2$7d\n", entry.getKey().getName(), entry.getValue());
    }

    System.out.println("\nPROPORTIONAL APPROVAL VOTING");
    System.out.println("=============================");
    Map<Candidate, Double> results = countProportionalApproval(list);
    LOGGER.log(Level.TRACE, "Final Results");
    LOGGER.log(Level.TRACE, "-------------");
    for (Entry<Candidate, Double> entry : sortDescByValue(results)) {
      System.out.format("%1$-22s%2$7.3f\n", entry.getKey().getName(), entry.getValue());
    }

    System.out.println("\nSATISFACTION APPROVAL VOTING");
    System.out.println("=============================");
    for (Entry<Candidate, Double> entry : sortDescByValue(countSatisfactionApproval(list))) {
      System.out.format("%1$-22s%2$7.3f\n", entry.getKey().getName(), entry.getValue());
    }
  }

  /**
   * Counts approval votes.
   *
   * @param ballots the {@code List} of ballots
   * @return the {@code Map} of {@code Candidate}s to votes
   */
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

  /**
   * Counts votes and returns proportional approval voting scores.
   *
   * @param ballots the {@code List} of ballots
   * @return the {@code Map} of {@code Candidate}s to scores
   */
  private static Map<Candidate, Double> countProportionalApproval(
      List<Set<Candidate>> ballots) {
    Map<Candidate, Double> results = new HashMap<>();
    for (int round = 1; round <= Candidate.count(); round++) {
      LOGGER.log(Level.TRACE, "Round {0}", round);
      Map<Candidate, Double> roundResults = new HashMap<>();
      for (Set<Candidate> ballot : ballots) {
        if (!ballot.isEmpty()) {
          int count = getIntersectionSize(ballot, results.keySet());
          for (Candidate c : ballot) {
            if (!results.containsKey(c)) {
              roundResults.put(c,
                  roundResults.getOrDefault(c, 0.0) + 1.0 / (1.0 + count));
            }
          }
        }
      }
      Set<Entry<Candidate, Double>> sortedResults =
          sortDescByValue(roundResults);
      if (LOGGER.isLoggable(Level.TRACE)) {
        for (Entry<Candidate, Double> entry : sortedResults) {
          LOGGER.log(Level.TRACE, "{0} {1}", entry.getKey().getName(),
              entry.getValue());
        }
      }
      Entry<Candidate, Double> winner = sortedResults.iterator().next();
      results.put(winner.getKey(), winner.getValue());
    }

    return results;
  }

  /**
   * Counts votes and returns satisfaction approval voting scores.
   *
   * @param ballots the {@code List} of ballots
   * @return the {@code Map} of {@code Candidate}s to scores
   */
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

  /**
   * Returns the size of the intersection between two sets.
   *
   * @param set1 {@code Set} 1
   * @param set2 {@code Set} 2
   * @return the size of the intersection
   */
  private static int getIntersectionSize(Set<?> set1, Set<?> set2) {
    Set<?> intersection = new HashSet<>(set1);
    intersection.retainAll(set2);
    return intersection.size();
  }

  /**
   * Reads the file from the given file path and returns the parsed ballots.
   *
   * @param filepathStr the file path
   * @return the {@code List} of ballots
   * @throws IOException if there was an error reading the file
   */
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

  /**
   * Sorts a {@code Map} by value and returns the sorted {@code Entry}s.
   * @param map the {@code Map}
   * @return the sorted {@code Entry}s
   */
  private static <K, V extends Comparable<? super V>> Set<Entry<K, V>> sortDescByValue(Map<K, V> map) {
    return map.entrySet().stream()
        .sorted(Entry.<K, V>comparingByValue().reversed())
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new))
        .entrySet();
  }

  /**
   * An election candidate.
   */
  private static class Candidate {

    private final String name;

    private static final Map<Integer, Candidate> lookup = new HashMap<>();

    Candidate(int i, String name) {
      this.name = name;
      lookup.put(i, this);
    }

    String getName() {
      return name;
    }

    /**
     * Returns the {@code Candidate} for the given index.
     *
     * @param i the index
     * @return the {@code Candidate}
     */
    static Candidate fromValue(Integer i) {
      return lookup.get(i);
    }

    /**
     * Returns the count of {@code Candidate}s.
     *
     * @return the count
     */
    static int count() {
      return lookup.size();
    }
  }
}
