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

import io.termd.core.util.Vector;
import io.termd.core.util.Helper;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A connection to a tty.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface TtyConnection {

  /**
   * @return the last time this connection received input
   */
  long lastAccessedTime();

  Vector size();

  /**
   * @return the current charset for sending data
   */
  Charset inputCharset();

  /**
   * @return the current charset for receiving data
   */
  Charset outputCharset();

  /**
   * @return the terminal type value, this can be null when the terminal has not declared its type yet. The {@link #setTerminalTypeHandler(Consumer)}
   *         can be used to aware when the terminal type is known.
   */
  String terminalType();

  /**
   * @return the terminal type handler
   */
  Consumer<String> getTerminalTypeHandler();

  /**
   *
   * @param handler set an handler called when the terminal type value is known
   */
  void setTerminalTypeHandler(Consumer<String> handler);

  Consumer<Vector> getSizeHandler();

  void setSizeHandler(Consumer<Vector> handler);

  BiConsumer<TtyEvent, Integer> getEventHandler();

  void setEventHandler(BiConsumer<TtyEvent, Integer> handler);

  Consumer<int[]> getStdinHandler();

  /**
   * Set the read handler on this connection.
   *
   * @param handler the event handler
   */
  void setStdinHandler(Consumer<int[]> handler);

  /**
   * @return the stdout handler of this connection
   */
  Consumer<int[]> stdoutHandler();

  void setCloseHandler(Consumer<Void> closeHandler);

  Consumer<Void> getCloseHandler();

  void close();

  default void close(int exit) {
    close();
  }

  /**
   * Write a string to the client.
   *
   * @param s the string to write
   */
  default TtyConnection write(String s) {
    int[] codePoints = Helper.toCodePoints(s);
    stdoutHandler().accept(codePoints);
    return this;
  }

  /**
   * Schedule a task for execution.
   *
   * @param task the task to schedule
   */
  void execute(Runnable task);

  /**
   * Schedule a task for execution.
   *
   * @param task the task to schedule
   * @param delay the delay
   * @param unit the time unit
   */
  void schedule(Runnable task, long delay, TimeUnit unit);

}
