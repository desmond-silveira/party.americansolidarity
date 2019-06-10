/**
 * Copyright © 2018 Desmond Silveira.  All rights reserved.
 *
 * This software is free to use and extend to all registered members of the
 * American Solidarity Party.  Any extensions must give credit to the author
 * and preserve this notice.
 */
package dsilveira;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * An election candidate.
 *
 * @author dsilveira
 */
public class Candidate implements Comparable<Candidate> {

  private final String name;
  private final String lastName;
  private final int i;

  private static final Map<Integer, Candidate> lookup = new TreeMap<>();
  private static Map<Candidate, Integer> counts;
  private static int maxLength = 0;

  Candidate(int i, String name) {
    this.name = name;
    this.lastName = name.substring(name.lastIndexOf(" ") + 1);
    this.i = i;
    lookup.put(i, this);
    maxLength = Math.max(name.length(), maxLength);
  }

  /**
   * @return the name of the {@code Candidate}
   */
  String getName() {
    return name;
  }

  /**
   * @return the last name (last space-delimited token) of the {@code Candidate}
   */
  String getLastName() {
    return lastName;
  }

  /**
   * Returns the {@code Candidate} for the given index.
   *
   * @param i the index
   * @return the {@code Candidate}
   */
  static Candidate fromValue(int i) {
    return lookup.get(i);
  }

  /**
   * @param vals an array of {@code Candidate} values
   * @return the {@code Set} of {@code Candidate}s with those values
   */
  static Set<Candidate> fromValues(int[] vals) {
    Set<Candidate> candidates = new HashSet<>(vals.length);
    for (int i : vals) {
      candidates.add(fromValue(i));
    }
    return candidates;
  }

  /**
   * Returns a {@code Collection} of all {@code Candidate}s sorted by index.
   * This is backed by the internal {@code Collection} of {@code Candidate}s, so
   * as more {@code Candidate}s are added, they are reflected in the
   * {@code Collection} returned.
   *
   * @return a {@code Collection} of all {@code Candidate}s
   */
  static Collection<Candidate> all() {
    return lookup.values();
  }

  /**
   * Sets the {@code Map} of counts used to compare {@code Candidate}s.
   *
   * @param counts the {@code Map} of counts
   */
  static void setCounts(Map<Candidate, Integer> counts) {
    Candidate.counts = counts;
  }

  /**
   * Returns the count of {@code Candidate}s.
   *
   * @return the count
   */
  static int count() {
    return lookup.size();
  }

  /**
   * @return the maximum name length among the candidates
   */
  static int maxLength() {
    return maxLength;
  }

  /**
   * {@inheritDoc}
   *
   * @return the {@code Candidate} name
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return name;
  }

  /**
   * Orders candidates by counts, if set, then by name, and then by value.
   *
   * @see #setCounts(Map)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(Candidate o) {
    int ret = 0;
    if (counts != null && counts.get(this) != null && counts.get(o) != null) {
      ret = counts.get(o).compareTo(counts.get(this));
    }
    if (ret == 0) {
      ret = name.compareToIgnoreCase(o.name);
    }
    if (ret == 0) {
      ret = i - o.i;
    }
    return ret;
  }
}