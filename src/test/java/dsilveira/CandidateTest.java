/**
 *
 */
package dsilveira;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * @author dsilveira
 *
 */
class CandidateTest {

  @Test
  void testCompareTo() {
    Candidate a = new Candidate(4, "d");
    Candidate b = new Candidate(-2, "b");
    Candidate c = new Candidate(-1, "c");
    Candidate e = new Candidate(0, "e");
    Candidate d = new Candidate(1, "d");
    Candidate f = new Candidate(2, "f");
    Candidate g = new Candidate(-3, "d");

    Map<Candidate, Integer> map = new HashMap<>();
    map.put(a, 9);
    map.put(b, 9);
    map.put(c, 8);
    map.put(d, 9);
    map.put(e, 10);
    map.put(f, 9);
    map.put(g, 9);

    // e comes after d, by name, if counts aren't set.
    assertTrue(e.compareTo(d) > 0);
    Candidate.setCounts(map);

    // e:10 comes before d:9 because d has a higher count.
    assertTrue(e.compareTo(d) < 0);
    // c:8 comes after d:9 because c has a lower count.
    assertTrue(c.compareTo(d) > 0);
    // d:9 comes after b:9 because b has a lower name.
    assertTrue(d.compareTo(b) > 0);
    // d:9 comes before f:9 because f has a higher name.
    assertTrue(d.compareTo(f) < 0);
    // d:9 comes before a:9 because d has a lower value.
    assertTrue(d.compareTo(a) < 0);
    // d:9 comes after g:9 because d has a higher value.
    assertTrue(d.compareTo(g) > 0);

    List<Candidate> list = new ArrayList<>();
    list.add(a);
    list.add(b);
    list.add(c);
    list.add(d);
    list.add(e);
    list.add(f);
    list.add(g);
    Collections.sort(list);

    Iterator<Candidate> iter = list.iterator();
    assertEquals(e, iter.next());
    assertEquals(b, iter.next());
    assertEquals(g, iter.next());
    assertEquals(d, iter.next());
    assertEquals(a, iter.next());
    assertEquals(f, iter.next());
    assertEquals(c, iter.next());
  }
}
