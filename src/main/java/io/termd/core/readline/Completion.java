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

import io.termd.core.util.Dimension;
import io.termd.core.util.Helper;

import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Completion {

  /**
   * @return the text to complete
   */
  int[] text();

  /**
   * @return the current screen dimension at the moment the completion was initiated
   */
  Dimension size();

  /**
   * Complete this completion, this should be called once with the result, each result should be prefixed
   * by {@link #text()} otherwise this method will throw an {@link java.lang.IllegalArgumentException}.
   * This method will end the completion whatsoever and {@link #end()} should not be called.
   *
   * @param completions the resulting completions
   */
  default void complete(List<int[]> completions) {
    int[] line;
    int[] text = text();
    if (completions.size() == 0) {
      // Do nothing
      end();
      return;
    } else if (completions.size() == 1) {
      line = Arrays.copyOf(completions.get(0), completions.get(0).length + 1);
      line[line.length - 1] = ' ';
    } else {
      // Find common prefix
      int[] prefix = Helper.findLongestCommonPrefix(completions);
      if (prefix.length > text.length) {
        line = prefix;
      } else {
        // Todo : paginate vertically somehow
        int[] block = Helper.computeBlock(size(), completions);
        write(block);
        end();
        return;
      }
    }
    int delta = line.length - text.length;
    if (delta > 0  && Arrays.equals(text, Arrays.copyOf(line, text.length))) {
      int[] tmp = new int[delta];
      System.arraycopy(line, text.length, tmp, 0, tmp.length);
      inline(tmp);
      end();
    } else {
      end();
      throw new IllegalArgumentException("Determined completion " + Helper.fromCodePoints(line) +
          " must be prefixed by " + Helper.fromCodePoints(text));
    }
  }

  Completion inline(int[] line);

  Completion write(int[] data);

  void end();

}
