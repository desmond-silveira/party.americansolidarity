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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Combinations;
import org.apache.commons.math3.util.CombinatoricsUtils;

import me.tongfei.progressbar.ProgressBar;

/**
 * This program reads either approval-style ballots from a CSV file or
 * ranked-style ballots from a BLT file.  It returns the tallied results to the
 * console using
 * <ul>
 * <li><a href="https://en.wikipedia.org/wiki/Approval_voting">Approval Voting</a></li>
 * <li>Net Approval Voting</li>
 * <li><a href="https://en.wikipedia.org/wiki/Satisfaction_approval_voting">Satisfaction Approval Voting</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Proportional_approval_voting">Proportional Approval Voting</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Sequential_proportional_approval_voting">Sequential Proportional Approval Voting</a></li>
 * <li><a href="https://en.wikipedia.org/wiki/Single_transferable_vote">Single Transferable Vote</a>
 *  using <a href="https://en.wikipedia.org/wiki/Droop_quota">Droop quota</a>
 *  and <a href="https://en.wikipedia.org/wiki/Wright_system">Wright redistribution</li>
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
 * The BLT file format is that first described by Hill, Wichmann & Woodall in
 * "Algorithm 123 - Single Transferable Vote by Meek's method" (1987) and
 * popularized by OpenSTV.
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
    List<? extends Set<Candidate>> approvalBallots = null;
    List<LinkedHashSet<Candidate>> rankedBallots = null;
    Map<Candidate, Integer> approvalCounts = null;
    BltFile bltFile = null;
    try {
      String filepath = args[0];
      if (filepath.substring(filepath.length() - 4).toLowerCase().equals(".blt")) {
        bltFile = new BltFile(filepath);
        bltFile.read();
        rankedBallots = bltFile.getBallots();
        approvalBallots = simulateApprovalVoting(rankedBallots, bltFile.getSeatCount());
        approvalCounts = countAV(approvalBallots);
        Candidate.setCounts(approvalCounts);
      } else {
        approvalBallots = readApprovalBallots(filepath);
        approvalCounts = countAV(approvalBallots);
        Candidate.setCounts(approvalCounts);
        rankedBallots = simulateRankedVoting(approvalBallots);
      }
    } catch (IOException e) {
      System.err.println(e);
      System.exit(2);
    }

    int seatCount = 1;
    if (args.length >= 2) {
      seatCount = Integer.parseInt(args[1]);
    } else if (bltFile != null) {
      seatCount = bltFile.getSeatCount();
    }

    int maxProportionalSlates = 10;
    if (args.length >= 3) {
      maxProportionalSlates = Integer.parseInt(args[2]);
    }

    int nameFieldLength = Math.max(Candidate.maxLength(), 31);

    System.out.println("\nAPPROVAL VOTING");
    System.out.println("=======================================");
    for (Entry<Candidate, Integer> candidateResult : sortDescByValue(approvalCounts)) {
      System.out.format("%1$-" + nameFieldLength + "s%2$8d\n",
          candidateResult.getKey().getName(), candidateResult.getValue());
    }

    System.out.println("\nNET APPROVAL VOTING");
    System.out.println("=======================================");
    for (Entry<Candidate, Integer> candidateResult : sortDescByValue(countNetAV(approvalBallots))) {
      System.out.format("%1$-" + nameFieldLength + "s%2$8d\n",
          candidateResult.getKey().getName(), candidateResult.getValue());
    }

    System.out.println("\nSATISFACTION APPROVAL VOTING");
    System.out.println("=======================================");
    for (Entry<Candidate, Double> candidateResult : sortDescByValue(countSAV(approvalBallots))) {
      System.out.format("%1$-" + nameFieldLength + "s%2$8.3f\n",
          candidateResult.getKey().getName(), candidateResult.getValue());
    }

    System.out.println("\nPROPORTIONAL APPROVAL VOTING");
    System.out.println("=======================================");
    Map<Set<Candidate>, Double> pavResults = countPAV(approvalBallots, seatCount);
    LOGGER.log(Level.TRACE, "Final Results");
    LOGGER.log(Level.TRACE, "-------------");
    for (ListIterator<Entry<Set<Candidate>, Double>> iter =
        (new ArrayList<Entry<Set<Candidate>, Double>>(sortDescByValue(pavResults))).listIterator();
        iter.hasNext() && iter.nextIndex() < maxProportionalSlates;) {
      Entry<Set<Candidate>, Double> slateResult = iter.next();
      System.out.format("%1$-" + (Candidate.maxLength() * seatCount) + "s%2$8.3f\n",
          new TreeSet<>(slateResult.getKey()), slateResult.getValue());
    }

    System.out.println("\nSEQUENTIAL PROPORTIONAL APPROVAL VOTING");
    System.out.println("=======================================");
    Map<Candidate, Double> spavResults = countSPAV(approvalBallots);
    LOGGER.log(Level.TRACE, "Final Results");
    LOGGER.log(Level.TRACE, "-------------");
    for (Entry<Candidate, Double> candidateResult : sortDescByValue(spavResults)) {
      System.out.format("%1$-" + nameFieldLength + "s%2$8.3f\n",
          candidateResult.getKey().getName(), candidateResult.getValue());
    }

//    System.out.println("\nSILVEIRA SEQUENTIAL PROPORTIONAL APPROVAL VOTING");
//    System.out.println("=======================================");
//    Map<Candidate, Double> sspavResults = countSSPAV(approvalBallots, seatCount);
//    LOGGER.log(Level.TRACE, "Final Results");
//    LOGGER.log(Level.TRACE, "-------------");
//    for (Entry<Candidate, Double> candidateResult : sortDescByValue(sspavResults)) {
//      System.out.format("%1$-" + nameFieldLength + "s%2$8.3f\n",
//          candidateResult.getKey().getName(), candidateResult.getValue());
//    }
//
    System.out.println("\nSINGLE TRANSFERABLE VOTE");
    System.out.println("=======================================");
    List<Candidate> stvResults = countSTV(rankedBallots, seatCount);
    System.out.println("Final Results");
    System.out.println("-------------");
    for (Candidate candidate : stvResults) {
      System.out.format("%1$-" + nameFieldLength + "s\n", candidate.getName());
    }
  }

  private static List<LinkedHashSet<Candidate>> simulateRankedVoting(
      List<? extends Set<Candidate>> ballots) {
    List<LinkedHashSet<Candidate>> results = new ArrayList<>(ballots.size());
    for (Set<Candidate> ballot : ballots) {
      results.add(new LinkedHashSet<Candidate>(new TreeSet<Candidate>(ballot)));
    }
    return results;
  }

  private static List<Set<Candidate>> simulateApprovalVoting(
      List<? extends Set<Candidate>> ballots, int approvalThreshold) {
    List<Set<Candidate>> results = new ArrayList<>(ballots.size());
    for (Set<Candidate> ballot : ballots) {
      Set<Candidate> newBallot = new HashSet<>();
      int i = 0;
      for (Candidate candidate : ballot) {
        if (i == approvalThreshold) {
          break;
        }
        newBallot.add(candidate);
        i++;
      }
      results.add(newBallot);
    }
    return results;
  }

  /**
   * Counts approval votes.
   *
   * @param ballots the {@code List} of ballots
   * @return the {@code Map} of {@code Candidate}s to votes
   */
  public static Map<Candidate, Integer> countAV(List<? extends Set<Candidate>> ballots) {
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
  public static Map<Candidate, Integer> countNetAV(List<? extends Set<Candidate>> ballots) {
    Map<Candidate, Integer> results = new HashMap<>();
    for (Set<Candidate> ballot : ballots) {
      if (!ballot.isEmpty()) {
        for (Candidate c : Candidate.all()) {
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
  public static Map<Candidate, Double> countSAV(List<? extends Set<Candidate>> ballots) {
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
  public static Map<Candidate, Double> countSPAV(
      List<? extends Set<Candidate>> ballots) {
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
   * Counts votes and returns the single transferable voting winners.
   * <p/>
   * This uses the more popular Droop Quota, favoring larger parties, over the
   * Hare quota that favors smaller parties.
   *
   * @param ballots
   * @param seatCount
   * @return
   * @see <http href="https://en.wikipedia.org/wiki/Wright_system">Wright System - Wikipedia</a>
   */
  public static List<Candidate> countSTV(
      List<LinkedHashSet<Candidate>> rankedBallots, int seatCount) {

    List<LinkedHashSet<Candidate>> ballots = new ArrayList<>(rankedBallots.size());
    for (LinkedHashSet<Candidate> rankedBallot : rankedBallots) {
      // STV requires that blank ballots not be counted, so as not to affect the
      // quota.
      if (!rankedBallot.isEmpty()) {
        ballots.add(rankedBallot);
      }
    }

    List<Candidate> elected = new ArrayList<>(seatCount);
    Set<Candidate> excluded = new HashSet<>(Candidate.count() - seatCount);
    Map<Candidate, Double> candidateTotalValueOfVotes = countFirstChoiceVotes(ballots);

    final double quota = getHagenbachBischoffQuota(ballots.size(), seatCount);
    System.out.format("Quota: %1$.2f\n", quota);

    System.out.print("First preferences: ");
    for (Entry<Candidate, Double> entry : sortDescByValue(candidateTotalValueOfVotes)) {
      System.out.format("%1$s: %2$.0f; ", entry.getKey().getLastName(), entry.getValue());
    }
    System.out.println();

    // This algorithm determines all candidates to be seated in this round
    // before transferring any of their votes.
    while (elected.size() < seatCount) {
      List<Entry<Candidate, Double>> sorted =
          new ArrayList<>(sortDescByValue(candidateTotalValueOfVotes));
      // When there is a tie in votes among qualified candidates, the order that
      // their votes are redistributed is non-deterministic, and the actual
      // order used may affect the outcome.
      List<Candidate> provisionals = getProvisionalCandidates(sorted, quota);
      elected.addAll(provisionals);
      if (!provisionals.isEmpty()) {
        for (Candidate provisional : provisionals) {
          double redistributionVotes = candidateTotalValueOfVotes.get(provisional) - quota;
          redistribute(provisional, redistributionVotes, candidateTotalValueOfVotes,
              ballots, seatCount, elected, excluded, "Seat");
        }
      } else {
        Entry<Candidate, Double> lowest = sorted.get(sorted.size() - 1);
        Candidate candidate = lowest.getKey();
        double redistributionVotes = lowest.getValue();
        excluded.add(candidate);
        redistribute(candidate, redistributionVotes, candidateTotalValueOfVotes,
            ballots, seatCount, elected, excluded, "Eliminate");
      }
    }

    return elected;
  }

  /**
   * Returns the Droop quota.
   * <p/>
   * This is considered the best quota for use with IRV elections.  Any
   * candidate that attains a vote count that equals or exceeds this quota
   * should be elected.
   *
   * @param ballotCount the count of non-blank, unspoiled ballots
   * @param seatCount the count of seats to be filled
   * @return the Droop quota
   */
  public static int getDroopQuota(int ballotCount, int seatCount) {
    return (int) ((double) ballotCount / (seatCount + 1)) + 1;
  }

  /**
   * Returns the Hagenbach-Bischoff quota.
   * <p/>
   * This is considered the best quota for use with STV elections.  Any
   * candidate that attains a vote count that exceeds this quota should be
   * elected.
   *
   * @param ballotCount the count of non-blank, unspoiled ballots
   * @param seatCount the count of seats to be filled
   * @return the Hagenbach-Bischoff quota
   */
  public static double getHagenbachBischoffQuota(int ballotCount, int seatCount) {
    return (double) ballotCount / (seatCount + 1);
  }

  /**
   * Return the count of first choice votes for all {@code Candidate}s.
   *
   * @param ballots
   * @param seated
   * @return
   */
  private static Map<Candidate, Double> countFirstChoiceVotes(
      List<LinkedHashSet<Candidate>> ballots) {
    Map<Candidate, Double> votes = new HashMap<>(Candidate.count());
    for (Set<Candidate> ballot : ballots) {
      if (!ballot.isEmpty()) {
        Candidate candidate = ballot.iterator().next();
        votes.put(candidate, votes.getOrDefault(candidate, 0.0) + 1);
      }
    }
    // Add any candidates that didn't get any first choice votes.
    for (Candidate candidate : Candidate.all()) {
      if (!votes.containsKey(candidate)) {
        votes.put(candidate, 0.0);
      }
    }
    return votes;
  }

  /**
   * Returns the {@code Candidate}s that are qualified to be seated in this round.
   *
   * @param sorted {@code List} of {@code Candidate}s sorted by votes
   * @param quota the qualification threshold
   * @return the {@code Candidate}s
   */
  private static List<Candidate> getProvisionalCandidates(
      List<Entry<Candidate, Double>> sorted, double quota) {
    List<Candidate> qualified = new ArrayList<>();
    for (Entry<Candidate, Double> entry : sorted) {
      if (entry.getValue() > quota) {
        qualified.add(entry.getKey());
      }
    }
    return qualified;
  }

  /**
   * Redistribute votes via the Wright system.
   *
   * @param candidate the {@code Candidate} whose votes to redistribute
   * @param redistributionVotes the number of votes to redistribute
   * @param candidateTotalValueOfVotes the running total value of votes for each {@code Candidate}
   * @param ballots the ballots
   * @param seatCount the total number of seats to fill
   * @param elected the {@code Candidate}s that are already designated as elected
   * @param excluded the {@code Candidate}s that are already designated as not eliminated
   * @param action whether this redistribution is to "Seat" or "Eliminate" a {@code Candidate}
   */
  private static void redistribute(Candidate candidate, double redistributionVotes,
      Map<Candidate, Double> candidateTotalValueOfVotes,
      List<LinkedHashSet<Candidate>> ballots, int seatCount,
      List<Candidate> elected, Set<Candidate> excluded, String action) {
    System.out.format("%1$s, %2$s, %3$.1f; ", action, candidate.getLastName(),
        candidateTotalValueOfVotes.get(candidate));
    if (elected.size() < seatCount && redistributionVotes > 0) {
      System.out.format("Redistribute %1$.1f votes; ", redistributionVotes);
      Map<Candidate, Double> redistributionCandidates =
          getRedistributionCandidates(ballots, elected, excluded, candidate);
      for (Entry<Candidate, Double> nextChoice : redistributionCandidates.entrySet()) {
        Candidate redistributionCandidate = nextChoice.getKey();
        double redistribution = nextChoice.getValue() * redistributionVotes
            / getTotalVoteValue(redistributionCandidates);
        double current = candidateTotalValueOfVotes.getOrDefault(redistributionCandidate, 0.0);
        double total = redistribution + current;
        System.out.format("%1$s: %2$.1f+%3$.1f=%4$.1f; ",
            redistributionCandidate.getLastName(),
            candidateTotalValueOfVotes.getOrDefault(redistributionCandidate, 0.0),
            redistribution, total);
        candidateTotalValueOfVotes.put(candidate,
            candidateTotalValueOfVotes.get(candidate) - redistribution);
        candidateTotalValueOfVotes.put(redistributionCandidate, total);
      }
    }
    System.out.println();
    candidateTotalValueOfVotes.remove(candidate);
  }

  private static Map<Candidate, Double> getRedistributionCandidates(
      List<LinkedHashSet<Candidate>> ballots, Collection<Candidate> seated,
      Collection<Candidate> eliminated, Candidate candidate) {
    Map<Candidate, Double> nextChoices = new HashMap<>(Candidate.count());
    for (Set<Candidate> ballot : ballots) {
      boolean candidateFound = false;
      for (Candidate c : ballot) {
        if (candidateFound && !seated.contains(c) && !eliminated.contains(c)) {
          nextChoices.put(c, nextChoices.getOrDefault(c, 0.0) + 1);
          break;
        } else if (c.equals(candidate)) {
          candidateFound = true;
        } else if (!seated.contains(c) && !eliminated.contains(c)) {
          // Not a candidate to redistribute votes to.
          break;
        }
      }
    }
    return nextChoices;
  }

  private static double getTotalVoteValue(Map<Candidate, Double> redistributionCandidates) {
    return redistributionCandidates.values().stream().mapToDouble(i -> i).sum();
  }

  /**
   * Counts votes and returns sequential proportional approval voting scores.
   *
   * @param ballots the {@code List} of ballots
   * @return the {@code Map} of {@code Candidate}s to scores
   */
  private static Map<Candidate, Double> countSSPAV(
      List<? extends Set<Candidate>> ballots, int seatCount) {
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
                  roundResults.getOrDefault(c, 0.0) + Math.max(1 - (double) count / seatCount, 0.0));
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
  public static Map<Set<Candidate>, Double> countPAV(
      List<? extends Set<Candidate>> ballots, int seatCount) {
    long comboCount = CombinatoricsUtils.binomialCoefficient(Candidate.count(), seatCount);
    int mapCapacity = Integer.MAX_VALUE;
    if (comboCount < mapCapacity) {
      mapCapacity = (int) comboCount;
    }
    Map<Set<Candidate>, Double> results = new HashMap<>(mapCapacity);

    Iterable<? extends Set<Candidate>> iter = ballots;
    if (comboCount > MIN_COMBO_COUNT_FOR_PROGRESS_BAR) {
      iter = ProgressBar.wrap(ballots, "Tallying");
    }
    for (Set<Candidate> ballot : iter) {
      if (!ballot.isEmpty()) {
        for (int[] slateValues : new Combinations(Candidate.count(), seatCount)) {
          Set<Candidate> slate = Candidate.fromValues(slateValues);
          int count = getIntersectionSize(ballot, slate);
          if (count > 0) {
            results.put(slate, results.getOrDefault(slate, 0.0) + harmonic(count));
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
  public static List<Set<Candidate>> readApprovalBallots(String filepathStr) throws IOException {
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
