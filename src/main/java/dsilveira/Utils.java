/**
 * Copyright © 2018 Desmond Silveira.  All rights reserved.
 *
 * This software is free to use and extend to all registered members of the
 * American Solidarity Party.  Any extensions must give credit to the author
 * and preserve this notice.
 */
package dsilveira;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author dsilveira
 *
 */
public class Utils {

  /**
   * Sorts a {@code Map} by value and returns the sorted {@code Entry}s.
   *
   * @param map the {@code Map}
   * @return the sorted {@code Entry}s
   */
  public static <K, V extends Comparable<? super V>> Set<Entry<K, V>> sortDescByValue(Map<K, V> map) {
    return map.entrySet().stream()
        .sorted(Entry.<K, V>comparingByValue().reversed())
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new))
        .entrySet();
  }

  /**
   * Sorts a {@code Set} by values and returns the {@code SortedSet}.
   *
   * @param map the {@code Map}
   * @return the sorted {@code Entry}s
   */
  public static <K> SortedSet<K> sortDescByValue(Set<K> set) {
    TreeSet<K> sortedSet = new TreeSet<K>();
    sortedSet.addAll(set);
    return sortedSet;
  }

  /**
   * Returns the sum of the harmonic series for the given value of n.
   *
   * @param n the input value
   * @return the sum
   */
  public static double harmonic(int n) {
    if (n < 1) {
      throw new IllegalArgumentException("The value of n must be positive for harmonic series.  n: " + n);
    }
    double sum = 0;
    for (int i = 1; i <= n; i++) {
      sum += 1.0 / i;
    }
    return sum;
  }

  /**
   * Returns the size of the intersection between two sets.
   *
   * @param set1 {@code Set} 1
   * @param set2 {@code Set} 2
   * @return the size of the intersection
   */
  public static int getIntersectionSize(Set<?> set1, Set<?> set2) {
    Set<?> intersection = new HashSet<>(set1);
    intersection.retainAll(set2);
    return intersection.size();
  }
}
