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

package io.termd.core.telnet;

import io.termd.core.tty.ReadBuffer;
import io.termd.core.tty.TtyEvent;
import io.termd.core.tty.TtyEventDecoder;
import io.termd.core.tty.TtyOutputMode;
import io.termd.core.util.Vector;
import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.io.TelnetCharset;
import io.termd.core.tty.TtyConnection;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A telnet handler that implements {@link io.termd.core.tty.TtyConnection}.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public final class TelnetTtyConnection extends TelnetHandler implements TtyConnection {

  private Vector size;
  private String terminalType;
  private Consumer<Vector> sizeHandler;
  private Consumer<String> termHandler;
  private Consumer<Void> closeHandler;
  protected TelnetConnection conn;
  private final ReadBuffer readBuffer = new ReadBuffer(this::execute);
  private final TtyEventDecoder eventDecoder = new TtyEventDecoder(3, 26, 4).setReadHandler(readBuffer);
  private final BinaryDecoder decoder = new BinaryDecoder(512, TelnetCharset.INSTANCE, eventDecoder);
  private final BinaryEncoder encoder = new BinaryEncoder(512, StandardCharsets.US_ASCII, data -> conn.write(data));
  private final Consumer<int[]> stdout = new TtyOutputMode(encoder);
  private final Consumer<TtyConnection> handler;

  public TelnetTtyConnection(Consumer<TtyConnection> handler) {
    this.handler = handler;
  }

  @Override
  public String term() {
    return terminalType;
  }

  @Override
  public void execute(Runnable task) {
    conn.execute(task);
  }

  @Override
  public void schedule(Runnable task, long delay, TimeUnit unit) {
    conn.schedule(task, delay, unit);
  }

  @Override
  protected void onSendBinary(boolean binary) {
    if (binary) {
      encoder.setCharset(StandardCharsets.UTF_8);
    }
  }

  @Override
  protected void onReceiveBinary(boolean binary) {
    if (binary) {
      decoder.setCharset(StandardCharsets.UTF_8);
    }
  }

  @Override
  protected void onData(byte[] data) {
    decoder.write(data);
  }

  @Override
  protected void onOpen(TelnetConnection conn) {
    this.conn = conn;

    // Kludge mode
    conn.writeWillOption(Option.ECHO);
    conn.writeWillOption(Option.SGA);

    // Window size
    conn.writeDoOption(Option.NAWS);

    // Binary by all means
    conn.writeDoOption(Option.BINARY);
    conn.writeWillOption(Option.BINARY);

    // Get some info about user
    conn.writeDoOption(Option.TERMINAL_TYPE);

    //
    handler.accept(this);
  }

  @Override
  protected void onTerminalType(String terminalType) {
    this.terminalType = terminalType;
    if (termHandler != null) {
      termHandler.accept(terminalType);
    }
  }

  @Override
  public Vector size() {
    return size;
  }

  @Override
  protected void onSize(int width, int height) {
    this.size = new Vector(width, height);
    if (sizeHandler != null) {
      sizeHandler.accept(size);
    }
  }

  @Override
  public Consumer<Vector> getSizeHandler() {
    return sizeHandler;
  }

  @Override
  public void setSizeHandler(Consumer<Vector> handler) {
    this.sizeHandler = handler;
  }

  @Override
  public Consumer<String> getTermHandler() {
    return termHandler;
  }

  @Override
  public void setTermHandler(Consumer<String> handler) {
    termHandler = handler;
    if (handler != null && terminalType != null) {
      handler.accept(terminalType);
    }
  }

  @Override
  public BiConsumer<TtyEvent, Integer> getEventHandler() {
    return eventDecoder.getEventHandler();
  }

  @Override
  public void setEventHandler(BiConsumer<TtyEvent, Integer> handler) {
    eventDecoder.setEventHandler(handler);
  }

  @Override
  public Consumer<int[]> getStdinHandler() {
    return readBuffer.getReadHandler();
  }

  @Override
  public void setStdinHandler(Consumer<int[]> handler) {
    readBuffer.setReadHandler(handler);
  }

  @Override
  public Consumer<int[]> stdoutHandler() {
    return stdout;
  }

  @Override
  public void setCloseHandler(Consumer<Void> closeHandler) {
    this.closeHandler = closeHandler;
  }

  @Override
  public Consumer<Void> getCloseHandler() {
    return closeHandler;
  }

  @Override
  protected void onClose() {
    if (closeHandler != null) {
      closeHandler.accept(null);
    }
  }

  @Override
  public void close() {
    conn.close();
  }
}
