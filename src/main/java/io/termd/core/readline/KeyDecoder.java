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

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * The event queue is a state machine that consumes chars and produces events.
 *
 * todo : use a Trie for the mapping instead of using a lookup
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class KeyDecoder {

  private final Keymap keymap;
  private State state;

  public KeyDecoder(Keymap keymap) {
    this.keymap = keymap;
    this.state = new State(new int[0]);
  }

  public KeyDecoder append(int... chars) {
    for (int c : chars) {
      state = state.append(c);
    }
    return this;
  }

  public boolean hasNext() {
    return state.head != null;
  }

  public Event peek() {
    return state.head;
  }

  public Event next() {
    if (state.head != null) {
      Event next = state.head;
      state = state.next();
      return next;
    } else {
      throw new NoSuchElementException();
    }
  }

  public int[] clear() {
    int[] buffer = state.buffer;
    state = new State(new int[0]);
    return buffer;
  }

  /**
   * @return the buffer chars as a read-only int buffer
   */
  public IntBuffer getBuffer() {
    return IntBuffer.wrap(state.buffer).asReadOnlyBuffer();
  }

  private class State {

    private final Event head;
    private final int length;
    private final int[] buffer;

    private State(int[] buffer) {

      Match match = reduce(buffer);

      //
      this.head = match != null ? match.event : null;
      this.length = match != null ? match.size : 0;
      this.buffer = buffer;
    }

    State next() {
      if (head != null) {
        return new State(Arrays.copyOfRange(buffer, length, buffer.length));
      } else {
        return this;
      }
    }

    State append(int c) {
      int[] buffer2 = Arrays.copyOf(buffer, buffer.length + 1);
      buffer2[buffer.length] = c;
      return new State(buffer2);
    }
  }

  static class Match {
    final Event event;
    final int size;
    public Match(Event event, int size) {
      this.event = event;
      this.size = size;
    }
  }

  private Match reduce(int[] buffer) {
    if (buffer.length > 0) {
      Keymap.Binding candidate = null;
      int prefixes = 0;
      next:
      for (Keymap.Binding action : keymap.bindings) {
        if (action.seq.length > 0) {
          if (action.seq.length <= buffer.length) {
            for (int i = 0;i < action.seq.length;i++) {
              if (action.seq[i] != buffer[i]) {
                continue next;
              }
            }
            if (candidate != null && candidate.seq.length > action.seq.length) {
              continue next;
            }
            candidate = action;
          } else {
            for (int i = 0;i < buffer.length;i++) {
              if (action.seq[i] != buffer[i]) {
                continue next;
              }
            }
            prefixes++;
          }
        }
      }
      if (candidate == null) {
        if (prefixes == 0) {
          final int c = buffer[0];
          return new Match(new KeyEvent() {
            @Override
            public int getAt(int index) throws IndexOutOfBoundsException {
              if (index != 0) {
                throw new IndexOutOfBoundsException("Wrong index " + index);
              }
              return c;
            }
            @Override
            public int length() {
              return 1;
            }
            @Override
            public String toString() {
              return "key:" + c;
            }
          }, 1);
        }
      } else {
        return new Match(candidate.event, candidate.seq.length);
      }
    }
    return null;
  }
}
