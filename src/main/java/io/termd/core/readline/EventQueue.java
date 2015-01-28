/*
 * Copyright 2014 Julien Viet
 *
 * Julien Viet licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
package io.termd.core.readline;

import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * The event queue is a state machine that consumes chars and produces events.
 *
 * todo : use a Trie for the mapping instead of using a lookup
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class EventQueue {

  private final EventMapping[] mappings;
  private State state;

  public EventQueue() {
    this(Keys.values());
  }

  public EventQueue(InputStream inputrc) {
    final ArrayList<EventMapping> actions = new ArrayList<>();
    InputrcHandler handler = new InputrcHandler() {
      @Override
      public void bindFunction(final int[] keySequence, final String functionName) {
        actions.add(new EventMapping(keySequence, new FunctionEvent() {
          @Override
          public String getName() {
            return functionName;
          }
          @Override
          public String toString() {
            return functionName;
          }
        }));
      }
    };
    InputrcHandler.parse(inputrc, handler);
    this.mappings = actions.toArray(new EventMapping[actions.size()]);
    this.state = new State(new int[0]);
  }

  public EventQueue(KeyEvent[] keys) {
    this.mappings = new EventMapping[keys.length];
    for (int i = 0;i < keys.length;i++) {
      mappings[i] = new EventMapping(keys[i]);
    }
    this.state = new State(new int[0]);
  }

  public EventQueue append(int... chars) {
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

      FooBar fooBar = reduce(buffer, mappings);

      //
      this.head = fooBar != null ? fooBar.event : null;
      this.length = fooBar != null ? fooBar.size : 0;
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

  private static class EventMapping {
    final int[] seq;
    final Event event;
    public EventMapping(KeyEvent event) {
      this.seq = new int[event.length()];
      for (int i = 0;i < seq.length;i++) {
        seq[i] = event.getAt(i);
      }
      this.event = event;
    }
    public EventMapping(int[] seq, Event event) {
      this.seq = seq;
      this.event = event;
    }
  }

  static class FooBar {
    final Event event;
    final int size;
    public FooBar(Event event, int size) {
      this.event = event;
      this.size = size;
    }
  }

  private static FooBar reduce(int[] buffer, EventMapping[] mappings) {
    if (buffer.length > 0) {
      EventMapping candidate = null;
      int prefixes = 0;
      next:
      for (EventMapping action : mappings) {
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
          return new FooBar(new KeyEvent() {
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
        return new FooBar(candidate.event, candidate.seq.length);
      }
    }
    return null;
  }
}
