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

import io.termd.core.util.Dimension;
import io.termd.core.util.Helper;

import java.util.function.Consumer;

/**
 * A connection to a tty.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface TtyConnection {

  Consumer<String> termHandler();

  void setTermHandler(Consumer<String> handler);

  Consumer<Dimension> sizeHandler();

  void setSizeHandler(Consumer<Dimension> handler);

  Consumer<TtyEvent> eventHandler();

  void setEventHandler(Consumer<TtyEvent> handler);

  Consumer<int[]> readHandler();

  /**
   * Set the read handler on this connection.
   *
   * @param handler the event handler
   */
  void setReadHandler(Consumer<int[]> handler);

  /**
   * @return the write handler of this connection
   */
  Consumer<int[]> writeHandler();

  void setCloseHandler(Consumer<Void> closeHandler);

  Consumer<Void> closeHandler();

  void close();

  /**
   * Write a string to the client.
   *
   * @param s the string to write
   */
  default void write(String s) {
    int[] codePoints = Helper.toCodePoints(s);
    writeHandler().accept(codePoints);
  }

  /**
   * Schedule a task for execution.
   *
   * @param task the task to schedule
   */
  void schedule(Runnable task);

  default String getInvokerContext() {
    return null;
  }
}
