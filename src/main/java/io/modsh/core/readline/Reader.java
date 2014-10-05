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
package io.modsh.core.readline;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Reader {

  private final Action[] actions;
  private State state;

  public Reader() {
    this(Keys.values());
  }

  public Reader(InputStream inputrc) {
    ArrayList<Action> actions = new ArrayList<>();
    InputrcHandler handler = new InputrcHandler() {
      @Override
      public void bindFunction(int[] keySequence, String functionName) {
        actions.add(new Function() {
          @Override
          public String getName() {
            return functionName;
          }

          @Override
          public int getAt(int index) throws IndexOutOfBoundsException {
            if (index < 0 || index > keySequence.length) {
              throw new IndexOutOfBoundsException("Wrong index not in the range [0, " + keySequence.length + "[");
            }
            return keySequence[index];
          }

          @Override
          public int length() {
            return keySequence.length;
          }

          @Override
          public String toString() {
            return functionName;
          }
        });
      }
    };
    InputrcHandler.parse(inputrc, handler);
    this.actions = actions.toArray(new Action[actions.size()]);
    this.state = new State(new int[0], new Action[0]);
  }

  public Reader(Key[] keys) {
    this.actions = keys;
    this.state = new State(new int[0], new Action[0]);
  }

  public Reader append(int... chars) {
    for (int c : chars) {
      state = state.append(c);
    }
    return this;
  }

  public Reader reduceOnce() {
    state = state.reduceOnce();
    return this;
  }

  public Reader reduce() {
    while (true) {
      State next = state.reduceOnce();
      if (next == state) {
        break;
      } else {
        state = next;
      }
    }
    return this;
  }

  /**
   * Pop the next key, returns null if no key is present.
   *
   * @return the next key
   */
  public Action popKey() {
    if (state.queue.length > 0) {
      Action key = state.queue[0];
      state = state.next();
      return key;
    }
    return null;
  }

  public List<Action> getActions() {
    return Arrays.asList(state.queue);
  }

  public class State {

    private final int[] buffer;
    private final Action[] queue;

    private State(int[] buffer, Action[] queue) {
      this.buffer = buffer;
      this.queue = queue;
    }

    State next() {
      if (queue.length > 0) {
        return new State(buffer, Arrays.copyOfRange(queue, 1, queue.length));
      } else {
        return this;
      }
    }

    State append(int c) {
      int[] buffer2 = Arrays.copyOf(buffer, buffer.length + 1);
      buffer2[buffer.length] = c;
      return new State(buffer2, queue);
    }

    State reduceOnce() {
      if (buffer.length > 0) {
        Action candidate = null;
        int prefixes = 0;
        next:
        for (Action action : Reader.this.actions) {
          if (action.length() > 0) {
            if (action.length() <= buffer.length) {
              for (int i = 0;i < action.length();i++) {
                if (action.getAt(i) != buffer[i]) {
                  continue next;
                }
              }
              if (candidate != null && candidate.length() > action.length()) {
                continue next;
              }
              candidate = action;
            } else {
              for (int i = 0;i < buffer.length;i++) {
                if (action.getAt(i) != buffer[i]) {
                  continue next;
                }
              }
              prefixes++;
            }
          }
        }
        if (candidate == null) {
          if (prefixes == 0) {
            int c = buffer[0];
            Action[] a = Arrays.copyOf(queue, queue.length + 1);
            a[queue.length] = new Key() {
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
            };
            return new State(Arrays.copyOfRange(buffer, 1, buffer.length), a);
          }
        } else {
          Action[] a = Arrays.copyOf(queue, queue.length + 1);
          a[queue.length] = candidate;
          return new State(Arrays.copyOfRange(buffer, candidate.length(), buffer.length), a);
        }
      }
      return this;
    }
  }
}
