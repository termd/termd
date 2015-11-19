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

package io.termd.core.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.io.TelnetCharset;
import io.termd.core.tty.TtyEvent;
import io.termd.core.tty.TtyEventDecoder;
import io.termd.core.tty.TtyConnection;
import io.termd.core.tty.TtyOutputMode;
import io.termd.core.util.Vector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A connection to an http client, independant of the protocol, it could be straight Websockets or
 * SockJS, etc...
 *
 * The incoming protocol is based on json messages:
 *
 * {
 *   "action": "read",
 *   "data": "what the user typed"
 * }
 *
 * or
 *
 * {
 *   "action": "resize",
 *   "cols": 30,
 *   "rows: 50
 * }
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public abstract class HttpTtyConnection implements TtyConnection {

  private static final Vector DEFAULT_SIZE = new Vector(80, 24);

  private Vector size;
  private Consumer<Vector> sizeHandler;
  private final TtyEventDecoder eventDecoder;
  private final BinaryDecoder decoder;
  private final Consumer<int[]> stdout;
  private Consumer<Void> closeHandler;
  private Consumer<String> termHandler;

  public HttpTtyConnection() {
    this(DEFAULT_SIZE);
  }

  public HttpTtyConnection(Vector size) {
    this.size = size;
    this.eventDecoder = new TtyEventDecoder(3, 26, 4);
    this.decoder = new BinaryDecoder(512, TelnetCharset.INSTANCE, eventDecoder);
    this.stdout = new TtyOutputMode(new BinaryEncoder(StandardCharsets.US_ASCII, this::write));
  }

  @Override
  public String terminalType() {
    return "vt100";
  }

  protected abstract void write(byte[] buffer);

  public void writeToDecoder(String msg) {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> obj;
    String action;
    try {
      obj = mapper.readValue(msg, Map.class);
      action = (String) obj.get("action");
    } catch (IOException e) {
      // Log this
      return;
    }
    if (action != null) {
      switch (action) {
        case "read":
          String data = (String) obj.get("data");
          decoder.write(data.getBytes()); //write back echo
          break;
        case "resize":
          try {
            int cols = (int) obj.getOrDefault("cols", size.x());
            int rows = (int) obj.getOrDefault("rows", size.y());
            if (cols > 0 && rows > 0) {
              Vector newSize = new Vector(cols, rows);
              if (!newSize.equals(size())) {
                size = newSize;
                if (sizeHandler != null) {
                  sizeHandler.accept(size);
                }
              }
            }
          } catch (Exception e) {
            // Invalid size
            // Log this
          }
          break;
      }
    }
  }

  public Consumer<String> getTerminalTypeHandler() {
    return termHandler;
  }

  public void setTerminalTypeHandler(Consumer<String> handler) {
    termHandler = handler;
  }

  @Override
  public Vector size() {
    return size;
  }

  public Consumer<Vector> getSizeHandler() {
    return sizeHandler;
  }

  public void setSizeHandler(Consumer<Vector> handler) {
    this.sizeHandler = handler;
  }

  @Override
  public BiConsumer<TtyEvent, Integer> getEventHandler() {
    return eventDecoder.getEventHandler();
  }

  @Override
  public void setEventHandler(BiConsumer<TtyEvent, Integer> handler) {
    eventDecoder.setEventHandler(handler);
  }

  public Consumer<int[]> getStdinHandler() {
    return eventDecoder.getReadHandler();
  }

  public void setStdinHandler(Consumer<int[]> handler) {
    eventDecoder.setReadHandler(handler);
  }

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
}
