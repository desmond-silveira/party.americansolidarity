/**
 *
 */
package dsilveira;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author dsilveira
 *
 */
class BallotCounterTest {

  @Test
  void testDroopQuota() {
    assertEquals(51, BallotCounter.getDroopQuota(100, 1));
    assertEquals(34, BallotCounter.getDroopQuota(100, 2));
    assertEquals(26, BallotCounter.getDroopQuota(100, 3));
    assertEquals(21, BallotCounter.getDroopQuota(100, 4));
    assertEquals(17, BallotCounter.getDroopQuota(100, 5));
    assertEquals(15, BallotCounter.getDroopQuota(100, 6));
    assertEquals(13, BallotCounter.getDroopQuota(100, 7));
  }

  @Test
  void testHagenbachBischoffQuota() {
    final double delta = 0.000001;
    assertEquals(50, BallotCounter.getHagenbachBischoffQuota(100, 1), delta);
    assertEquals(33.333333, BallotCounter.getHagenbachBischoffQuota(100, 2), delta);
    assertEquals(25, BallotCounter.getHagenbachBischoffQuota(100, 3), delta);
    assertEquals(20, BallotCounter.getHagenbachBischoffQuota(100, 4), delta);
    assertEquals(16.666667, BallotCounter.getHagenbachBischoffQuota(100, 5), delta);
    assertEquals(14.285714, BallotCounter.getHagenbachBischoffQuota(100, 6), delta);
    assertEquals(12.5, BallotCounter.getHagenbachBischoffQuota(100, 7), delta);
  }
}
