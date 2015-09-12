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

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class CompletionImpl implements Completion {

  private final AtomicReference<CompletionStatus> status = new AtomicReference<>(CompletionStatus.PENDING);
  private final Readline.Interaction interaction;
  private final int[] line;
  private final int [] prefix;

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
  public void end() {
    while (true) {
      CompletionStatus current = status.get();
      if (current != CompletionStatus.COMPLETED) {
        if (status.compareAndSet(current, CompletionStatus.COMPLETED)) {
          switch (current) {
            case COMPLETING:
              interaction.redraw();
              break;
          }
          interaction.resume();
          break;
        }
        // Try again
      } else {
        throw new IllegalStateException();
      }
    }
  }

  @Override
  public Completion complete(int[] text, boolean terminal) {
    if (status.compareAndSet(CompletionStatus.PENDING, CompletionStatus.INLINING)) {
      if (text.length > 0 || terminal) {
        LineBuffer work = interaction.buffer().copy();
        ParsedBuffer toto = work.insertEscaped(text); // Todo improve that
        if (terminal) {
          switch (toto.quoting) {
            case WEAK:
              if (toto.escaping) {
                // Do nothing emit bell
              } else {
                work.insert('"', ' ');
                toto.accept('"');
                toto.accept(' ');
              }
              break;
            case STRONG:
              work.insert('\'', ' ');
              toto.accept('\'');
              toto.accept(' ');
              break;
            case NONE:
              if (toto.escaping) {
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
    } else {
      throw new IllegalStateException();
    }
    return this;
  }

  @Override
  public Completion suggest(int[] text) {
    while (true) {
      CompletionStatus current = status.get();
      if ((current == CompletionStatus.PENDING || current == CompletionStatus.COMPLETING)) {
        if (status.compareAndSet(current, CompletionStatus.COMPLETING)) {
          if (current == CompletionStatus.PENDING) {
            interaction.conn.write("\n");
          }
          interaction.conn.stdoutHandler().accept(text);
          return this;
        }
        // Try again
      } else {
        throw new IllegalStateException();
      }
    }
  }
}
