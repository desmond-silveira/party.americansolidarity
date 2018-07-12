/**
 * Copyright © 2018 Desmond Silveira.  All rights reserved.
 *
 * This software is free to use and extend to all registered members of the
 * American Solidarity Party.  Any extensions must give credit to the author
 * and preserve this notice.
 */

package dsilveira;

import static dsilveira.Utils.getIntersectionSize;
import static dsilveira.Utils.harmonic;
import static dsilveira.Utils.sortDescByValue;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Combinations;
import org.apache.commons.math3.util.CombinatoricsUtils;

import me.tongfei.progressbar.ProgressBar;

/**
 * This program reads a CSV file of ballots and returns the tallied results
 * using
 * <ul>
 * <li><a href="https://en.wikipedia.org/wiki/Approval_voting">Approval Voting</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Proportional_approval_voting">Proportional Approval Voting</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Satisfaction_approval_voting">Satisfaction Approval Voting</a></li>
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

  private static final System.Logger LOGGER =
      System.getLogger(BallotCounter.class.getName());
  private static final int MIN_COMBO_COUNT_FOR_PROGRESS_BAR = 3000;

  /**
   * Reads ballot file, tallies ballots, and prints results.
   *
   * @param args command line arguments
   * @throws IOException if there was an error reading the file
   */
  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println(
          "Usage:  BallotCounter <filepath> [seat_count] [max_proportional_slates]");
      System.exit(1);
    }
    List<Set<Candidate>> ballots = null;
    try {
      ballots = readBallots(args[0]);
    } catch (IOException e) {
      System.err.println(e);
      System.exit(2);
    }

    int seatCount = 1;
    if (args.length >= 2) {
      seatCount = Integer.parseInt(args[1]);
    }

    int maxProportionalSlates = 10;
    if (args.length >= 3) {
      maxProportionalSlates = Integer.parseInt(args[2]);
    }

    Map<Candidate, Integer> approvalCounts = countAV(ballots);
    Candidate.setCounts(approvalCounts);

    int nameFieldLength = Math.max(Candidate.maxLength(), 31);

    System.out.println("APPROVAL VOTING");
    System.out.println("=======================================");
    for (Entry<Candidate, Integer> candidateResult : sortDescByValue(approvalCounts)) {
      System.out.format("%1$-" + nameFieldLength + "s%2$8d\n",
          candidateResult.getKey().getName(), candidateResult.getValue());
    }

    System.out.println("\nNET APPROVAL VOTING");
    System.out.println("=======================================");
    for (Entry<Candidate, Integer> candidateResult : sortDescByValue(countNetAV(ballots))) {
      System.out.format("%1$-" + nameFieldLength + "s%2$8d\n",
          candidateResult.getKey().getName(), candidateResult.getValue());
    }

    System.out.println("\nSATISFACTION APPROVAL VOTING");
    System.out.println("=======================================");
    for (Entry<Candidate, Double> candidateResult : sortDescByValue(countSAV(ballots))) {
      System.out.format("%1$-" + nameFieldLength + "s%2$8.3f\n",
          candidateResult.getKey().getName(), candidateResult.getValue());
    }

    System.out.println("\nSEQUENTIAL PROPORTIONAL APPROVAL VOTING");
    System.out.println("=======================================");
    Map<Candidate, Double> spavResults = countSPAV(ballots);
    LOGGER.log(Level.TRACE, "Final Results");
    LOGGER.log(Level.TRACE, "-------------");
    for (Entry<Candidate, Double> candidateResult : sortDescByValue(spavResults)) {
      System.out.format("%1$-" + nameFieldLength + "s%2$8.3f\n",
          candidateResult.getKey().getName(), candidateResult.getValue());
    }

    System.out.println("\nPROPORTIONAL APPROVAL VOTING");
    System.out.println("=======================================");
    Map<Set<Candidate>, Double> pavResults = countPAV(ballots, seatCount);
    LOGGER.log(Level.TRACE, "Final Results");
    LOGGER.log(Level.TRACE, "-------------");
    for (ListIterator<Entry<Set<Candidate>, Double>> iter =
        (new ArrayList<Entry<Set<Candidate>, Double>>(sortDescByValue(pavResults))).listIterator();
        iter.hasNext() && iter.nextIndex() < maxProportionalSlates;) {
      Entry<Set<Candidate>, Double> slateResult = iter.next();
      System.out.format("%1$-" + (Candidate.maxLength() * seatCount) + "s%2$8.3f\n",
          sortDescByValue(slateResult.getKey()), slateResult.getValue());
    }
  }

  /**
   * Counts approval votes.
   *
   * @param ballots the {@code List} of ballots
   * @return the {@code Map} of {@code Candidate}s to votes
   */
  private static Map<Candidate, Integer> countAV(List<Set<Candidate>> ballots) {
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
   * Counts net approval votes.
   *
   * @param ballots the {@code List} of ballots
   * @return the {@code Map} of {@code Candidate}s to votes
   */
  private static Map<Candidate, Integer> countNetAV(List<Set<Candidate>> ballots) {
    Map<Candidate, Integer> results = new HashMap<>();
    for (Set<Candidate> ballot : ballots) {
      if (!ballot.isEmpty()) {
        for (int i = 0; i < Candidate.count(); i++) {
          Candidate c = Candidate.fromValue(i);
          int points = ballot.contains(c) ? 1 : -1;
          results.put(c, results.getOrDefault(c, 0) + points);
        }
      }
    }
    return results;
  }

  /**
   * Counts votes and returns satisfaction approval voting scores.
   *
   * @param ballots the {@code List} of ballots
   * @return the {@code Map} of {@code Candidate}s to scores
   */
  private static Map<Candidate, Double> countSAV(List<Set<Candidate>> ballots) {
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
   * Counts votes and returns sequential proportional approval voting scores.
   *
   * @param ballots the {@code List} of ballots
   * @return the {@code Map} of {@code Candidate}s to scores
   */
  private static Map<Candidate, Double> countSPAV(
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
      if (roundResults.isEmpty()) {
        break;
      }
      Set<Entry<Candidate, Double>> sortedResults = sortDescByValue(roundResults);
      if (LOGGER.isLoggable(Level.TRACE)) {
        for (Entry<Candidate, Double> entry : sortedResults) {
          LOGGER.log(Level.TRACE, "{0} {1}", entry.getKey().getName(),
              entry.getValue());
        }
      }
      // Seat the winner (or winners if there is a tie) of the round.
      double topRoundScore = 0;
      for (Entry<Candidate, Double> nextTop : sortedResults) {
        if (topRoundScore == 0) {
          topRoundScore = nextTop.getValue();
        }
        if (nextTop.getValue() == topRoundScore) {
          results.put(nextTop.getKey(), nextTop.getValue());
        } else {
          break;
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
  private static Map<Set<Candidate>, Double> countPAV(
      List<Set<Candidate>> ballots, int seatCount) {
    long comboCount = CombinatoricsUtils.binomialCoefficient(Candidate.count(), seatCount);
    int mapCapacity = Integer.MAX_VALUE;
    if (comboCount < mapCapacity) {
      mapCapacity = (int) comboCount;
    }
    Map<Set<Candidate>, Double> results = new HashMap<>(mapCapacity);

    Iterable<Set<Candidate>> iter = ballots;
    if (comboCount > MIN_COMBO_COUNT_FOR_PROGRESS_BAR) {
      iter = ProgressBar.wrap(ballots, "Tallying");
    }
    for (Set<Candidate> ballot : iter) {
      if (!ballot.isEmpty()) {
        for (int[] slateValues : new Combinations(Candidate.count(), seatCount)) {
          Set<Candidate> slate = Candidate.fromValues(slateValues);
          int intersectionSize = getIntersectionSize(ballot, slate);
          if (intersectionSize > 0) {
            results.put(slate, results.getOrDefault(slate, 0.0) + harmonic(intersectionSize));
          }
        }
      }
    }
    return results;
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
}
