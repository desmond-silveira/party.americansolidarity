/**
 *
 */
package dsilveira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
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

    Map<Candidate, Double> map = new HashMap<>();
    map.put(a, 9.0);
    map.put(b, 9.0);
    map.put(c, 8.0);
    map.put(d, 9.0);
    map.put(e, 10.0);
    map.put(f, 9.0);
    map.put(g, 9.0);

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

  @Test
  void testAll() {
    new Candidate(4, "d");
    new Candidate(-2, "b");
    new Candidate(-1, "c");
    new Candidate(0, "e");

    Collection<Candidate> candidates = Candidate.all();
    assertEquals(4, candidates.size());

    new Candidate(1, "d");
    new Candidate(2, "f");
    assertEquals(6, candidates.size());

    assertThrows(RuntimeException.class, () -> {
      candidates.add(new Candidate(-3, "d"));
    });

    assertEquals(7, candidates.size());
  }
}
