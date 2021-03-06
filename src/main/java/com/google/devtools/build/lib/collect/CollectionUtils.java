// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.collect;

import static com.google.common.collect.Sets.newEnumSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for collection classes.
 */
public final class CollectionUtils {

  private CollectionUtils() {}

  /**
   * Given a collection of elements and an equivalence relation, returns a new
   * unordered collection of the disjoint subsets of those elements which are
   * equivalent under the specified relation.
   *
   * <p>Note: the Comparator needs only to implement the less-strict contract
   * of EquivalenceRelation (q.v.).  (Hopefully this will one day be a
   * superinterface of Comparator.)
   *
   * @param elements the collection of elements to be partitioned.  May
   *   contain duplicates.
   * @param equivalenceRelation an equivalence relation over the elements.
   * @return a collection of sets of elements that are equivalent under the
   *   specified relation.
   */
  public static <T> Collection<Set<T>> partition(Collection<T> elements,
      Comparator<T> equivalenceRelation) {
    //  TODO(bazel-team): (2009) O(n*m) where n=|elements| and m=|eqClasses|; i.e.,
    //  quadratic.  Use Tarjan's algorithm instead.
    List<Set<T>> eqClasses = new ArrayList<>();
    for (T element : elements) {
      boolean found = false;
      for (Set<T> eqClass : eqClasses) {
        if (equivalenceRelation.compare(eqClass.iterator().next(),
                                        element) == 0) {
          eqClass.add(element);
          found = true;
          break;
        }
      }
      if (!found) {
        Set<T> eqClass = new HashSet<>();
        eqClass.add(element);
        eqClasses.add(eqClass);
      }
    }
    return eqClasses;
  }

  /**
   * See partition(Collection, Comparator).
   */
  public static <T> Collection<Set<T>> partition(Collection<T> elements,
      final EquivalenceRelation<T> equivalenceRelation) {
    return partition(elements, new Comparator<T>() {
      @Override
      public int compare(T o1, T o2) {
        return equivalenceRelation.compare(o1, o2);
      }
    });
  }

  /**
   * Returns the set of all elements in the given collection that appear more than once.
   * @param input some collection.
   * @return the set of repeated elements.  May return an empty set, but never null.
   */
  public static <T> Set<T> duplicatedElementsOf(Iterable<T> input) {
    Set<T> duplicates = new HashSet<>();
    Set<T> elementSet = new HashSet<>();
    for (T el : input) {
      if (!elementSet.add(el)) {
        duplicates.add(el);
      }
    }
    return duplicates;
  }

  /**
   * Returns an immutable list of all non-null parameters in the order in which
   * they are specified.
   */
  @SuppressWarnings("unchecked")
  public static <T> ImmutableList<T> asListWithoutNulls(T... elements) {
    ImmutableList.Builder<T> builder = ImmutableList.builder();
    for (T element : elements) {
      if (element != null) {
        builder.add(element);
      }
    }
    return builder.build();
  }

  /**
   * Returns true if the given iterable can be verified to be immutable.
   *
   * <p>Note that if this method returns false, that does not mean that the iterable is mutable.
   */
  public static <T> boolean isImmutable(Iterable<T> iterable) {
    return iterable instanceof ImmutableList<?>
        || iterable instanceof ImmutableSet<?>
        || iterable instanceof IterablesChain<?>
        || iterable instanceof NestedSet<?>
        || iterable instanceof ImmutableIterable<?>;
  }

  /**
   * Throws a runtime exception if the given iterable can not be verified to be immutable.
   */
  public static <T> void checkImmutable(Iterable<T> iterable) {
    Preconditions.checkState(isImmutable(iterable), iterable.getClass());
  }

  /**
   * Given an iterable, returns an immutable iterable with the same contents.
   */
  public static <T> Iterable<T> makeImmutable(Iterable<T> iterable) {
    if (isImmutable(iterable)) {
      return iterable;
    } else {
      return ImmutableList.copyOf(iterable);
    }
  }

  /**
   * Converts a set of enum values to a bit field. Requires that the enum contains at most 32
   * elements.
   */
  public static <T extends Enum<T>> int toBits(Set<T> values) {
    int result = 0;
    for (T value : values) {
      // <p>Note that when the 32. bit is set, the integer becomes negative (because that is the
      // sign bit). This does not affect the function of the bitwise operators, so it is fine.
      Preconditions.checkArgument(value.ordinal() < 32);
      result |= (1 << value.ordinal());
    }

    return result;
  }

  /**
   * Converts a set of enum values to a bit field. Requires that the enum contains at most 32
   * elements.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Enum<T>> int toBits(T... values) {
    return toBits(ImmutableSet.copyOf(values));
  }

  /**
   * Converts a bit field to a set of enum values. Requires that the enum contains at most 32
   * elements.
   */
  public static <T extends Enum<T>> EnumSet<T> fromBits(int value, Class<T> clazz) {
    T[] elements = clazz.getEnumConstants();
    Preconditions.checkArgument(elements.length <= 32);
    ArrayList<T> result = new ArrayList<>();
    for (T element : elements) {
      if ((value & (1 << element.ordinal())) != 0) {
        result.add(element);
      }
    }

    return newEnumSet(result, clazz);
  }

  /**
   * Returns whether an {@link Iterable} is a superset of another one.
   */
  public static <T> boolean containsAll(Iterable<T> superset, Iterable<T> subset) {
    return ImmutableSet.copyOf(superset).containsAll(ImmutableList.copyOf(subset));
  }

  /**
   * Returns an ImmutableMap of ImmutableMaps created from the Map of Maps parameter.
   */
  public static <KEY_1, KEY_2, VALUE> ImmutableMap<KEY_1, ImmutableMap<KEY_2, VALUE>> toImmutable(
      Map<KEY_1, Map<KEY_2, VALUE>> map) {
    ImmutableMap.Builder<KEY_1, ImmutableMap<KEY_2, VALUE>> builder = ImmutableMap.builder();
    for (Map.Entry<KEY_1, Map<KEY_2, VALUE>> entry : map.entrySet()) {
      builder.put(entry.getKey(), ImmutableMap.copyOf(entry.getValue()));
    }
    return builder.build();
  }

  /**
   * Returns a copy of the Map of Maps parameter.
   */
  public static <KEY_1, KEY_2, VALUE> Map<KEY_1, Map<KEY_2, VALUE>> copyOf(
      Map<KEY_1, ? extends Map<KEY_2, VALUE>> map) {
    Map<KEY_1, Map<KEY_2, VALUE>> result = new HashMap<>();
    for (Map.Entry<KEY_1, ? extends Map<KEY_2, VALUE>> entry : map.entrySet()) {
      result.put(entry.getKey(), new HashMap<>(entry.getValue()));
    }
    return result;
  }
}
