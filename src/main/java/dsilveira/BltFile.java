package dsilveira;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A BLT file is a file, first described by Hill, Wichmann & Woodall in
 * "Algorithm 123 - Single Transferable Vote by Meek's method" (1987), for
 * capturing election results and metadata, particularly well-suited for STV
 * elections.
 *
 * @author dsilveira
 */
public class BltFile {

  private String filepath;
  private String title;
  private Candidate[] candidates;
  private Set<Integer> withdrawn = new HashSet<>();
  private int candidateCount;
  private int seatCount;
  private Map<List<Integer>, Integer> ballots = new HashMap<>();
  private List<LinkedHashSet<Candidate>> candidateBallots = null;

  public BltFile(String filepath) {
    this.filepath = filepath;
  }

  public void read() throws IOException {
    Path path = Paths.get(filepath);
    BufferedReader reader = Files.newBufferedReader(path);
    StreamTokenizer tokenizer = new StreamTokenizer(reader);
    tokenizer.commentChar('#');
    tokenizer.quoteChar('"');
    tokenizer.slashStarComments(true);

    if (tokenizer.nextToken() == StreamTokenizer.TT_WORD
        && tokenizer.sval.startsWith("\uFEFF")) {
      String token = tokenizer.sval.substring(1);
      if (token.isEmpty()) {
        tokenizer.nextToken();
        candidateCount = (int) tokenizer.nval;
      } else {
        candidateCount = Integer.parseInt(token);
      }
    } else {
      candidateCount = (int) tokenizer.nval;
    }

    tokenizer.nextToken();
    seatCount = (int) tokenizer.nval;

    tokenizer.nextToken();
    while (tokenizer.nval < 0) {
      withdrawn.add((int) -tokenizer.nval);
      tokenizer.nextToken();
    }

    do {
      int weight = (int) tokenizer.nval;
      tokenizer.nextToken();
      List<Integer> ballot = new ArrayList<>();
      while (tokenizer.nval != 0) {
        ballot.add((int) tokenizer.nval);
        tokenizer.nextToken();
      }
      ballots.put(ballot, ballots.getOrDefault(ballot, 0) + weight);
      tokenizer.nextToken();
    } while (tokenizer.nval != 0);

    candidates = new Candidate[candidateCount - withdrawn.size()];
    int index = 0;
    for (int i = 0; i < candidateCount; i++) {
      tokenizer.nextToken();
      if (!withdrawn.contains(i + 1)) {
        candidates[index] = new Candidate(i + 1, tokenizer.sval);
        index++;
      }
    }

    tokenizer.nextToken();
    title = tokenizer.sval;
  }

  public List<LinkedHashSet<Candidate>> getBallots() {
    if (candidateBallots != null) {
      return candidateBallots;
    }
    candidateBallots = new ArrayList<>();
    for (Map.Entry<List<Integer>, Integer> ballotConfig : ballots.entrySet()) {
      LinkedHashSet<Candidate> ballot = new LinkedHashSet<>();
      for (Integer i : ballotConfig.getKey()) {
        if (!withdrawn.contains(i)) {
          ballot.add(Candidate.fromValue(i));
        }
      }
      for (int i = 0; i < ballotConfig.getValue(); i++) {
        candidateBallots.add(ballot);
      }
    }
    return candidateBallots;
  }

  public int getSeatCount() {
    return seatCount;
  }

  public String getTitle() {
    return title;
  }
}
