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

package io.termd.core.readline;

import io.termd.core.util.Vector;
import io.termd.core.util.Helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An object for asynchronous completion.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Completion {

  static int[] findLongestCommonPrefix(List<int[]> entries) {
    return Helper.findLongestCommonPrefix(entries);
  }

  /**
   * @return the line being edited when completion started
   */
  int[] line();

  /**
   * @return the detected prefix to complete
   */
  int[] prefix();

  /**
   * @return the current screen dimension at the moment the completion was initiated
   */
  Vector size();

  /**
   * End this completion with no modifications.
   */
  default void end() {
    complete(new int[0], false);
  }

  /**
   * Insert an inline completion with {@code terminate} arg set to false.
   *
   * @see #complete(int[], boolean)
   */
  default void complete(int[] text) {
    complete(text, false);
  }

  /**
   * Perform completion based on a map of candidates, when the map:
   *
   * <ul>
   *   <li>is empty: end this completion</li>
   *   <li>has a single entry, perform a completion with the </li>
   * </ul>
   *
   * @param candidates
   */
  default void complete(Map<int[], Boolean> candidates) {
    switch (candidates.size()) {
      case 0:
        end();
        break;
      case 1:
        Map.Entry<int[], Boolean> match = candidates.entrySet().iterator().next();
        complete(match.getKey(), match.getValue());
        break;
      default:
        ArrayList<int[]> list = new ArrayList<>(candidates.keySet());
        int[] prefix = Helper.findLongestCommonPrefix(list);
        if (prefix.length > 0) {
          complete(prefix);
        } else {
          suggest(list);
        }
        break;
    }
  }

  /**
   * Insert an inline completion, the {@code text} argument will be inserted at the current position and
   * the new position will be changed, the underlying edition buffer is also modified.<p/>
   *
   * @param text the text to insert inline
   * @param terminal true if an extra whitespace must be inserted after the text
   */
  void complete(int[] text, boolean terminal);

  /**
   * Complete this completion, this should be called once with the result, each result being a possible suffix.
   *
   * @param candidates the candidates for completion
   */
  default void suggest(List<int[]> candidates) {
    suggest(Helper.computeBlock(size(), candidates));
  }

  /**
   * Write a block of text below the current edition line, a {@code CRLF} sequence is inserted before the
   * block of text.<p/>
   *
   * @param text the text to insert inline
   */
  void suggest(int[] text);

}
