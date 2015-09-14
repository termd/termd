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

import io.termd.core.util.Helper;
import io.termd.core.util.Vector;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An object for asynchronous completion.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Completion {

  public static int[] findLongestCommonPrefix(List<int[]> entries) {
    return Helper.findLongestCommonPrefix(entries);
  }

  private AtomicBoolean done = new AtomicBoolean();
  private final Readline.Interaction interaction;
  private final int[] line;
  private final int[] prefix;

  public Completion(Readline.Interaction interaction) {

    //
    int index = interaction.buffer().getCursor();
    while (index > 0 && interaction.buffer().getAt(index - 1) != ' ') {
      index--;
    }

    // Compute line : need to test full line :-)

    // Compute prefix
    LineBuffer prefix = interaction.line().copy();
    for (int i = index; i < interaction.buffer().getCursor();i++) {
      prefix.insert(interaction.buffer().getAt(i));
    }

    this.interaction = interaction;
    this.prefix = prefix.toArray();
    this.line = interaction.line().copy().insert(interaction.buffer().toArray()).toArray();
  }

  /**
   * @return the line being edited when completion started
   */
  public int[] line() {
    return line;
  }

  /**
   * @return the detected prefix to complete
   */
  public int[] prefix() {
    return prefix;
  }

  /**
   * @return the current screen dimension at the moment the completion was initiated
   */
  public Vector size() {
    return interaction.size();
  }

  /**
   * Insert an inline completion, the {@code text} argument will be inserted at the current position and
   * the new position will be changed, the underlying edition buffer is also modified.<p/>
   *
   * @param text the text to insert inline
   * @param terminal true if an extra whitespace must be inserted after the text
   */
  public void complete(int[] text, boolean terminal) {
    if (!done.compareAndSet(false, true)) {
      throw new IllegalStateException();
    }
    if (text.length > 0 || terminal) {
      LineBuffer work = interaction.buffer().copy();
      LineStatus.Ext toto = work.insertEscaped(text); // Todo improve that
      if (terminal) {
        switch (toto.getQuote()) {
          case '"':
            if (toto.isEscaping()) {
              // Do nothing emit bell
            } else {
              work.insert('"', ' ');
              toto.accept('"');
              toto.accept(' ');
            }
            break;
          case '\'':
            work.insert('\'', ' ');
            toto.accept('\'');
            toto.accept(' ');
            break;
          case 0:
            if (toto.isEscaping()) {
              // Do nothing emit bell
            } else {
              work.insert(' ');
              toto.accept(' ');
            }
            break;
        }
      }
      interaction.refresh(work);
    }
    interaction.resume();
  }

  /**
   * Write a block of text below the current edition line, a {@code CRLF} sequence is inserted before the
   * block of text.<p/>
   *
   * @param text the text to insert inline
   */
  public void suggest(int[] text) {
    if (!done.compareAndSet(false, true)) {
      throw new IllegalStateException();
    }
    interaction.conn.write("\n");
    interaction.conn.stdoutHandler().accept(text);
    interaction.redraw();
    interaction.resume();
  }

  /**
   * End this completion with no modifications.
   */
  public void end() {
    complete(new int[0], false);
  }

  /**
   * Insert an inline completion with {@code terminate} arg set to false.
   *
   * @see #complete(int[], boolean)
   */
  public void complete(int[] text) {
    complete(text, false);
  }

  /**
   * Complete this completion, this should be called once with the result, each result being a possible suffix.
   *
   * @param candidates the candidates for completion
   */
  public void suggest(List<int[]> candidates) {
    suggest(Helper.computeBlock(size(), candidates));
  }

}
