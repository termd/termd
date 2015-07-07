/*
 * Copyright 2015 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.termd.core.util;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class HelperTest {

  @Test
  public void testFindLongestCommonPrefix() {
    assertLongestCommonPrefix(new int[]{});
    assertLongestCommonPrefix(new int[]{0,1,2}, new int[]{0,1,2});
    assertLongestCommonPrefix(new int[]{0,1,2}, new int[]{0,1,2,4}, new int[]{0,1,2,3});
    assertLongestCommonPrefix(new int[]{0,1}, new int[]{0,1,2,4}, new int[]{0,1,2,3}, new int[]{0,1,3});
  }

  private void assertLongestCommonPrefix(int[] expected, int[]... tests) {
    assertTrue(Arrays.equals(expected, Helper.findLongestCommonPrefix(Arrays.asList(tests))));
  }
}
