package dsilveira;

import static dsilveira.Utils.sortDescByValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A SurveyMonkey export in CSV format.
 *
 * @author dsilveira
 */
public class SurveyMonkeyCsv {

  private String filepath;
  private Candidate[] candidates;
  private Map<List<Integer>, Integer> ballots = new HashMap<>();

  public SurveyMonkeyCsv(String filepath) {
    this.filepath = filepath;
  }

  public void read() throws IOException {
    Path path = Paths.get(filepath);
    List<String> lines = Files.readAllLines(path);
    String[] candidates = lines.get(0).split(",");
    for (int i = 0; i < candidates.length; i++) {
      new Candidate(i + 1, candidates[i]);
    }
    this.candidates = Candidate.all().toArray(new Candidate[candidates.length]);
    List<Map<Candidate, Double>> ballotValuesList =
        lines.stream().skip(1).map(mapper).collect(Collectors.toList());

    for (Map<Candidate, Double> ballotValues : ballotValuesList) {
      List<Integer> ballot = new ArrayList<>();
      for (Entry<Candidate, Double> entry : sortDescByValue(ballotValues)) {
        ballot.add(entry.getKey().getIndex());
      }
      Collections.reverse(ballot);
      ballots.put(ballot, ballots.getOrDefault(ballot, 0) + 1);
    }
  }

  private static Function<String, Map<Candidate, Double>> mapper = (line) -> {
    Map<Candidate, Double> ballot = new HashMap<>();
    String[] votes = line.split(",");
    for (int i = 0; i < votes.length; i++) {
      if (votes[i] != null && !votes[i].isEmpty()) {
        ballot.put(Candidate.fromValue(i + 1), Double.valueOf(votes[i]));
      }
    }
    return ballot;
  };

  public Map<List<Integer>, Integer> getBallots() {
    return ballots;
  }

  public Candidate[] getCandidates() {
    return candidates;
  }
}
