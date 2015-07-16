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

package io.termd.core.tty;

import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TtyEventDecoder implements Consumer<int[]> {

  private Consumer<int[]> readHandler;
  private Consumer<TtyEvent> eventHandler;
  private final int vintr;
  private final int veof;
  private final int vsusp;

  public TtyEventDecoder(int vintr, int vsusp, int veof) {
    this.vintr = vintr;
    this.vsusp = vsusp;
    this.veof = veof;
  }

  public Consumer<int[]> getReadHandler() {
    return readHandler;
  }

  public TtyEventDecoder setReadHandler(Consumer<int[]> readHandler) {
    this.readHandler = readHandler;
    return this;
  }

  public Consumer<TtyEvent> getEventHandler() {
    return eventHandler;
  }

  public TtyEventDecoder setEventHandler(Consumer<TtyEvent> eventHandler) {
    this.eventHandler = eventHandler;
    return this;
  }

  @Override
  public void accept(int[] data) {
    if (eventHandler != null) {
      int index = 0;
      while (index < data.length) {
        int val = data[index];
        TtyEvent event = null;
        if (val == vintr) {
          event = TtyEvent.INTR;
        } else if (val == vsusp) {
          event = TtyEvent.SUSP;
        } else if (val == veof) {
          event = TtyEvent.EOT;
        }
        if (event != null) {
          if (eventHandler != null) {
            if (readHandler != null) {
              int[] a = new int[index];
              if (index > 0) {
                System.arraycopy(data, 0, a, 0, index);
                readHandler.accept(a);
              }
            }
            eventHandler.accept(event);
            int[] a = new int[data.length - index - 1];
            System.arraycopy(data, index + 1, a, 0, a.length);
            data = a;
            index = 0;
            continue;
          }
        }
        index++;
      }
    }
    if (readHandler != null && data.length > 0) {
      readHandler.accept(data);
    }
  }
}
