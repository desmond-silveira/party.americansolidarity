/**
 * Copyright © 2019 Desmond Silveira.  All rights reserved.
 *
 * This software is free to use and extend to all registered members of the
 * American Solidarity Party.  Any extensions must give credit to the author
 * and preserve this notice.
 */
package dsilveira;

import java.io.IOException;

/**
 * @author dsilveira
 */
public class SurveyMonkeyToBlt {

  /**
   * @param args
   */
  public static void main(String[] args) {
    if (args.length < 2) {
      System.out.println(
          "Usage:  SurveyMonkeyToBlt <filepath> <seat_count>");
      System.exit(1);
    }
    try {
      String filepath = args[0];
      SurveyMonkeyCsv csv = new SurveyMonkeyCsv(filepath);
      csv.read();
      String output = filepath.replaceAll(".csv$", ".blt");
      BltFile bltFile = new BltFile(output, Integer.parseInt(args[1]), csv.getBallots(), csv.getCandidates());
      bltFile.write();
    } catch (IOException e) {
      System.err.println(e);
      System.exit(2);
    }
  }
}
