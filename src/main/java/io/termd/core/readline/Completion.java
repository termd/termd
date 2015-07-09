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
import java.util.stream.Collectors;

/**
 * An object for asynchronous completion.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Completion {

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
  Dimension size();

  /**
   * Complete this completion, this should be called once with the result, each result being a possible suffix.
   * This method will end the completion whatsoever and {@link #end()} should not be called.
   *
   * @param candidates the candidates for completion
   */
  default void complete(List<int[]> candidates) {
    int[] line;
    int[] text = prefix();
    boolean terminate;
    if (candidates.size() == 0) {
      // Do nothing
      end();
      return;
    } else if (candidates.size() == 1) {
      line = Arrays.copyOf(candidates.get(0), candidates.get(0).length);
      terminate = true;
    } else {
      // Find common prefix
      int[] prefix = Helper.findLongestCommonPrefix(candidates);
      if (prefix.length > 0) {
        line = prefix;
        terminate = false;
      } else {
        // Todo : paginate vertically somehow
        int[] block = Helper.computeBlock(size(), candidates.stream().map(i -> {
          int[] concat = Arrays.copyOf(text, text.length + i.length);
          System.arraycopy(i, 0, concat, text.length, i.length);
          return concat;
        }).collect(Collectors.toList()));
        write(block);
        end();
        return;
      }
    }
    inline(line, terminate);
    end();
  }

  /**
   * Insert an inline completion with {@code terminate} arg set to false.
   *
   * @see #inline(int[], boolean)
   */
  default Completion inline(int[] text) {
    return inline(text, false);
  }

  /**
   * Insert an inline completion, the {@code text} argument will be inserted at the current position and
   * the new position will be changed, the underlying edition buffer is also modified.<p/>
   *
   * Once this method is called, only the same method and the {@link #end()} method can be called.
   *
   * @param text the text to insert inline
   * @param terminate true if an extra whitespace must be inserted after the text
   * @return this completion object
   */
  Completion inline(int[] text, boolean terminate);

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
