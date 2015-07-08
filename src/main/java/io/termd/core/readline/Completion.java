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
 * An object for asynchronous completion.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Completion {

  /**
   * @return the prefix to complete
   */
  int[] prefix();

  /**
   * @return the current screen dimension at the moment the completion was initiated
   */
  Dimension size();

  /**
   * Complete this completion, this should be called once with the result, each result should be prefixed
   * by {@link #prefix()} otherwise this method will throw an {@link java.lang.IllegalArgumentException}.
   * This method will end the completion whatsoever and {@link #end()} should not be called.
   *
   * @param candidates the candidates for completion
   */
  default void complete(List<int[]> candidates) {
    int[] line;
    int[] text = prefix();
    if (candidates.size() == 0) {
      // Do nothing
      end();
      return;
    } else if (candidates.size() == 1) {
      line = Arrays.copyOf(candidates.get(0), candidates.get(0).length + 1);
      line[line.length - 1] = ' ';
    } else {
      // Find common prefix
      int[] prefix = Helper.findLongestCommonPrefix(candidates);
      if (prefix.length > text.length) {
        line = prefix;
      } else {
        // Todo : paginate vertically somehow
        int[] block = Helper.computeBlock(size(), candidates);
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

  /**
   * Insert an inline completion, the {@code text} argument will be inserted at the current position and
   * the new position will be changed, the underlying edition buffer is also modified.<p/>
   *
   * Once this method is called, only the same method and the {@link #end()} method can be called.
   *
   * @param text the text to insert inline
   * @return this completion object
   */
  Completion inline(int[] text);

  /**
   * Write a block of text below the current edition line, the first time this method is called, a {@code CRLF}
   * sequence is inserted before write the block of text.<p/>
   *
   * Once this method is called, only the same method and the {@link #end()} method can be called. When {@link #end()}
   * is called, the last edition line will be rewritten so the user can pursue the line edition.
   *
   * @param text the text to insert inline
   * @return this completion object
   */
  Completion write(int[] text);

  /**
   * End the completion, doing the necessary udpates if necessary. This method should be called once, and after
   * all updates are done, no further updates are possible after this call.
   */
  void end();

}
