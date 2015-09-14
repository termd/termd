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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class CompletionImpl implements Completion {

  private AtomicBoolean done = new AtomicBoolean();
  private final Readline.Interaction interaction;
  private final int[] line;
  private final int[] prefix;

  public CompletionImpl(Readline.Interaction interaction) {

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

  @Override
  public int[] line() {
    return line;
  }

  @Override
  public int[] prefix() {
    return prefix;
  }

  @Override
  public Vector size() {
    return interaction.size();
  }

  @Override
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

  @Override
  public void suggest(int[] text) {
    if (!done.compareAndSet(false, true)) {
      throw new IllegalStateException();
    }
    interaction.conn.write("\n");
    interaction.conn.stdoutHandler().accept(text);
    interaction.redraw();
    interaction.resume();
  }
}
