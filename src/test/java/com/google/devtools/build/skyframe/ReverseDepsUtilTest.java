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
package com.google.devtools.build.skyframe;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test for {@code ReverseDepsUtil}.
 */
@RunWith(Parameterized.class)
public class ReverseDepsUtilTest {

  private static final SkyFunctionName NODE_TYPE = SkyFunctionName.create("Type");
  private final int numElements;

  @Parameters
  public static List<Object[]> paramenters() {
    List<Object[]> params = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      params.add(new Object[]{i});
    }
    return params;
  }

  public ReverseDepsUtilTest(int numElements) {
    this.numElements = numElements;
  }

  private static final ReverseDepsUtil<Example> REVERSE_DEPS_UTIL = new ReverseDepsUtil<Example>() {
    @Override
    void setReverseDepsObject(Example container, Object object) {
      container.reverseDeps = object;
    }

    @Override
    void setSingleReverseDep(Example container, boolean singleObject) {
      container.single = singleObject;
    }

    @Override
    void setReverseDepsToRemove(Example container, List<SkyKey> object) {
      container.reverseDepsToRemove = object;
    }

    @Override
    Object getReverseDepsObject(Example container) {
      return container.reverseDeps;
    }

    @Override
    boolean isSingleReverseDep(Example container) {
      return container.single;
    }

    @Override
    List<SkyKey> getReverseDepsToRemove(Example container) {
      return container.reverseDepsToRemove;
    }
  };

  private class Example {

    Object reverseDeps = ImmutableList.of();
    boolean single;
    List<SkyKey> reverseDepsToRemove;
  }

  @Test
  public void testAddAndRemove() {
    for (int numRemovals = 0; numRemovals <= numElements; numRemovals++) {
      Example example = new Example();
      for (int j = 0; j < numElements; j++) {
        REVERSE_DEPS_UTIL.addReverseDeps(example, Collections.singleton(new SkyKey(NODE_TYPE, j)));
      }
      // Not a big test but at least check that it does not blow up.
      assertThat(REVERSE_DEPS_UTIL.toString(example)).isNotEmpty();
      assertThat(REVERSE_DEPS_UTIL.getReverseDeps(example)).hasSize(numElements);
      for (int i = 0; i < numRemovals; i++) {
        REVERSE_DEPS_UTIL.removeReverseDep(example, new SkyKey(NODE_TYPE, i));
      }
      assertThat(REVERSE_DEPS_UTIL.getReverseDeps(example)).hasSize(numElements - numRemovals);
      assertThat(example.reverseDepsToRemove).isNull();
    }
  }

  // Same as testAdditionAndRemoval but we add all the reverse deps in one call.
  @Test
  public void testAddAllAndRemove() {
    for (int numRemovals = 0; numRemovals <= numElements; numRemovals++) {
      Example example = new Example();
      List<SkyKey> toAdd = new ArrayList<>();
      for (int j = 0; j < numElements; j++) {
        toAdd.add(new SkyKey(NODE_TYPE, j));
      }
      REVERSE_DEPS_UTIL.addReverseDeps(example, toAdd);
      assertThat(REVERSE_DEPS_UTIL.getReverseDeps(example)).hasSize(numElements);
      for (int i = 0; i < numRemovals; i++) {
        REVERSE_DEPS_UTIL.removeReverseDep(example, new SkyKey(NODE_TYPE, i));
      }
      assertThat(REVERSE_DEPS_UTIL.getReverseDeps(example)).hasSize(numElements - numRemovals);
      assertThat(example.reverseDepsToRemove).isNull();
    }
  }

  @Test
  public void testDuplicateCheckOnGetReverseDeps() {
    Example example = new Example();
    for (int i = 0; i < numElements; i++) {
      REVERSE_DEPS_UTIL.addReverseDeps(example, Collections.singleton(new SkyKey(NODE_TYPE, i)));
    }
    // Should only fail when we call getReverseDeps().
    REVERSE_DEPS_UTIL.addReverseDeps(example, Collections.singleton(new SkyKey(NODE_TYPE, 0)));
    try {
      REVERSE_DEPS_UTIL.getReverseDeps(example);
      assertThat(numElements).isEqualTo(0);
    } catch (Exception expected) { }
  }

  @Test
  public void testMaybeCheck() {
    Example example = new Example();
    for (int i = 0; i < numElements; i++) {
      REVERSE_DEPS_UTIL.addReverseDeps(example, Collections.singleton(new SkyKey(NODE_TYPE, i)));
      // This should always succeed, since the next element is still not present.
      REVERSE_DEPS_UTIL.maybeCheckReverseDepNotPresent(example, new SkyKey(NODE_TYPE, i + 1));
    }
    try {
      REVERSE_DEPS_UTIL.maybeCheckReverseDepNotPresent(example, new SkyKey(NODE_TYPE, 0));
      // Should only fail if empty or above the checking threshold.
      assertThat(numElements == 0 || numElements >= ReverseDepsUtil.MAYBE_CHECK_THRESHOLD).isTrue();
    } catch (Exception expected) { }
  }
}
