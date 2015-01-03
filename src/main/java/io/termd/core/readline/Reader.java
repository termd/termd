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

import io.termd.core.Handler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Reader {

  static class EventMapping {
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

  private final EventMapping[] mappings;
  private State state;
  private final Handler<Integer> appender = new Handler<Integer>() {
    @Override
    public void handle(Integer event) {
      append(event);
    }
  };
  private final Handler<int[]> appender2 = new Handler<int[]>() {
    @Override
    public void handle(int[] event) {
      append(event);
    }
  };

  public Reader() {
    this(Keys.values());
  }

  public Reader(InputStream inputrc) {
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
    this.state = new State(new int[0], new Event[0]);
  }

  public Reader(KeyEvent[] keys) {
    this.mappings = new EventMapping[keys.length];
    for (int i = 0;i < keys.length;i++) {
      mappings[i] = new EventMapping(keys[i]);
    }
    this.state = new State(new int[0], new Event[0]);
  }

  public Handler<Integer> appender() {
    return appender;
  }

  public Handler<int[]> appender2() {
    return appender2;
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
   * Pop the next event, returns null if no event is present.
   *
   * @return the next event
   */
  public Event popEvent() {
    if (state.queue.length > 0) {
      Event event = state.queue[0];
      state = state.next();
      return event;
    }
    return null;
  }

  /**
   * Returns the list of events in the queue.
   *
   * @return the event list
   */
  public List<Event> getEvents() {
    return Arrays.asList(state.queue);
  }

  public class State {

    private final int[] buffer;
    private final Event[] queue;

    private State(int[] buffer, Event[] queue) {
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
        EventMapping candidate = null;
        int prefixes = 0;
        next:
        for (EventMapping action : Reader.this.mappings) {
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
            Event[] a = Arrays.copyOf(queue, queue.length + 1);
            a[queue.length] = new KeyEvent() {
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
          Event[] a = Arrays.copyOf(queue, queue.length + 1);
          a[queue.length] = candidate.event;
          return new State(Arrays.copyOfRange(buffer, candidate.seq.length, buffer.length), a);
        }
      }
      return this;
    }
  }
}
